package com.apehum.replayaudio;

import com.replaymod.core.Module;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.render.events.ReplayRenderCallback;

public class AudioRenderModule extends EventRegistrations implements Module {

    @Override
    public void initClient() {
        on(ReplayRenderCallback.Pre.EVENT, ReplayModAudioRender::startRender);
        on(ReplayRenderCallback.Post.EVENT, ReplayModAudioRender::stopRender);
        register();
    }
}
