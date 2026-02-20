package me.clientsidecrystals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data config = new Data();

    private static Path cfgPath() {
        Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
        return dir.resolve("clientsidecrystals.json");
    }

    public static void load() {
        try {
            Path p = cfgPath();
            if (!Files.exists(p)) {
                save();
                return;
            }
            try (BufferedReader r = Files.newBufferedReader(p)) {
                Data read = GSON.fromJson(r, Data.class);
                if (read != null) config = read;
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Path p = cfgPath();
            Files.createDirectories(p.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(p)) {
                w.write(GSON.toJson(config));
            }
        } catch (IOException ignored) {}
    }

    public static final class Data {
        public boolean instantEnabled = true;
        public boolean seamlessEnabled = true;
        public int predictionTimeoutTicks = 12;
    }

    private ConfigManager() {}
}
