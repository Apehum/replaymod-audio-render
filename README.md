# ReplayModAudioRender

ReplayMod addon for rendering audio using loopback device.

This addon basically replaces your audio device with [OpenAL loopback device](https://github.com/openalext/openalext/blob/master/ALC_SOFT_loopback.txt) when rendering and saves the audio in AAC format.
Audio is saved in the same location and the same name (but with .aac extension) as the video.

## Download
- [Modrinth](https://modrinth.com/mod/replaymod-audio-render)

## Known issues

The audio seems slightly off-sync with video (but I'm not sure).

Game audio is completely gone when video is being rendered.

## TODO

- [ ] Config
- [ ] Disable UI sounds
- [ ] Add audio "preview" when rendering the video
- [ ] Support all versions of replaymod