package com.apehum.replayaudio.mixin;

import com.apehum.replayaudio.AudioCodec;
import com.apehum.replayaudio.AudioRender;
import com.apehum.replayaudio.AudioRenderSettings;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiPanel;
import com.replaymod.lib.de.johni0702.minecraft.gui.container.GuiVerticalList;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiButton;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiCheckbox;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiElement;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiLabel;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.GuiTooltip;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.IGuiClickable;
import com.replaymod.lib.de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.GridLayout;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.LayoutData;
import com.replaymod.lib.de.johni0702.minecraft.gui.layout.VerticalLayout;
import com.replaymod.lib.de.johni0702.minecraft.gui.popup.GuiFileChooserPopup;
import com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import com.replaymod.render.gui.GuiRenderSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(GuiRenderSettings.class)
public abstract class MixinGuiRenderSettings {
    @Shadow @Final public GuiVerticalList settingsList;
    @Shadow @Final public GuiPanel mainPanel;
    @Shadow private File outputFile;

    @Unique private AudioRenderSettings replayaudio$settings;
    @Unique private GuiCheckbox replayaudio$recordCheckbox;
    @Unique private GuiCheckbox replayaudio$mergeCheckbox;
    @Unique private GuiDropdownMenu<AudioCodec> replayaudio$codecDropdown;
    @Unique private GuiCheckbox replayaudio$stereoCheckbox;
    @Unique private GuiButton replayaudio$outputFileButton;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void replayaudio$addAudioPanel(CallbackInfo ci) {
        replayaudio$settings = AudioRenderSettings.get();

        replayaudio$recordCheckbox = new GuiCheckbox()
                .setI18nLabel("replayaudio.gui.rendersettings.render")
                .setChecked(replayaudio$settings.enabled);

        replayaudio$mergeCheckbox = new GuiCheckbox()
                .setI18nLabel("replayaudio.gui.rendersettings.merge")
                .setChecked(replayaudio$settings.mergeIntoVideo);

        replayaudio$codecDropdown = new GuiDropdownMenu<AudioCodec>()
                .setMinSize(new Dimension(200, 20))
                .setValues(AudioCodec.values())
                .setSelected(replayaudio$settings.codec);

        replayaudio$stereoCheckbox = new GuiCheckbox()
                .setI18nLabel("replayaudio.gui.rendersettings.stereo")
                .setChecked(replayaudio$settings.stereo);

        replayaudio$outputFileButton = new GuiButton()
                .setMinSize(new Dimension(200, 20));

        GuiPanel audioPanel = new GuiPanel()
                .setLayout(new VerticalLayout().setSpacing(15))
                .addElements(
                        null,
                        replayaudio$recordCheckbox,
                        replayaudio$mergeCheckbox,
                        replayaudio$stereoCheckbox,
                        new GuiPanel().setLayout(
                                new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(5).setSpacingY(15)
                        )
                                .addElements(
                                        new GridLayout.Data(0, 0.5),

                                        new GuiLabel()
                                                .setMinSize(new Dimension(100, 0))
                                                .setI18nText("replayaudio.gui.rendersettings.codec"),
                                        replayaudio$codecDropdown,

                                        new GuiLabel()
                                                .setMinSize(new Dimension(100, 0))
                                                .setI18nText("replayaudio.gui.rendersettings.outputfile"),
                                        replayaudio$outputFileButton
                                )
                );

        replayaudio$onClick(replayaudio$recordCheckbox, () -> {
            replayaudio$settings.enabled = replayaudio$recordCheckbox.isChecked();
            replayaudio$settings.save();
            replayaudio$updateEnabled();
        });
        replayaudio$onClick(replayaudio$mergeCheckbox, () -> {
            replayaudio$settings.mergeIntoVideo = replayaudio$mergeCheckbox.isChecked();
            replayaudio$settings.save();
            replayaudio$updateEnabled();
        });
        replayaudio$onClick(replayaudio$stereoCheckbox, () -> {
            replayaudio$settings.stereo = replayaudio$stereoCheckbox.isChecked();
            replayaudio$settings.save();
        });
        replayaudio$codecDropdown.onSelection(old -> {
            replayaudio$settings.codec = replayaudio$codecDropdown.getSelectedValue();
            replayaudio$settings.save();
            replayaudio$updateOutputButtonLabel();
        });
        replayaudio$onClick(replayaudio$outputFileButton, () -> {
            GuiRenderSettings self = (GuiRenderSettings) (Object) this;

            GuiFileChooserPopup popup = GuiFileChooserPopup.openSaveGui(
                    self,
                    "replaymod.gui.save",
                    replayaudio$settings.codec.extension
            );

            if (replayaudio$settings.outputFile != null) {
                File current = replayaudio$settings.outputFile;
                if (current.getParentFile() != null) popup.setFolder(current.getParentFile());
                popup.setFileName(current.getName());
            } else if (outputFile != null) {
                if (outputFile.getParentFile() != null) popup.setFolder(outputFile.getParentFile());
                popup.setFileName(replayaudio$derivedAudioFileName());
            }
            popup.onAccept(file -> {
                replayaudio$settings.outputFile = file;
                replayaudio$settings.save();
                replayaudio$updateOutputButtonLabel();
            });
        });

        replayaudio$updateEnabled();
        replayaudio$updateOutputButtonLabel();

        GuiPanel list = settingsList.getListPanel();
        Map<GuiElement, LayoutData> snapshot = new LinkedHashMap<>(list.getElements());
        snapshot.keySet().forEach(list::removeElement);
        for (Map.Entry<GuiElement, LayoutData> e : snapshot.entrySet()) {
            list.addElements(e.getValue(), e.getKey());
            if (e.getKey() == mainPanel) {
                list.addElements(
                        new VerticalLayout.Data(0.5),
                        new GuiPanel(),
                        new GuiLabel().setI18nText("replayaudio.gui.rendersettings.audio"),
                        audioPanel
                );
            }
        }
    }

