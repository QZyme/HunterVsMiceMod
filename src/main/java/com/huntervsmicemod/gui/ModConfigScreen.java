package com.huntervsmicemod.gui;

import com.huntervsmicemod.HunterVsMiceMod;
import com.huntervsmicemod.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class ModConfigScreen {

    public static Screen getScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("huntervsmicemod.config.title"))
                .setSavingRunnable(() -> {
                    AutoConfig.getConfigHolder(ModConfig.class).save();
                    HunterVsMiceMod.reloadConfig();
                });

        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory gameCategory = builder.getOrCreateCategory(Text.translatable("huntervsmicemod.config.category.game"));

        gameCategory.addEntry(entryBuilder.startIntField(Text.translatable("huntervsmicemod.config.swap_interval"),  config.swapInterval  / 20)
                .setMin(3).setMax(3600)
                .setSaveConsumer(val -> config.swapInterval  = val * 20)
                .setDefaultValue(600)
                .setTooltip(Text.translatable("huntervsmicemod.config.swap_interval.tooltip"))
                .build());

        gameCategory.addEntry(entryBuilder.startIntField(Text.translatable("huntervsmicemod.config.broadcast_interval"),  config.broadcastInterval  / 20)
                .setMin(1).setMax(600)
                .setSaveConsumer(val -> config.broadcastInterval  = val * 20)
                .setDefaultValue(60)
                .setTooltip(Text.translatable("huntervsmicemod.config.broadcast_interval.tooltip"))
                .build());

        gameCategory.addEntry(entryBuilder.startIntField(Text.translatable("huntervsmicemod.config.prepare_time"),  config.prepareTime  / 20)
                .setMin(1).setMax(600)
                .setSaveConsumer(val -> config.prepareTime  = val * 20)
                .setDefaultValue(5)
                .setTooltip(Text.translatable("huntervsmicemod.config.prepare_time.tooltip"))
                .build());

        gameCategory.addEntry(entryBuilder.startIntField(Text.translatable("huntervsmicemod.config.reallocate_delay"),  config.reallocateDelay  / 20)
                .setMin(1).setMax(600)
                .setSaveConsumer(val -> config.reallocateDelay  = val * 20)
                .setDefaultValue(10)
                .setTooltip(Text.translatable("huntervsmicemod.config.reallocate_delay.tooltip"))
                .build());

        gameCategory.addEntry(entryBuilder.startIntField(Text.translatable("huntervsmicemod.config.respawn_radius"),  config.respawnRadius)
                .setMin(10).setMax(500)
                .setDefaultValue(100)
                .setSaveConsumer(val -> config.respawnRadius  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.respawn_radius.tooltip"))
                .build());

        ConfigCategory dangerCategory = builder.getOrCreateCategory(Text.translatable("huntervsmicemod.config.category.danger"));

        dangerCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("huntervsmicemod.config.enable_danger_alerts"),  config.enableDangerAlerts)
                .setDefaultValue(true)
                .setSaveConsumer(val -> config.enableDangerAlerts  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.enable_danger_alerts.tooltip"))
                .build());

        dangerCategory.addEntry(entryBuilder.startIntField(Text.translatable("huntervsmicemod.config.danger_distance"),  config.dangerDistance)
                .setMin(1).setMax(100)
                .setDefaultValue(15)
                .setSaveConsumer(val -> config.dangerDistance  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.danger_distance.tooltip"))
                .build());

        ConfigCategory hudCategory = builder.getOrCreateCategory(Text.translatable("huntervsmicemod.config.category.hud"));

        hudCategory.addEntry(entryBuilder.startIntSlider(Text.translatable("huntervsmicemod.config.hud_x"),  config.hud.x,  0, 1000)
                .setDefaultValue(5)
                .setSaveConsumer(val -> config.hud.x  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.hud_x.tooltip"))
                .build());

        hudCategory.addEntry(entryBuilder.startIntSlider(Text.translatable("huntervsmicemod.config.hud_y"),  config.hud.y,  0, 1000)
                .setDefaultValue(15)
                .setSaveConsumer(val -> config.hud.y  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.hud_y.tooltip"))
                .build());

        hudCategory.addEntry(entryBuilder.startIntSlider(Text.translatable("huntervsmicemod.config.hud_width"),  config.hud.width,  50, 300)
                .setDefaultValue(140)
                .setSaveConsumer(val -> config.hud.width  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.hud_width.tooltip"))
                .build());

        hudCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("huntervsmicemod.config.show_background"),  config.hud.showBackground)
                .setDefaultValue(true)
                .setSaveConsumer(val -> config.hud.showBackground  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.show_background.tooltip"))
                .build());

        hudCategory.addEntry(entryBuilder.startBooleanToggle(Text.translatable("huntervsmicemod.config.show_countdown"),  config.hud.showCountdown)
                .setDefaultValue(true)
                .setSaveConsumer(val -> config.hud.showCountdown  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.show_countdown.tooltip"))
                .build());

        hudCategory.addEntry(entryBuilder.startIntField(Text.translatable("huntervsmicemod.config.countdown_x_offset"),  config.hud.countdownXOffset)
                .setMin(-50).setMax(50)
                .setDefaultValue(0)
                .setSaveConsumer(val -> config.hud.countdownXOffset  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.countdown_x_offset.tooltip"))
                .build());

        hudCategory.addEntry(entryBuilder.startIntField(Text.translatable("huntervsmicemod.config.countdown_y_offset"),  config.hud.countdownYOffset)
                .setMin(-50).setMax(50)
                .setDefaultValue(0)
                .setSaveConsumer(val -> config.hud.countdownYOffset  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.countdown_y_offset.tooltip"))
                .build());

        ConfigCategory colorsCategory = builder.getOrCreateCategory(Text.translatable("huntervsmicemod.config.category.colors"));

        colorsCategory.addEntry(entryBuilder.startColorField(Text.translatable("huntervsmicemod.config.background_color"),  config.hud.backgroundColor)
                .setDefaultValue(0xCC222222)
                .setSaveConsumer(val -> config.hud.backgroundColor  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.background_color.tooltip"))
                .build());

        colorsCategory.addEntry(entryBuilder.startColorField(Text.translatable("huntervsmicemod.config.border_color"),  config.hud.borderColor)
                .setDefaultValue(0xFF444444)
                .setSaveConsumer(val -> config.hud.borderColor  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.border_color.tooltip"))
                .build());

        colorsCategory.addEntry(entryBuilder.startColorField(Text.translatable("huntervsmicemod.config.header_color"),  config.hud.headerColor)
                .setDefaultValue(0xFFF5A623)
                .setSaveConsumer(val -> config.hud.headerColor  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.header_color.tooltip"))
                .build());

        colorsCategory.addEntry(entryBuilder.startColorField(Text.translatable("huntervsmicemod.config.text_color"),  config.hud.textColor)
                .setDefaultValue(0xFFDDDDDD)
                .setSaveConsumer(val -> config.hud.textColor  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.text_color.tooltip"))
                .build());

        colorsCategory.addEntry(entryBuilder.startColorField(Text.translatable("huntervsmicemod.config.highlight_color"),  config.hud.highlightColor)
                .setDefaultValue(0xFF00FF00)
                .setSaveConsumer(val -> config.hud.highlightColor  = val)
                .setTooltip(Text.translatable("huntervsmicemod.config.highlight_color.tooltip"))
                .build());

        return builder.build();
    }
}