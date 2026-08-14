package me.clientsidecrystals.mixin;

import me.clientsidecrystals.util.WestShopNameStyle;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class WestShopServerEntryMixin {
    @Shadow
    @Final
    private ServerInfo server;

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/DrawContext;IIZF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"
            )
    )
    private void csc$drawServerName(
            DrawContext context,
            TextRenderer renderer,
            String text,
            int x,
            int y,
            int color
    ) {
        if (WestShopNameStyle.isWestShop(server.address)) {
            WestShopNameStyle.drawAnimatedName(context, renderer, x, y);
            return;
        }
        if (WestShopNameStyle.isNaPvP(server.address)) {
            WestShopNameStyle.drawAnimatedNaPvPName(context, renderer, text, x, y);
            return;
        }

        context.drawTextWithShadow(renderer, text, x, y, color);
    }
}
