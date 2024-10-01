package com.apehum.replayaudio.mixin;

import com.apehum.replayaudio.ReplayModAudioRender;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.audio.Library;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.IntBuffer;

@Mixin(Library.class)
public class MixinLibrary {

    @Shadow private long currentDevice;

    @Inject(method = "openDeviceOrFallback", at = @At("HEAD"), cancellable = true)
    private static void openDeviceOrFallback(String deviceName, CallbackInfoReturnable<Long> cir) {
        if (!ReplayModAudioRender.isRendering()) return;

        var devicePointer = SOFTLoopback.alcLoopbackOpenDeviceSOFT((String) null);
        cir.setReturnValue(devicePointer);
    }

    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC10;alcCreateContext(JLjava/nio/IntBuffer;)J"))
    private long initCreateContext(long deviceHandle, IntBuffer attrList, Operation<Long> original) {
        if (!ReplayModAudioRender.isRendering()) {
            return original.call(deviceHandle, attrList);
        }

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer intBuffer = memoryStack.callocInt(7)
                    .put(ALC10.ALC_FREQUENCY).put(48000)
                    .put(SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT).put(SOFTLoopback.ALC_STEREO_SOFT)
                    .put(SOFTLoopback.ALC_FORMAT_TYPE_SOFT).put(SOFTLoopback.ALC_SHORT_SOFT)
                    .put(0)
                    .flip();
            return ALC10.alcCreateContext(this.currentDevice, intBuffer);
        }
    }
}
