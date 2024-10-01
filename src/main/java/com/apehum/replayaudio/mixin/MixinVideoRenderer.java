package com.apehum.replayaudio.mixin;

import com.replaymod.render.rendering.VideoRenderer;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VideoRenderer.class)
public class MixinVideoRenderer {

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;set(Ljava/lang/Object;)V"))
    private void setSoundVolume(OptionInstance<?> instance, Object object) {
        // do nothing
    }
}
