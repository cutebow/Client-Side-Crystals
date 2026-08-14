package me.clientsidecrystals.gui;

import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import me.clientsidecrystals.config.ConfigManager;
import me.clientsidecrystals.core.CrystalPredictor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ModSettingsScreen {
    private ModSettingsScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigCategory general = ConfigCategory.createBuilder()
                .name(Text.literal("General"))
                .option(booleanOption(
                        "Instant",
                        true,
                        () -> ConfigManager.config.instantEnabled,
                        value -> {
                            ConfigManager.config.instantEnabled = value;
                            CrystalPredictor.setEnabled(value);
                            ConfigManager.save();
                        }
                ))
                .option(booleanOption(
                        "Seamless",
                        true,
                        () -> ConfigManager.config.seamlessEnabled,
                        value -> {
                            ConfigManager.config.seamlessEnabled = value;
                            ConfigManager.save();
                        }
                ))
                .option(booleanOption(
                        "Instant Arm Swing",
                        false,
                        () -> ConfigManager.config.instantArmSwing,
                        value -> {
                            ConfigManager.config.instantArmSwing = value;
                            ConfigManager.save();
                        }
                ))
                .option(booleanOption(
                        "Color Fake Crystal",
                        false,
                        () -> ConfigManager.config.colorFakeCrystal,
                        value -> {
                            ConfigManager.config.colorFakeCrystal = value;
                            ConfigManager.save();
                        }
                ))
                .option(Option.createBuilder(Color.class)
                        .name(Text.literal("Fake Crystal Color"))
                        .binding(Binding.generic(
                                new Color(0xFF55FF),
                                ConfigManager::fakeCrystalColor,
                                value -> {
                                    ConfigManager.config.fakeCrystalColor =
                                            0xFF000000 | (value.getRGB() & 0x00FFFFFF);
                                    ConfigManager.save();
                                }
                        ))
                        .controller(ColorControllerBuilder::create)
                        .instant(true)
                        .build())
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Client Side Crystals"))
                .category(general)
                .save(ConfigManager::save)
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> booleanOption(
            String name,
            boolean defaultValue,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter
    ) {
        return Option.createBuilder(Boolean.class)
                .name(Text.literal(name))
                .binding(Binding.generic(defaultValue, getter, setter))
                .controller(TickBoxControllerBuilder::create)
                .instant(true)
                .build();
    }
}
