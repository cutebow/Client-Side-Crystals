package me.clientsidecrystals.core;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

public final class SeamlessCrystalBridge {
    private static final Int2IntOpenHashMap AGE_DELTA = new Int2IntOpenHashMap();
    private static final Int2LongOpenHashMap HIDE_UNTIL_TICK = new Int2LongOpenHashMap();
    private static final ThreadLocal<Integer> TL_ENTITY_ID = ThreadLocal.withInitial(() -> -1);
    private static final ThreadLocal<Integer> TL_AGE_DELTA = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Boolean> TL_ACTIVE = ThreadLocal.withInitial(() -> false);

    private SeamlessCrystalBridge() {}

    public static void link(int realEntityId, int ageDelta, int currentTick, int hideTicks) {
        AGE_DELTA.put(realEntityId, ageDelta);
        long until = (long) currentTick + (long) hideTicks;
        HIDE_UNTIL_TICK.put(realEntityId, until);
    }

    public static boolean shouldHide(int realEntityId, int currentTick) {
        long until = HIDE_UNTIL_TICK.getOrDefault(realEntityId, Long.MIN_VALUE);
        return (long) currentTick <= until;
    }

    public static int ageDeltaFor(int realEntityId) {
        return AGE_DELTA.getOrDefault(realEntityId, 0);
    }

    public static void clear(int realEntityId) {
        AGE_DELTA.remove(realEntityId);
        HIDE_UNTIL_TICK.remove(realEntityId);
    }

    public static void begin(int realEntityId) {
        TL_ENTITY_ID.set(realEntityId);
        TL_AGE_DELTA.set(ageDeltaFor(realEntityId));
        TL_ACTIVE.set(true);
    }

    public static void end() {
        TL_ENTITY_ID.set(-1);
        TL_AGE_DELTA.set(0);
        TL_ACTIVE.set(false);
    }

    public static boolean active() {
        return TL_ACTIVE.get();
    }

    public static int currentAgeDelta() {
        return TL_AGE_DELTA.get();
    }

    public static int currentEntityId() {
        return TL_ENTITY_ID.get();
    }

    public static void clearAll() {
        AGE_DELTA.clear();
        HIDE_UNTIL_TICK.clear();
        end();
    }
}
