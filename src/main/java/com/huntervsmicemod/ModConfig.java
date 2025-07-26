package com.huntervsmicemod;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "huntervsmicemod")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.BoundedDiscrete(min = 60, max = 72000)
    public int swapInterval = 10 * 60 * 20;

    @ConfigEntry.Gui.Tooltip
    public int broadcastInterval = 60 * 20;

    @ConfigEntry.Gui.Tooltip
    public int respawnRadius = 100;

    @ConfigEntry.Gui.Tooltip
    public int prepareTime = 20 * 5;

    @ConfigEntry.Gui.Tooltip
    public int reallocateDelay = 10 * 20;

    @Override
    public void validatePostLoad() {
        swapInterval = Math.max(60,  swapInterval);
        broadcastInterval = Math.max(20,  broadcastInterval);
    }

    @ConfigEntry.Gui.Tooltip(count = 2)
    public int dangerDistance = 15;

    @ConfigEntry.Gui.Tooltip
    public boolean enableDangerAlerts = true;

    @ConfigEntry.Gui.CollapsibleObject
    public HudConfig hud = new HudConfig();

    public static class HudConfig {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
        public int x = 5;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
        public int y = 15;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 50, max = 300)
        public int width = 140;

        @ConfigEntry.Gui.Tooltip
        public boolean showBackground = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.ColorPicker
        public int backgroundColor = 0xCC222222;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.ColorPicker
        public int borderColor = 0xFF444444;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.ColorPicker
        public int headerColor = 0xFFF5A623;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.ColorPicker
        public int textColor = 0xFFDDDDDD;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.ColorPicker
        public int highlightColor = 0xFF00FF00;

        @ConfigEntry.Gui.Tooltip
        public boolean showCountdown = true;

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = -50, max = 50)
        public int countdownXOffset = -10;

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = -50, max = 50)
        public int countdownYOffset = 0;
    }
}