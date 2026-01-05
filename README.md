**UPDATE** 
(read since the old code does not aline with this update these are the changes)

*What was broken*

Right-click actions (eat, shield, bow, etc.) wouldn’t work when the mod was on.

Keyboard “use/place” still worked, so I didn’t notice at first. - I use keyboard place so it wasn't an issue for me, I wanted to make sure it works nicely with keyboard placing, learned from this and will test with right click from now on.

*Why it happened*

I was returning SUCCESS on block use, which consumed the click and blocked vanilla right-click actions.

I also spawned a client-only crystal into the world, which gave it a hitbox. The mouse ray hit the fake crystal instead of the item/block, so right-click use never reached vanilla.

*What I changed*

I no longer consume the click; I return PASS so vanilla keeps running.

I don’t add a fake entity to the world anymore. I render a ghost crystal only during world render, so there’s no hitbox to steal focus.

Prediction triggers on the rising edge of “use” and only when the base is valid.

*Before vs After**

Old (problematic)

UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
    spawnPredicted(crystalPos);
    return ActionResult.SUCCESS;
});

world.addEntity(predictedCrystal);


New (fixed)

UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
    spawnGhost(crystalPos);
    return ActionResult.PASS;
});

WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
    dispatcher.render(ghost, x, y, z, 0f, td, matrices, consumers, light);
});


# Code Explained

config/ConfigManager

This just loads and saves the mod’s config JSON.
It stores the “instant” toggle (and whatever other simple settings are in the data object), then writes it back to disk when it changes.

core/ClientHooks

This is the client entrypoint.
It loads the config, turns the predictor on or off based on the config setting, then registers the client-side event hooks.
It hooks into using blocks (placing crystals), entity load/unload (seeing real crystals appear or disappear), and client tick (updating the prediction state every tick).

core/CrystalPredictor

This is the core logic.
When you try to place a crystal, it can spawn a temporary local crystal instantly on your client so you don’t feel that “wait for server” delay.
Then it watches for the real server crystal to show up (or for the action to fail), and removes the temporary one so everything stays consistent with server reality.

It also runs a tick loop that keeps the temporary crystals positioned correctly and deletes any that have expired.
It checks your attack key as well, so it can react to “I’m trying to break a crystal” situations and clean up predictions that should no longer exist.

gui/ModSettingsScreen

This is a simple in-game settings screen.
It basically just gives you a button to toggle the instant prediction setting and a done button to close the screen.

util/ModMenuIntegration

This is the Mod Menu hook.
It makes it so clicking the mod in Mod Menu opens the mod’s settings screen.





Source code isn't updated with these small changes, I just decided to add this here incase people are curious, these are the only changes everything else is the same.
