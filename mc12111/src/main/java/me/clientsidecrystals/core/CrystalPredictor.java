package me.clientsidecrystals.core;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.clientsidecrystals.compat.CrystalAnchorCounterCompat;
import me.clientsidecrystals.compat.FlashbackCompat;
import me.clientsidecrystals.compat.ReplayCrystalPayload;
import me.clientsidecrystals.config.ConfigManager;
import me.clientsidecrystals.mixin.EntityAgeAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

public final class CrystalPredictor {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Long2ObjectOpenHashMap<LocalCrystal> crystals = new Long2ObjectOpenHashMap<>();
    private static final Set<EndCrystalEntity> localEntities = Collections.newSetFromMap(new IdentityHashMap<>());

    private static int tick;
    private static int mainSwings;
    private static int offSwings;
    private static int mainSwingExpiry;
    private static int offSwingExpiry;

    private CrystalPredictor() {
    }

    public static void setEnabled(boolean enabled) {
        ConfigManager.config.instantEnabled = enabled;
        if (!enabled) {
            clearAll();
        }
    }

    public static boolean isEnabled() {
        return ConfigManager.config.instantEnabled;
    }

    public static void handleReplayCrystalPayload(ReplayCrystalPayload payload) {
        if (payload.action() == ReplayCrystalPayload.Action.SPAWN) {
            spawnLocal(payload.pos(), Math.max(1, payload.ttlTicks()), false, true);
        } else {
            removeLocal(payload.pos().asLong(), false);
        }
    }

    public static void clientTick() {
        if (!isEnabled() || spectatorBlocked()) {
            if (!crystals.isEmpty()) {
                clearAll();
            }
            return;
        }

        if (client.world == null) {
            return;
        }

        tick++;
        expireSwings();

        long[] keys = crystals.keySet().toLongArray();
        for (long key : keys) {
            LocalCrystal local = crystals.get(key);
            EndCrystalEntity crystal = local.entity();

            // flashback uses its own replay tick here
            int clock = local.replayClock() ? FlashbackCompat.replayTick() : tick;
            if (crystal.isRemoved() || !crystal.isAlive() || clock >= 0 && clock >= local.expiresTick()) {
                removeLocal(key, false);
                continue;
            }

            BlockPos pos = BlockPos.fromLong(key);
            crystal.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            crystal.setShowBottom(false);
            crystal.noClip = true;
        }

        if (!FlashbackCompat.isReplayActive() && client.options.attackKey.isPressed()) {
            removeCrosshairCrystal();
        }
    }

    public static void onUseBlock(Hand hand, BlockHitResult hit) {
        if (!isEnabled() || FlashbackCompat.isReplayActive() || spectatorBlocked() || client.world == null) {
            return;
        }

        if (hit.getType() != HitResult.Type.BLOCK || hit.getSide() == Direction.DOWN) {
            return;
        }

        BlockPos base = hit.getBlockPos();
        if (!canPlaceOn(base)) {
            return;
        }

        BlockPos crystalPos = base.up();
        if (crystals.containsKey(crystalPos.asLong()) || hasRealCrystal(crystalPos)) {
            return;
        }

        spawnLocal(crystalPos);
        swing(hand);
    }

    public static void onEntityLoaded(Entity entity) {
        if (!isEnabled() || spectatorBlocked() || !(entity instanceof EndCrystalEntity realCrystal)) {
            return;
        }

        // keep fake crystal until real spawns in
        LocalCrystal local = detach(realCrystal.getBlockPos().asLong());
        if (local == null) {
            return;
        }

        EndCrystalEntity fakeCrystal = local.entity();
        if (fakeCrystal.isRemoved() || !fakeCrystal.isAlive()) {
            SeamlessCrystalBridge.clear(fakeCrystal.getId());
            return;
        }

        if (ConfigManager.config.seamlessEnabled && !client.options.attackKey.isPressed()) {
            float entityAge = ((EntityAgeAccessor) (Object) fakeCrystal).csc$getAge();
            float renderedAge = SeamlessCrystalBridge.takeRenderedAge(fakeCrystal.getId(), entityAge);
            SeamlessCrystalBridge.link(realCrystal.getId(), renderedAge);
        } else {
            SeamlessCrystalBridge.clear(fakeCrystal.getId());
        }

        fakeCrystal.discard();
    }

    public static void onEntityUnloaded(Entity entity) {
        if (!(entity instanceof EndCrystalEntity crystal)) {
            return;
        }

        SeamlessCrystalBridge.clear(crystal.getId());
        removeLocal(crystal.getBlockPos().asLong(), !FlashbackCompat.isReplayActive());
    }

    public static boolean isLocalCrystal(Entity entity) {
        return entity instanceof EndCrystalEntity crystal && localEntities.contains(crystal);
    }

