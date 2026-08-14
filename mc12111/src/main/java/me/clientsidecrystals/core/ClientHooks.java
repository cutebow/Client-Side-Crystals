package me.clientsidecrystals.core;

import me.clientsidecrystals.compat.FlashbackCompat;
import me.clientsidecrystals.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

public final class ClientHooks implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FlashbackCompat.init();
        ConfigManager.load();

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world instanceof ClientWorld && player.getStackInHand(hand).isOf(Items.END_CRYSTAL)) {
                CrystalPredictor.onUseBlock(hand, hit);
            }
            return ActionResult.PASS; // geeg
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> CrystalPredictor.onEntityLoaded(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> CrystalPredictor.onEntityUnloaded(entity));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            CrystalPredictor.clientTick();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CrystalPredictor.clearAll());
    }
}
