package com.apehum.replayaudio;

import com.apehum.replayaudio.mixin.MixinLibraryAccessor;
import com.apehum.replayaudio.mixin.MixinSoundEngineAccessor;
import com.apehum.replayaudio.mixin.MixinSoundManagerAccessor;
import com.apehum.replayaudio.mixin.MixinVideoRendererAccessor;
import com.replaymod.render.FFmpegWriter;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.rendering.VideoRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.SOFTLoopback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class AudioRender {

    private final @NotNull VideoRenderer videoRenderer;
    private final int channels;
    private final boolean mergeIntoVideo;
    private final @NotNull String ffmpegExecutable;
    private final @NotNull String videoExtension;
    private final FFmpegWriter ffmpegWriter;
    private final @NotNull File audioFile;
    private final @NotNull File outputFolder;

    private final @NotNull Process process;
    private final @NotNull InputStream inputStream;
    private final @NotNull OutputStream outputStream;

    public AudioRender(final @NotNull VideoRenderer videoRenderer) {
        this.videoRenderer = videoRenderer;

        AudioRenderSettings settings = AudioRenderSettings.get();
        RenderSettings renderSettings = videoRenderer.getRenderSettings();

        this.channels = settings.stereo ? 2 : 1;
        this.ffmpegExecutable = renderSettings.getExportCommandOrDefault();

        this.videoExtension = renderSettings.getEncodingPreset().getFileExtension();

        // null for BLEND/EXR/PNG exports, which have no video stream to merge into
        this.ffmpegWriter = ((MixinVideoRendererAccessor) videoRenderer).getFfmpegWriter();
        this.mergeIntoVideo = settings.mergeIntoVideo && ffmpegWriter != null;

        if (settings.mergeIntoVideo && !mergeIntoVideo) {
            ReplayModAudioRender.LOGGER.info(
                    "Render format {} produces no video, exporting audio separately",
                    renderSettings.getEncodingPreset()
            );
        }

        AudioCodec codec = mergeIntoVideo ? mergeCodecFor(videoExtension) : settings.codec;

        this.audioFile = settings.outputFile != null
                ? settings.outputFile
                : deriveAudioFile(renderSettings.getOutputFile(), codec);

        this.outputFolder = audioFile.getParentFile();

        String[] commandLine = {
                ffmpegExecutable, "-y",
                "-f", "s16le",
                "-ar", "48000",
                "-ac", String.valueOf(channels),
                "-i", "-",
                "-c:a", codec.ffmpegCodec,
                audioFile.getAbsolutePath()
        };

        try {
            process = startFFmpeg(commandLine);

            inputStream = process.getInputStream();
            outputStream = process.getOutputStream();
        } catch (IOException e) {
            ReplayModAudioRender.LOGGER.info("Failed to create ffmpeg process", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized void submit() {
        Minecraft minecraft = Minecraft.getInstance();

        MixinSoundManagerAccessor soundManager = (MixinSoundManagerAccessor) minecraft.getSoundManager();
        if (soundManager == null) return;

        MixinSoundEngineAccessor soundEngine = (MixinSoundEngineAccessor) soundManager.getSoundEngine();
        MixinLibraryAccessor library = (MixinLibraryAccessor) soundEngine.getLibrary();

        long devicePointer = library.getCurrentDevice();
        if (devicePointer == 0L) return;

        int fps = videoRenderer.getRenderSettings().getFramesPerSecond();
        int frameSize = 48000 / fps;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        ((SoundEngine) soundEngine).updateSource(camera);

        short[] shortsBuffer = new short[frameSize * channels];
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

        if (mergeIntoVideo) {
            mergeIntoVideo();
        }
    }

    private void mergeIntoVideo() {
        File videoFile;
        try {
            videoFile = ffmpegWriter.getVideoFile();
        } catch (IOException e) {
            ReplayModAudioRender.LOGGER.warn("Skipping audio merge: ffmpeg wrote no video file", e);
            return;
        }

        if (!videoFile.isFile() || videoFile.length() == 0) {
            ReplayModAudioRender.LOGGER.warn("Skipping audio merge: video file {} is missing", videoFile);
            return;
        }
        if (!audioFile.isFile() || audioFile.length() == 0) {
            ReplayModAudioRender.LOGGER.warn("Skipping audio merge: audio file {} is missing", audioFile);
            return;
        }

        File mergedFile = new File(
                videoFile.getParentFile(),
                baseName(videoFile) + "-merged." + videoExtension
        );

        // -c copy remuxes both already-encoded streams into the container without re-encoding
        String[] commandLine = {
                ffmpegExecutable, "-y",
                "-i", videoFile.getAbsolutePath(),
                "-i", audioFile.getAbsolutePath(),
                "-c", "copy",
                "-map", "0:v:0",
                "-map", "1:a:0",
                "-shortest",
                mergedFile.getAbsolutePath()
        };

        try {
            Process mergeProcess = startFFmpeg(commandLine);

            drain(mergeProcess.getInputStream());
            int exitCode = mergeProcess.waitFor();

            if (exitCode == 0 && mergedFile.isFile() && mergedFile.length() > 0) {
                Files.move(mergedFile.toPath(), videoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(audioFile.toPath());
            } else {
                ReplayModAudioRender.LOGGER.warn(
                        "Audio merge failed (exit {}), keeping separate audio file {}",
                        exitCode,
                        audioFile
                );
                Files.deleteIfExists(mergedFile.toPath());
            }
        } catch (InterruptedException | IOException e) {
            ReplayModAudioRender.LOGGER.warn("Failed to merge audio into video", e);
        }
    }

    private Process startFFmpeg(String[] commandLine) throws IOException {
        ReplayModAudioRender.LOGGER.info("ffmpeg command: {}", String.join(" ", commandLine));

        return (new ProcessBuilder(commandLine))
                .directory(outputFolder)
                .redirectErrorStream(true)
                .start();
    }

    public static File deriveAudioFile(File videoFile, AudioCodec codec) {
        return new File(videoFile.getParentFile(), baseName(videoFile) + "." + codec.extension);
    }

    // WebM only allows Opus/Vorbis; everything else (mp4/mkv) uses AAC
    private static AudioCodec mergeCodecFor(String videoExtension) {
        return "webm".equalsIgnoreCase(videoExtension)
                ? AudioCodec.OPUS
                : AudioCodec.AAC;
    }

    private static String baseName(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static void drain(InputStream stream) throws IOException {
        byte[] buffer = new byte[4096];
        while (stream.read(buffer) != -1) {
            // discard ffmpeg output; draining prevents it from blocking on a full pipe
        }
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
