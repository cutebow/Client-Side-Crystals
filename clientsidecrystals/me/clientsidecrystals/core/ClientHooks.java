package me.clientsidecrystals.core;

import me.clientsidecrystals.config.ConfigManager;
import me.clientsidecrystals.gui.ModSettingsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;

public final class ClientHooks implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        CrystalPredictor.setEnabled(ConfigManager.config.instantEnabled);

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (!CrystalPredictor.isEnabled()) return ActionResult.PASS;
            if (!player.getStackInHand(hand).isOf(Items.END_CRYSTAL)) return ActionResult.PASS;
            CrystalPredictor.onUseBlock((BlockHitResult) hit);
            return ActionResult.PASS;
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!CrystalPredictor.isEnabled()) return;
            CrystalPredictor.onEntityLoaded(entity);
        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!CrystalPredictor.isEnabled()) return;
            CrystalPredictor.onEntityUnloaded(entity);
        });

        ClientTickEvents.START_CLIENT_TICK.register(mc -> CrystalPredictor.clientTick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> CrystalPredictor.clearAll());
    }

    public static void openSettings() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen(new ModSettingsScreen(mc.currentScreen));
    }
}
