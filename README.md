Client Side Crystals shows a crystal preview on your screen when placing crystals. It is client side only and visual only. It does not place real crystals, it does not change damage, and it does not change anything on the server. Real crystals are still handled by vanilla and by the server.

**When a crystal is placed, a temporary client crystal is shown right away so placement feels instant. When the real crystal appears from the server, the preview is removed. If the place fails, the preview times out and disappears. actions pass through the fake crystal and the mod just listens for those inputs and removes the fake crystal so you target the real one and never hit the fake, which you cannot even hit anyways since it just passes through it.**

# Code explained by class
**config/ConfigManager**

Loads and saves the config file clientsidecrystals.json.
Stores three settings: instantEnabled, seamlessEnabled, predictionTimeoutTicks.

**core/ClientHooks**

This is the client entrypoint.
Loads the config, turns the predictor on or off, and registers all client events.
Block use is registered with PASS so vanilla right click actions keep working.
Also opens the settings screen.

**core/CrystalPredictor**

Main logic for the visual preview.
When the crystal item is used on a valid base, it spawns a temporary client crystal at the placement spot and tracks it.
On tick it keeps the preview positioned, and removes it when the real crystal shows up or when the timeout is reached.
If seamless is enabled, it links the preview to the real crystal for a smooth handoff.

**core/SeamlessCrystalBridge**

Small helper used only for the seamless option.
Tracks a short hide window and an age offset for real crystals so the switch from preview to real looks smoother.

**gui/ModSettingsScreen**

Simple settings screen with two toggles.
Instant turns the preview on or off.
Seamless turns the smooth handoff on or off.

**util/ModMenuIntegration**

Mod Menu hook.
Makes Mod Menu open ModSettingsScreen.

**mixin/EntityAgeAccessor**

Accessor for the entity age field.
Used to shift the crystal animation age when seamless is enabled.

**mixin/EndCrystalEntityRendererSeamlessMixin**

Render mixin for crystals.
Applies the age shift during render state update and hides the real crystal for a very short time when needed, so the handoff does not look like a sudden reset this is for the seemless look so it doesnt look weird.
