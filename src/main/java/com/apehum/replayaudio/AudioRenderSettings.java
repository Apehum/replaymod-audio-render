package com.apehum.replayaudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} else {
/*import net.neoforged.fml.loading.FMLPaths;
*///?}

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioRenderSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            //? if fabric {
            FabricLoader.getInstance().getConfigDir().resolve("replaymodaudiorender.json");
            //?} else {
            /*FMLPaths.CONFIGDIR.get().resolve("replaymodaudiorender.json");
            *///?}

    private static AudioRenderSettings INSTANCE;

    public boolean enabled = true;
    public boolean mergeIntoVideo = false;
    public AudioCodec codec = AudioCodec.AAC;
    public boolean stereo = true;
    public transient File outputFile = null;

    public static AudioRenderSettings get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    private static AudioRenderSettings load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                AudioRenderSettings loaded = GSON.fromJson(
                        new String(Files.readAllBytes(CONFIG_PATH), StandardCharsets.UTF_8),
                        AudioRenderSettings.class
                );
                if (loaded != null) {
                    if (loaded.codec == null) loaded.codec = AudioCodec.AAC;
                    return loaded;
                }
            }
        } catch (IOException e) {
            ReplayModAudioRender.LOGGER.warn("Failed to load audio render settings", e);
        }
        return new AudioRenderSettings();
    }

    public void save() {
        try {
            Files.write(CONFIG_PATH, GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            ReplayModAudioRender.LOGGER.warn("Failed to save audio render settings", e);
        }
    }
}