    public static boolean consumeServerSwingSuppression(Hand hand) {
        if (!ConfigManager.config.instantArmSwing) {
            return false;
        }

        if (hand == Hand.MAIN_HAND) {
            if (mainSwings == 0 || tick > mainSwingExpiry) {
                mainSwings = 0;
                return false;
            }
            mainSwings--;
            return true;
        }

        if (offSwings == 0 || tick > offSwingExpiry) {
            offSwings = 0;
            return false;
        }
        offSwings--;
        return true;
    }

    public static void clearAll() {
        for (long key : crystals.keySet().toLongArray()) {
            removeLocal(key, false);
        }
        localEntities.clear();
        mainSwings = 0;
        offSwings = 0;
        mainSwingExpiry = 0;
        offSwingExpiry = 0;
        SeamlessCrystalBridge.clearAll();
    }

    private static void removeCrosshairCrystal() {
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            return;
        }
        if (!(hit.getEntity() instanceof EndCrystalEntity crystal) || !localEntities.contains(crystal)) {
            return;
        }

        removeLocal(crystal.getBlockPos().asLong(), true);
        CrystalAnchorCounterCompat.recordCrystalBreak(crystal.getId(), crystal.getUuid());
    }

    private static void spawnLocal(BlockPos pos) {
        spawnLocal(pos, Math.max(2, ConfigManager.config.predictionTimeoutTicks), true, false);
    }

    private static void spawnLocal(BlockPos pos, int ttl, boolean record, boolean replayClock) {
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }

        long key = pos.asLong();
        removeLocal(key, false);

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        EndCrystalEntity crystal = new EndCrystalEntity(world, x, y, z);
        crystal.setUuid(UUID.randomUUID());
        crystal.setShowBottom(false);
        crystal.noClip = true;
        crystal.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
        world.addEntity(crystal);

        int lifetime = Math.max(1, ttl);
        int clock = tick;
        boolean useReplayClock = false;
        if (replayClock) {
            int replayTick = FlashbackCompat.replayTick();
            if (replayTick >= 0) {
                clock = replayTick;
                useReplayClock = true;
            }
        }
        crystals.put(key, new LocalCrystal(crystal, clock + lifetime, useReplayClock));
        localEntities.add(crystal);

        if (record) {
            FlashbackCompat.recordCrystalSpawn(pos, lifetime);
        }
    }

    private static void swing(Hand hand) {
        if (!ConfigManager.config.instantArmSwing || client.player == null) {
            return;
        }

        client.player.swingHand(hand, false);
        int expiry = tick + Math.max(4, ConfigManager.config.predictionTimeoutTicks + 2);

        if (hand == Hand.MAIN_HAND) {
            mainSwings++;
            mainSwingExpiry = Math.max(mainSwingExpiry, expiry);
        } else {
            offSwings++;
            offSwingExpiry = Math.max(offSwingExpiry, expiry);
        }
    }

    private static void expireSwings() {
        if (mainSwings > 0 && tick > mainSwingExpiry) {
            mainSwings = 0;
        }
        if (offSwings > 0 && tick > offSwingExpiry) {
            offSwings = 0;
        }
    }

    private static boolean spectatorBlocked() {
        return client.player != null && client.player.isSpectator() && !FlashbackCompat.isReplayActive();
    }

    private static boolean hasRealCrystal(BlockPos pos) {
        ClientWorld world = client.world;
        if (world == null) {
            return false;
        }

        return !world.getEntitiesByType(
                EntityType.END_CRYSTAL,
                crystalBox(pos),
                crystal -> !localEntities.contains(crystal)
        ).isEmpty();
    }

    private static boolean canPlaceOn(BlockPos base) {
        ClientWorld world = client.world;
        if (world == null) {
            return false;
        }

        BlockState state = world.getBlockState(base);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) {
            return false;
        }

        BlockPos crystalPos = base.up();
        if (!world.getBlockState(crystalPos).isAir()) {
            return false;
        }

        return world.getOtherEntities(
                null,
                crystalBox(crystalPos),
                entity -> entity.isAlive() && !isLocalCrystal(entity)
        ).isEmpty();
    }

    private static Box crystalBox(BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        return new Box(center.add(-0.5, -0.5, -0.5), center.add(0.5, 1.5, 0.5));
    }

    private static LocalCrystal detach(long key) {
        LocalCrystal local = crystals.remove(key);
        if (local != null) {
            localEntities.remove(local.entity());
        }
        return local;
    }

    private static void removeLocal(long key, boolean record) {
        LocalCrystal local = detach(key);
        if (local == null) {
            return;
        }

        if (record) {
            FlashbackCompat.recordCrystalRemove(BlockPos.fromLong(key));
        }

        EndCrystalEntity crystal = local.entity();
        SeamlessCrystalBridge.clear(crystal.getId());
        if (crystal.isAlive()) {
            crystal.discard();
        }
    }

    private record LocalCrystal(EndCrystalEntity entity, int expiresTick, boolean replayClock) {
    }
}
