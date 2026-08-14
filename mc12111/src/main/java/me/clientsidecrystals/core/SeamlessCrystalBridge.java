package me.clientsidecrystals.core;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

public final class SeamlessCrystalBridge {
    private static final Int2FloatOpenHashMap AGE_DELTA = new Int2FloatOpenHashMap();
    private static final Int2FloatOpenHashMap PENDING_TARGET_AGE = new Int2FloatOpenHashMap();
    private static final Int2FloatOpenHashMap LAST_RENDERED_AGE = new Int2FloatOpenHashMap();

    private SeamlessCrystalBridge() {
    }

    public static void recordRenderedAge(int entityId, float age) {
        LAST_RENDERED_AGE.put(entityId, age);
    }

    public static float takeRenderedAge(int entityId, float fallback) {
        if (!LAST_RENDERED_AGE.containsKey(entityId)) {
            return fallback;
        }
        return LAST_RENDERED_AGE.remove(entityId);
    }

    public static void link(int realEntityId, float targetAge) {
        PENDING_TARGET_AGE.put(realEntityId, targetAge);
    }

    public static float apply(int realEntityId, float currentAge) {
        if (PENDING_TARGET_AGE.containsKey(realEntityId)) {
            float targetAge = PENDING_TARGET_AGE.remove(realEntityId);
            float delta = targetAge - currentAge;
            AGE_DELTA.put(realEntityId, delta);
            return targetAge;
        }

        return currentAge + AGE_DELTA.getOrDefault(realEntityId, 0.0F);
    }

    public static void clear(int entityId) {
        // entity ids get poofed after unload
        AGE_DELTA.remove(entityId);
        PENDING_TARGET_AGE.remove(entityId);
        LAST_RENDERED_AGE.remove(entityId);
    }

    public static void clearAll() {
        AGE_DELTA.clear();
        PENDING_TARGET_AGE.clear();
        LAST_RENDERED_AGE.clear();
    }
}
