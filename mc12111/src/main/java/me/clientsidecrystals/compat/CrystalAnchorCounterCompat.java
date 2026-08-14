package me.clientsidecrystals.compat;

import java.lang.reflect.Method;
import java.util.UUID;

public final class CrystalAnchorCounterCompat {
    private static final Method RECORD_BREAK = findRecordBreak();

    private CrystalAnchorCounterCompat() {
    }

    public static void recordCrystalBreak(int entityId, UUID uuid) {
        if (RECORD_BREAK == null) {
            return;
        }

        try {
            RECORD_BREAK.invoke(null, entityId, uuid);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static Method findRecordBreak() {
        try {
            return Class.forName("me.cutebow.crystalanchorcounter.client.CrystalAnchorCounterClient")
                    .getMethod("externalRecordCrystalBreak", int.class, UUID.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
