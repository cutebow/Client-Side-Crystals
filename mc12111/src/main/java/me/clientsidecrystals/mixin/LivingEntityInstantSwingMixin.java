package me.clientsidecrystals.mixin;

import me.clientsidecrystals.core.CrystalPredictor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInstantSwingMixin {
    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
    private void csc$cancelDelayedServerSwing(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        if (!fromServerPlayer || (Object) this != MinecraftClient.getInstance().player) {
            return;
        }

        if (CrystalPredictor.consumeServerSwingSuppression(hand)) {
            ci.cancel();
        }
    }
}
