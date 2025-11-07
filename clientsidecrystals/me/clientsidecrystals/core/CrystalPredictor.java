package me.clientsidecrystals.core;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.clientsidecrystals.config.ConfigManager;
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

import java.util.UUID;

public final class CrystalPredictor {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final Long2ObjectOpenHashMap<Local> LOCAL = new Long2ObjectOpenHashMap<>();
    private static volatile boolean enabled = true;
    private static int tick;
    private static boolean attackPrev;

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
            attackPrev = false;
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
            if (tick >= l.expiresTick) {
                l.entity.discard();
                LOCAL.remove(k);
            }
        }

        boolean pressed = MC.options.attackKey.isPressed();
        if (pressed && !attackPrev) handleAttackCrosshair();
        attackPrev = pressed;
    }

    public static void onUseBlock(BlockHitResult hit) {
        if (!isEnabled()) return;
        if (MC.player == null || MC.world == null) return;
        if (!MC.player.getMainHandStack().isOf(Items.END_CRYSTAL) && !MC.player.getOffHandStack().isOf(Items.END_CRYSTAL)) return;
        if (hit.getSide() != Direction.UP) return;

        BlockPos base = hit.getBlockPos();
        if (!isValidBase(base)) return;

        BlockPos pos = base.up();
        if (hasAnyRealCrystal(pos)) return;

        spawnLocal(pos);
    }

    public static void onEntityLoaded(Entity e) {
        if (!isEnabled()) return;
        if (!(e instanceof EndCrystalEntity)) return;
        BlockPos pos = e.getBlockPos();
        long key = pos.asLong();
        Local l = LOCAL.remove(key);
        if (l != null && l.entity != null && l.entity.isAlive()) l.entity.discard();
    }

    public static void onEntityUnloaded(Entity e) {
        if (!isEnabled()) return;
        if (!(e instanceof EndCrystalEntity)) return;
        BlockPos pos = e.getBlockPos();
        long key = pos.asLong();
        Local l = LOCAL.remove(key);
        if (l != null && l.entity != null && l.entity.isAlive()) l.entity.discard();
    }

    private static void handleAttackCrosshair() {
        if (MC.crosshairTarget == null) return;
        if (MC.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        Entity target = ((EntityHitResult) MC.crosshairTarget).getEntity();
        if (!(target instanceof EndCrystalEntity)) return;

        target.setInvisible(true);

        BlockPos p = target.getBlockPos();
        Local l = LOCAL.remove(p.asLong());
        if (l != null && l.entity != null && l.entity.isAlive()) l.entity.discard();
    }

    private static void spawnLocal(BlockPos pos) {
        ClientWorld w = MC.world;
        if (w == null) return;
        Local old = LOCAL.remove(pos.asLong());
        if (old != null && old.entity != null && old.entity.isAlive()) old.entity.discard();

        EndCrystalEntity fake = new EndCrystalEntity(w, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        fake.setUuid(UUID.randomUUID());
        fake.setShowBottom(false);
        fake.noClip = true;

        w.addEntity(fake);
        int ttl = Math.max(4, ConfigManager.config.predictionTimeoutTicks);
        LOCAL.put(pos.asLong(), new Local(fake, tick + ttl));
    }

    private static boolean hasAnyRealCrystal(BlockPos pos) {
        return findRealNear(pos) != null;
    }

    private static EndCrystalEntity findRealNear(BlockPos pos) {
        ClientWorld w = MC.world;
        if (w == null) return null;
        Box box = new Box(Vec3d.ofCenter(pos).add(-0.5, -0.5, -0.5), Vec3d.ofCenter(pos).add(0.5, 1.5, 0.5));
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

        Box aabb = new Box(Vec3d.ofCenter(above).add(-0.5, -0.5, -0.5), Vec3d.ofCenter(above).add(0.5, 1.5, 0.5));
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
    }

    private record Local(EndCrystalEntity entity, int expiresTick) {}
    private CrystalPredictor() {}
}
