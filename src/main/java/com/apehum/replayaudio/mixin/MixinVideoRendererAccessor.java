package com.apehum.replayaudio.mixin;

import com.replaymod.render.FFmpegWriter;
import com.replaymod.render.rendering.VideoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VideoRenderer.class, remap = false)
public interface MixinVideoRendererAccessor {

    @Accessor("ffmpegWriter")
    FFmpegWriter getFfmpegWriter();
}
