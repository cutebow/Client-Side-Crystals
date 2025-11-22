**UPDATE**

*What was broken*

Right-click actions (eat, shield, bow, etc.) wouldn’t fire when the mod was on.

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
