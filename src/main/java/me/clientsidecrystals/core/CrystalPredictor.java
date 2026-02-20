package me.clientsidecrystals.core;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.clientsidecrystals.compat.CrystalAnchorCounterCompat;
import me.clientsidecrystals.config.ConfigManager;
import me.clientsidecrystals.mixin.EntityAgeAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CrystalPredictor {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final Long2ObjectOpenHashMap<Local> LOCAL = new Long2ObjectOpenHashMap();
    private static volatile boolean enabled = true;
    private static int tick;

    public static int debugTick() {
        return tick;
    }

    public static void setEnabled(boolean on) {
        enabled = on;
        if (!on) clearAll();
    }

    public static boolean isEnabled() {
        return enabled && ConfigManager.config.instantEnabled;
    }

    public static void clientTick() {
        if (!isEnabled()) {
            if (!LOCAL.isEmpty()) clearAll();
            return;
        }
        if (MC.world == null) return;

        tick++;

        long[] keys = LOCAL.keySet().toLongArray();
        for (long k : keys) {
            Local l = LOCAL.get(k);
            if (l == null || l.entity == null || l.entity.isRemoved() || !l.entity.isAlive()) {
                LOCAL.remove(k);
                continue;
            }

            BlockPos p = BlockPos.fromLong(k);
            double x = p.getX() + 0.5;
            double y = p.getY();
            double z = p.getZ() + 0.5;

            l.entity.refreshPositionAndAngles(x, y, z, 0.0f, 0.0f);
            l.entity.setShowBottom(false);
            l.entity.noClip = true;

            if (tick >= l.expiresTick) {
                if (l.pairedRealId != -1) SeamlessCrystalBridge.clear(l.pairedRealId);
                l.entity.discard();
                LOCAL.remove(k);
            }
        }

        if (MC.options.attackKey.isPressed()) {
            removeLocalIfCrosshairHitsIt();
        }
    }

    public static void onUseBlock(BlockHitResult hit) {
        if (!isEnabled()) return;
        if (MC.player == null || MC.world == null) return;
        if (!holdingCrystal()) return;

        BlockPos base = resolveBaseFromHit(hit);
        if (base == null) return;

        BlockPos crystalPos = base.up();
        if (hasAnyRealCrystal(crystalPos)) return;
        if (LOCAL.containsKey(crystalPos.asLong())) return;

        spawnLocal(crystalPos);
    }

    public static void onEntityLoaded(Entity e) {
        if (!isEnabled()) return;
        if (!(e instanceof EndCrystalEntity realCrystal)) return;

        boolean attacking = MC.options.attackKey.isPressed();

        BlockPos real = realCrystal.getBlockPos();
        long realKey = real.asLong();

        Local exact = LOCAL.get(realKey);
        if (exact != null && exact.entity != null && exact.entity.isAlive()) {
            if (ConfigManager.config.seamlessEnabled && !attacking) {
                int fakeAge = ((EntityAgeAccessor) exact.entity).clientsidecrystals$getAge();
                int realAge = ((EntityAgeAccessor) realCrystal).clientsidecrystals$getAge();
                int delta = fakeAge - realAge;

                exact.pairedRealId = realCrystal.getId();
                SeamlessCrystalBridge.link(exact.pairedRealId, delta, tick, 1);
                exact.expiresTick = Math.min(exact.expiresTick, tick + 1);
            } else {
                LOCAL.remove(realKey);
                exact.entity.discard();
            }
            return;
        }

        long[] keys = LOCAL.keySet().toLongArray();
        for (long k : keys) {
            BlockPos p = BlockPos.fromLong(k);
            if (Math.abs(p.getX() - real.getX()) <= 1 &&
                    Math.abs(p.getY() - real.getY()) <= 1 &&
                    Math.abs(p.getZ() - real.getZ()) <= 1) {
                Local near = LOCAL.get(k);
                if (near == null || near.entity == null || !near.entity.isAlive()) {
                    LOCAL.remove(k);
                    continue;
                }

                if (ConfigManager.config.seamlessEnabled && !attacking) {
                    int fakeAge = ((EntityAgeAccessor) near.entity).clientsidecrystals$getAge();
                    int realAge = ((EntityAgeAccessor) realCrystal).clientsidecrystals$getAge();
                    int delta = fakeAge - realAge;

                    near.pairedRealId = realCrystal.getId();
                    SeamlessCrystalBridge.link(near.pairedRealId, delta, tick, 1);
                    near.expiresTick = Math.min(near.expiresTick, tick + 1);
                } else {
                    LOCAL.remove(k);
                    near.entity.discard();
                }
            }
        }
    }

    public static void onEntityUnloaded(Entity e) {
        if (!isEnabled()) return;
        if (!(e instanceof EndCrystalEntity)) return;

        SeamlessCrystalBridge.clear(e.getId());

        BlockPos real = e.getBlockPos();
        long[] keys = LOCAL.keySet().toLongArray();
        for (long k : keys) {
            BlockPos p = BlockPos.fromLong(k);
            if (p.getX() == real.getX() && p.getY() == real.getY() && p.getZ() == real.getZ()) {
                Local l = LOCAL.remove(k);
                if (l != null && l.entity != null && l.entity.isAlive()) l.entity.discard();
                return;
            }
        }
    }

    private static void removeLocalIfCrosshairHitsIt() {
        HitResult hr = MC.crosshairTarget;
        if (hr == null || hr.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) hr).getEntity();
        if (!(target instanceof EndCrystalEntity)) return;

        BlockPos p = target.getBlockPos();
        Local l = LOCAL.get(p.asLong());
        if (l != null && l.entity == target) {
            LOCAL.remove(p.asLong());
            if (l.pairedRealId == -1) {
                CrystalAnchorCounterCompat.recordCrystalBreak(target.getId(), target.getUuid());
            }
            if (l.pairedRealId != -1) SeamlessCrystalBridge.clear(l.pairedRealId);
            if (l.entity.isAlive()) l.entity.discard();
        }
    }

    private static BlockPos resolveBaseFromHit(BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getSide();

        List<BlockPos> candidates = new ArrayList(4);
        if (side != Direction.DOWN) candidates.add(pos);
        candidates.add(pos.offset(side.getOpposite()));
        if (side == Direction.UP) candidates.add(pos.down());
        candidates.add(pos.down().offset(side.getOpposite()));

        for (BlockPos b : candidates) {
            if (isValidBase(b)) return b;
        }
        return null;
    }

    private static boolean holdingCrystal() {
        return MC.player.getMainHandStack().isOf(Items.END_CRYSTAL) || MC.player.getOffHandStack().isOf(Items.END_CRYSTAL);
    }

    private static void spawnLocal(BlockPos pos) {
        ClientWorld w = MC.world;
        if (w == null) return;

        Local old = LOCAL.remove(pos.asLong());
        if (old != null && old.entity != null && old.entity.isAlive()) {
            if (old.pairedRealId != -1) SeamlessCrystalBridge.clear(old.pairedRealId);
            old.entity.discard();
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        EndCrystalEntity e = new EndCrystalEntity(w, x, y, z);
        e.setUuid(UUID.randomUUID());
        e.setShowBottom(false);
        e.noClip = true;
        e.refreshPositionAndAngles(x, y, z, 0.0f, 0.0f);

        w.addEntity(e);

        int ttl = Math.max(2, ConfigManager.config.predictionTimeoutTicks);
        LOCAL.put(pos.asLong(), new Local(e, tick + ttl));
    }

    private static boolean hasAnyRealCrystal(BlockPos pos) {
        return findRealNear(pos) != null;
    }

    private static EndCrystalEntity findRealNear(BlockPos pos) {
        ClientWorld w = MC.world;
        if (w == null) return null;
        Box box = new Box(
                Vec3d.ofCenter(pos).add(-0.5, -0.5, -0.5),
                Vec3d.ofCenter(pos).add(0.5, 1.5, 0.5)
        );
        for (EndCrystalEntity e : w.getEntitiesByType(EntityType.END_CRYSTAL, box, x -> !isLocalCrystal(x))) return e;
        return null;
    }

    private static boolean isValidBase(BlockPos base) {
        ClientWorld w = MC.world;
        if (w == null) return false;

        BlockState below = w.getBlockState(base);
        if (!(below.isOf(Blocks.OBSIDIAN) || below.isOf(Blocks.BEDROCK))) return false;

        BlockPos above = base.up();
        if (!w.getBlockState(above).isAir()) return false;

        Box aabb = new Box(
                Vec3d.ofCenter(above).add(-0.5, -0.5, -0.5),
                Vec3d.ofCenter(above).add(0.5, 1.5, 0.5)
        );
        if (!w.getOtherEntities(null, aabb, e -> e.isAlive() && !(e instanceof EndCrystalEntity && isLocalCrystal(e))).isEmpty()) return false;

        return true;
    }

    private static boolean isLocalCrystal(Entity e) {
        Local l = LOCAL.get(e.getBlockPos().asLong());
        return l != null && l.entity == e;
    }

    public static void clearAll() {
        long[] keys = LOCAL.keySet().toLongArray();
        for (long k : keys) {
            Local l = LOCAL.remove(k);
            if (l != null && l.entity != null && l.entity.isAlive()) l.entity.discard();
        }
        SeamlessCrystalBridge.clearAll();
    }

    private static final class Local {
        private final EndCrystalEntity entity;
        private int expiresTick;
        private int pairedRealId;

        private Local(EndCrystalEntity entity, int expiresTick) {
            this.entity = entity;
            this.expiresTick = expiresTick;
            this.pairedRealId = -1;
        }
    }

    private CrystalPredictor() {}
}