    @Unique
    private void replayaudio$updateEnabled() {
        if (!replayaudio$recordCheckbox.isChecked()) {
            replayaudio$mergeCheckbox.setDisabled();
            replayaudio$stereoCheckbox.setDisabled();
            replayaudio$codecDropdown.setDisabled();
            replayaudio$outputFileButton.setDisabled();
            replayaudio$codecDropdown.setTooltip(null);
            return;
        }

        replayaudio$mergeCheckbox.setEnabled();
        replayaudio$stereoCheckbox.setEnabled();

        if (replayaudio$mergeCheckbox.isChecked()) {
            replayaudio$codecDropdown.setDisabled();
            replayaudio$outputFileButton.setDisabled();
            replayaudio$codecDropdown.setTooltip(
                    new GuiTooltip().setI18nText("replayaudio.gui.rendersettings.codec.mergetooltip")
            );
        } else {
            replayaudio$codecDropdown.setEnabled();
            replayaudio$outputFileButton.setEnabled();
            replayaudio$codecDropdown.setTooltip(null);
        }
    }

    @Unique
    private void replayaudio$updateOutputButtonLabel() {
        if (replayaudio$settings.outputFile != null) {
            replayaudio$outputFileButton.setLabel(replayaudio$settings.outputFile.getName());
        } else {
            replayaudio$outputFileButton.setLabel(replayaudio$derivedAudioFileName());
        }
    }

    @Unique
    private String replayaudio$derivedAudioFileName() {
        if (outputFile == null) return "." + replayaudio$settings.codec.extension;
        return AudioRender.deriveAudioFile(outputFile, replayaudio$settings.codec).getName();
    }

    @Unique
    private static void replayaudio$onClick(IGuiClickable<?> element, Runnable handler) {
        element.onClick(handler);
    }
}
