package me.clientsidecrystals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ClientSideCrystals/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "clientsidecrystals.json";

    public static Data config = new Data();

    private ConfigManager() {
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            save();
            return;
        }

        boolean migrated = false;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            Data loaded = GSON.fromJson(object, Data.class);
            if (loaded != null) {
                if (!object.has("seamlessEnabled")) { loaded.seamlessEnabled = true; migrated = true; }
                if (!object.has("colorFakeCrystal")) { loaded.colorFakeCrystal = false; migrated = true; }
                if (!object.has("fakeCrystalColor")) { loaded.fakeCrystalColor = 0xFFFF55FF; migrated = true; }
                config = loaded;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load Client Side Crystals config", e);
        }
        if (migrated) save();
    }

    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save Client Side Crystals config", e);
        }
    }

    public static Color fakeCrystalColor() {
        return new Color(config.fakeCrystalColor, true);
    }

    private static Path configPath() {
        return MinecraftClient.getInstance()
                .runDirectory
                .toPath()
                .resolve("config")
                .resolve(CONFIG_FILE_NAME);
    }

    public static final class Data {
        public boolean instantEnabled = true;
        public boolean seamlessEnabled = true;
        public boolean instantArmSwing;
        public int predictionTimeoutTicks = 12;
        public boolean colorFakeCrystal;
        public int fakeCrystalColor = 0xFFFF55FF;
    }
}
