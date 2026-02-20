package me.clientsidecrystals.mixin;

import me.clientsidecrystals.config.ConfigManager;
import me.clientsidecrystals.core.CrystalPredictor;
import me.clientsidecrystals.core.SeamlessCrystalBridge;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(EndCrystalEntityRenderer.class)
public class EndCrystalEntityRendererSeamlessMixin {
    @Unique private static final Map<EndCrystalEntityRenderState, Integer> CSC$STATE_TO_ID = new IdentityHashMap<>();
    @Unique private static final ThreadLocal<Boolean> CSC$SHIFTED = ThreadLocal.withInitial(() -> false);
    @Unique private static final ThreadLocal<Integer> CSC$OLD_AGE = ThreadLocal.withInitial(() -> 0);

    @Inject(
            method = "updateRenderState",
            at = @At("HEAD")
    )
    private void csc$shiftAgeBeforeUpdate(EndCrystalEntity entity, EndCrystalEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!ConfigManager.config.instantEnabled) return;
        if (!ConfigManager.config.seamlessEnabled) return;

        int id = entity.getId();
        int d = SeamlessCrystalBridge.ageDeltaFor(id);
        if (d == 0) return;

        int old = ((EntityAgeAccessor) (Object) entity).clientsidecrystals$getAge();
        ((EntityAgeAccessor) (Object) entity).clientsidecrystals$setAge(old + d);

        CSC$SHIFTED.set(true);
        CSC$OLD_AGE.set(old);
    }

    @Inject(
            method = "updateRenderState",
            at = @At("TAIL")
    )
    private void csc$trackAndRestore(EndCrystalEntity entity, EndCrystalEntityRenderState state, float tickDelta, CallbackInfo ci) {
        CSC$STATE_TO_ID.put(state, entity.getId());

        if (CSC$SHIFTED.get()) {
            ((EntityAgeAccessor) (Object) entity).clientsidecrystals$setAge(CSC$OLD_AGE.get());
            CSC$SHIFTED.set(false);
            CSC$OLD_AGE.set(0);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void csc$hideRealForSeamless(EndCrystalEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        if (!ConfigManager.config.instantEnabled) return;
        if (!ConfigManager.config.seamlessEnabled) return;

        Integer idObj = CSC$STATE_TO_ID.get(state);
        if (idObj == null) return;
        int id = idObj.intValue();

        if (SeamlessCrystalBridge.shouldHide(id, CrystalPredictor.debugTick())) {
            ci.cancel();
        }
    }
}
