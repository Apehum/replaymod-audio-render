package com.apehum.replayaudio;

import com.apehum.replayaudio.mixin.MixinLibraryAccessor;
import com.apehum.replayaudio.mixin.MixinSoundEngineAccessor;
import com.apehum.replayaudio.mixin.MixinSoundManagerAccessor;
import com.replaymod.lib.org.apache.commons.exec.CommandLine;
import com.replaymod.render.rendering.VideoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.SOFTLoopback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public final class AudioRender {

    private final @NotNull VideoRenderer videoRenderer;

    private final @NotNull Process process;
    private final @NotNull InputStream inputStream;
    private final @NotNull OutputStream outputStream;

    public AudioRender(final @NotNull VideoRenderer videoRenderer) {
        this.videoRenderer = videoRenderer;

        var outputVideoFile = videoRenderer.getRenderSettings().getOutputFile();
        var outputFolder = outputVideoFile.getParentFile();
        var outputFileNameSplit = outputVideoFile.getName().split("\\.");
        var outputFileName = String.join(".", Arrays.copyOf(outputFileNameSplit, outputFileNameSplit.length - 1)) + ".aac";

        var ffmpegCommand = videoRenderer.getRenderSettings().getExportCommandOrDefault();
        var commandArguments = "-y -f s16le -ar 48000 -ac 2 -i - -c:a aac " + outputFileName;

        ReplayModAudioRender.LOGGER.info("ffmpeg command arguments: {}", commandArguments);

        var commandLine = (new CommandLine(ffmpegCommand)).addArguments(commandArguments, false).toStrings();
        try {
            process = (new ProcessBuilder(commandLine))
                    .directory(outputFolder)
                    .redirectErrorStream(true)
                    .start();

            inputStream = process.getInputStream();
            outputStream = process.getOutputStream();
        } catch (IOException e) {
            ReplayModAudioRender.LOGGER.info("Failed to create ffmpeg process", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized void submit() {
        var minecraft = Minecraft.getInstance();

        var soundManager = (MixinSoundManagerAccessor) minecraft.getSoundManager();
        if (soundManager == null) return;

        var soundEngine = (MixinSoundEngineAccessor) soundManager.getSoundEngine();
        var library = (MixinLibraryAccessor) soundEngine.getLibrary();

        long devicePointer = library.getCurrentDevice();
        if (devicePointer == 0L) return;

        int fps = videoRenderer.getRenderSettings().getFramesPerSecond();
        int frameSize = 48000 / fps;

        var camera = minecraft.gameRenderer.getMainCamera();
        ((SoundEngine) soundEngine).updateSource(camera);

        short[] shortsBuffer = new short[frameSize * 2];
        SOFTLoopback.alcRenderSamplesSOFT(devicePointer, shortsBuffer, frameSize);

        try {
            outputStream.write(shortsToBytes(shortsBuffer));

            byte[] available = new byte[inputStream.available()];
            inputStream.read(available);
        } catch (IOException e) {
            ReplayModAudioRender.LOGGER.info("Failed to write to ffmpeg stdin", e);
        }
    }

    public synchronized void flush() {
        try {
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            ReplayModAudioRender.LOGGER.info("Failed to exit ffmpeg process", e);
        }
        process.destroy();
    }

    public static byte[] shortsToBytes(short[] shorts) {
        byte[] bytes = new byte[shorts.length * 2];

        for (int i = 0; i < bytes.length; i += 2) {
            byte[] sample = shortToBytes(shorts[i / 2]);
            bytes[i] = sample[0];
            bytes[i + 1] = sample[1];
        }

        return bytes;
    }

    public static byte[] shortToBytes(short s) {
        return new byte[]{(byte) (s & 0xFF), (byte) ((s >> 8) & 0xFF)};
    }
}
