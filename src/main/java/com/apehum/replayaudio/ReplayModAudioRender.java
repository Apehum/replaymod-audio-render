package com.apehum.replayaudio;

import com.replaymod.render.rendering.VideoRenderer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplayModAudioRender implements ModInitializer {
	public static final String MOD_ID = "replaymodaudiorender";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
	}

	public static AudioRender AUDIO_RENDER;

	public static boolean isRendering() {
		return AUDIO_RENDER != null;
	}

	public static void startRender(@NotNull VideoRenderer renderer) {
		if (AUDIO_RENDER != null) return;

		AUDIO_RENDER = new AudioRender(renderer);
		Minecraft.getInstance().executeBlocking(ReplayModAudioRender::reloadDevice);
	}

	public static void stopRender(@NotNull VideoRenderer videoRenderer) {
		if (AUDIO_RENDER == null) return;

		AUDIO_RENDER.flush();
		AUDIO_RENDER = null;

		Minecraft.getInstance().executeBlocking(ReplayModAudioRender::reloadDevice);
	}

	private static void reloadDevice() {
		Minecraft.getInstance().getSoundManager().reload();
	}
}