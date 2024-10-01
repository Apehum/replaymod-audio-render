package com.apehum.replayaudio.mixin;

import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface MixinSoundEngineAccessor {

    @Accessor
    Library getLibrary();
}
