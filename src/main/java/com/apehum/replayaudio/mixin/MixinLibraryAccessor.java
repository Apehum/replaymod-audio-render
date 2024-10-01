package com.apehum.replayaudio.mixin;

import com.mojang.blaze3d.audio.Library;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Library.class)
public interface MixinLibraryAccessor {

    @Accessor
    long getCurrentDevice();
}
