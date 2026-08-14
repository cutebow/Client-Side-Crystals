package me.clientsidecrystals.mixin;

import me.clientsidecrystals.config.ConfigManager;
import me.clientsidecrystals.core.CrystalPredictor;
import me.clientsidecrystals.core.SeamlessCrystalBridge;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class EndCrystalEntityRendererSeamlessMixin {
    @Unique
    private static final Map<EndCrystalEntityRenderState, Boolean> csc$localStates = new WeakHashMap<>();

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void csc$trackState(
            EndCrystalEntity entity,
            EndCrystalEntityRenderState state,
            float tickDelta,
            CallbackInfo ci
    ) {
        boolean local = CrystalPredictor.isLocalCrystal(entity);
        csc$localStates.put(state, local);

        if (local) {
            SeamlessCrystalBridge.recordRenderedAge(entity.getId(), state.age);
            return;
        }

        if (ConfigManager.config.instantEnabled && ConfigManager.config.seamlessEnabled) {
            state.age = SeamlessCrystalBridge.apply(entity.getId(), state.age);
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
            )
    )
    private void csc$redirectSubmitModel(
            OrderedRenderCommandQueue queue,
            Model<Object> model,
            Object stateObject,
            MatrixStack matrices,
            RenderLayer renderLayer,
            int light,
            int overlay,
            int outlineColor,
            ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay
    ) {
        if (stateObject instanceof EndCrystalEntityRenderState state
                && csc$localStates.getOrDefault(state, false)
                && ConfigManager.config.colorFakeCrystal) {
            queue.submitModel(
                    model,
                    stateObject,
                    matrices,
                    renderLayer,
                    light,
                    overlay,
                    ConfigManager.config.fakeCrystalColor,
                    null,
                    outlineColor,
                    crumblingOverlay
            );
            return;
        }

        queue.submitModel(
                model,
                stateObject,
                matrices,
                renderLayer,
                light,
                overlay,
                outlineColor,
                crumblingOverlay
        );
    }
}
