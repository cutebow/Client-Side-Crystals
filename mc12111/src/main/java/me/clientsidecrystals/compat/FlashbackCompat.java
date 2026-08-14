package me.clientsidecrystals.compat;

import me.clientsidecrystals.core.CrystalPredictor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class FlashbackCompat {
    private static Field recorderField;
    private static Method replayCheck;
    private static Method replayServerGetter;
    private static Method replayTickGetter;
    private static Method recorderReady;
    private static Method writePacket;
    private static Object playPhase;
    // optional dep base mod still works without flashback, not sure if it still works this was kinda a beta,  im not getting a ping spoofer to test but ik the tint doesnt show which I don't plan on fixing cause its too much work geeg
    private static final boolean AVAILABLE = loadFlashback();

    private FlashbackCompat() {
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(ReplayCrystalPayload.ID, ReplayCrystalPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ReplayCrystalPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (isReplayActive()) {
                        CrystalPredictor.handleReplayCrystalPayload(payload);
                    }
                })
        );
    }

    public static boolean isReplayActive() {
        if (!AVAILABLE) {
            return false;
        }

        try {
            return (boolean) replayCheck.invoke(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static int replayTick() {
        if (!AVAILABLE || !isReplayActive()) {
            return -1;
        }

        try {
            Object replayServer = replayServerGetter.invoke(null);
            if (replayServer == null) {
                return -1;
            }
            return ((Number) replayTickGetter.invoke(replayServer)).intValue();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return -1;
        }
    }

    public static void recordCrystalSpawn(BlockPos pos, int ttlTicks) {
        record(new ReplayCrystalPayload(ReplayCrystalPayload.Action.SPAWN, pos, Math.max(1, ttlTicks)));
    }

    public static void recordCrystalRemove(BlockPos pos) {
        record(new ReplayCrystalPayload(ReplayCrystalPayload.Action.REMOVE, pos, 0));
    }

    private static void record(ReplayCrystalPayload payload) {
        if (!AVAILABLE || isReplayActive()) {
            return;
        }

        try {
            Object recorder = recorderField.get(null);
            if (recorder == null || !(boolean) recorderReady.invoke(recorder)) {
                return;
            }

            writePacket.invoke(recorder, new CustomPayloadS2CPacket(payload), playPhase);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static boolean loadFlashback() {
        if (!FabricLoader.getInstance().isModLoaded("flashback")) {
            return false;
        }

        try {
            ClassLoader loader = FlashbackCompat.class.getClassLoader();
            Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback", false, loader);
            Class<?> recorder = Class.forName("com.moulberry.flashback.record.Recorder", false, loader);
            Class<?> replayServer = Class.forName("com.moulberry.flashback.playback.ReplayServer", false, loader);

            recorderField = flashback.getField("RECORDER");
            replayCheck = flashback.getMethod("isInReplay");
            replayServerGetter = flashback.getMethod("getReplayServer");
            replayTickGetter = replayServer.getMethod("getReplayTick");
            recorderReady = recorder.getMethod("readyToWrite");

            for (Method method : recorder.getMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!method.getName().equals("writePacketAsync") || parameters.length != 2) {
                    continue;
                }
                if (!parameters[0].isAssignableFrom(CustomPayloadS2CPacket.class)) {
                    continue;
                }

                Object phase = parameters[1].getField("PLAY").get(null);
                writePacket = method;
                playPhase = phase;
                break;
            }

            return writePacket != null && playPhase != null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
