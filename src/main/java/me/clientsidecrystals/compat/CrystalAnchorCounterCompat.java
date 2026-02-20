package me.clientsidecrystals.compat;

import java.lang.reflect.Method;
import java.util.UUID;

public final class CrystalAnchorCounterCompat {
    private static volatile boolean resolved;
    private static volatile Method recordCrystal;

    public static void recordCrystalBreak(int entityId, UUID uuid) {
        if (!ensure()) return;
        try {
            recordCrystal.invoke(null, entityId, uuid);
        } catch (Throwable ignored) {
        }
    }

    private static boolean ensure() {
        if (!resolved) {
            synchronized (CrystalAnchorCounterCompat.class) {
                if (!resolved) {
                    try {
                        Class<?> cls = Class.forName("me.cutebow.crystalanchorcounter.client.CrystalAnchorCounterClient");
                        recordCrystal = cls.getMethod("externalRecordCrystalBreak", int.class, UUID.class);
                    } catch (Throwable t) {
                        recordCrystal = null;
                    }
                    resolved = true;
                }
            }
        }
        return recordCrystal != null;
    }

    private CrystalAnchorCounterCompat() {
    }
}
