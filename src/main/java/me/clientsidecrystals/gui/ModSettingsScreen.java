package me.clientsidecrystals.gui;

import me.clientsidecrystals.config.ConfigManager;
import me.clientsidecrystals.core.CrystalPredictor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ModSettingsScreen extends Screen {
    private final Screen parent;

    public ModSettingsScreen(Screen parent) {
        super(Text.literal("ClientSideCrystals Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 6 + 40;

        this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleInstantLabel()), b -> {
            boolean next = !ConfigManager.config.instantEnabled;
            ConfigManager.config.instantEnabled = next;
            ConfigManager.save();
            CrystalPredictor.setEnabled(next);
            b.setMessage(Text.literal(toggleInstantLabel()));
        }).dimensions(cx - 100, y, 200, 20).build());

        y += 24;

        this.addDrawableChild(ButtonWidget.builder(Text.literal(toggleSeamlessLabel()), b -> {
            ConfigManager.config.seamlessEnabled = !ConfigManager.config.seamlessEnabled;
            ConfigManager.save();
            b.setMessage(Text.literal(toggleSeamlessLabel()));
        }).dimensions(cx - 100, y, 200, 20).build());

        y += 36;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx - 60, y, 120, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    private static String toggleInstantLabel() {
        return "Instant: " + (ConfigManager.config.instantEnabled ? "ON" : "OFF");
    }

    private static String toggleSeamlessLabel() {
        return "Seamless: " + (ConfigManager.config.seamlessEnabled ? "ON" : "OFF");
    }
}
