package com.huntervsmicemod.gui;

import com.huntervsmicemod.HunterVsMiceMod;
import com.huntervsmicemod.ModConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.text2speech.Narrator.LOGGER;

public class CoordinateHUD {
    private static int getBackgroundColor() {
        return HunterVsMiceMod.getConfig().hud.backgroundColor;
    }

    private static int getBorderColor() {
        return HunterVsMiceMod.getConfig().hud.borderColor;
    }

    private static int getHeaderColor() {
        return HunterVsMiceMod.getConfig().hud.headerColor;
    }

    private static int getTextColor() {
        return HunterVsMiceMod.getConfig().hud.textColor;
    }

    private static int getHighlightColor() {
        return HunterVsMiceMod.getConfig().hud.highlightColor;
    }

    private static int currentCountdown = -1;

    private static List<MutableText> cachedMousePositions = new ArrayList<>();

    public static void updateCountdown(int seconds) {
        currentCountdown = seconds;
    }

    private static void renderCountdown(DrawContext context, TextRenderer textRenderer, int x, int y, int width) {
        ModConfig.HudConfig hudConfig = HunterVsMiceMod.getConfig().hud;

        if (!hudConfig.showCountdown  || currentCountdown <= 0) return;

        int color = 0xFFFFFFFF;
        if (currentCountdown <= 5) {
            long time = System.currentTimeMillis();
            color = (time / 500) % 2 == 0 ? 0xFFFF0000 : 0xFFFF6060;
        }

        int offsetX = hudConfig.countdownXOffset;
        int offsetY = hudConfig.countdownYOffset;

        String countdownText = String.valueOf(currentCountdown);
        int textX = x + width - textRenderer.getWidth(countdownText)  - 5 + offsetX;
        int textY = y + 3 + offsetY;

        context.drawText(textRenderer,  countdownText, textX, textY, color, false);
    }

    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(
                new Identifier("huntervsmicemod", "mouse_hud_data"),
                (client, handler, buf, responseSender) -> {
                    List<MutableText> positions = new ArrayList<>();
                    int count = buf.readInt();
                    for (int i = 0; i < count; i++) {
                        positions.add(buf.readText().copy());
                    }
                    client.execute(()  -> {
                        cachedMousePositions = positions;

                        LOGGER.info(Text.translatable("huntervsmicemod.hud.mouse_positions_received",
                                positions.size(),  positions).getString());
                    });
                }
        );
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player  == null || client.world  == null) return;

        ModConfig.HudConfig hudConfig = HunterVsMiceMod.getConfig().hud;
        TextRenderer textRenderer = client.textRenderer;

        int sidebarX = hudConfig.x;
        int sidebarY = hudConfig.y;
        int sidebarWidth = hudConfig.width;
        int contentHeight = calculateContentHeight();
        int sidebarHeight = contentHeight + 15;

        if (hudConfig.showBackground)  {
            drawCompactPanel(context, sidebarX, sidebarY, sidebarWidth, sidebarHeight);
        }

        renderCountdown(context, textRenderer, sidebarX, sidebarY, sidebarWidth);

        renderCompactPlayerCoordinates(context, textRenderer, sidebarX + 3, sidebarY + 3, client.player.getBlockPos());
        renderCompactMousePositions(context, textRenderer, sidebarX + 3, sidebarY + 20, client.player.getBlockPos());
    }

    private static void drawCompactPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x,  y, x + width, y + height, getBackgroundColor());
        context.fill(x,  y, x + 1, y + height, getBorderColor());
        context.fill(x  + width - 1, y, x + width, y + height, getBorderColor());
    }

    private static int calculateContentHeight() {
        if (cachedMousePositions.isEmpty())  return 30;
        return 20 + cachedMousePositions.size()  * 28;
    }

    private static void renderCompactPlayerCoordinates(DrawContext context, TextRenderer textRenderer,
                                                       int x, int y, BlockPos pos) {
        context.drawText(textRenderer,
                Text.translatable("huntervsmicemod.hud.coordinates_header").formatted(Formatting.BOLD),
                x, y, getHeaderColor(), false);

        context.drawText(textRenderer,
                Text.translatable("huntervsmicemod.hud.coordinates_value",
                                pos.getX(),  pos.getY(),  pos.getZ())
                        .formatted(Formatting.GRAY),
                x + 30, y, getTextColor(), false);
    }

    private static void renderCompactMousePositions(DrawContext context,
                                                    TextRenderer textRenderer,
                                                    int x, int y,
                                                    BlockPos playerPos) {
        context.drawText(textRenderer,
                Text.translatable("huntervsmicemod.hud.mice_header").formatted(Formatting.BOLD),
                x, y, getHeaderColor(), false);

        if (cachedMousePositions.isEmpty())  {
            context.drawText(textRenderer,
                    Text.translatable("huntervsmicemod.hud.no_data").formatted(Formatting.GRAY),
                    x + 30, y, getTextColor(), false);
            return;
        }

        double closestDistance = Double.MAX_VALUE;
        MutableText closestPosition = null;
        int yOffset = 15;

        for (MutableText position : cachedMousePositions) {
            String[] parts = parsePositionText(position);
            if (parts == null) continue;

            try {
                int[] coords = parseCoordinates(parts[1]);
                int deltaX = coords[0] - playerPos.getX();
                int deltaY = coords[1] - playerPos.getY();
                int deltaZ = coords[2] - playerPos.getZ();
                double distance = calculateDistance(playerPos, coords);

                MutableText infoText = Text.translatable("huntervsmicemod.hud.mouse_position",
                        Text.literal(parts[0]).formatted(Formatting.GREEN),
                        deltaX, deltaY, deltaZ,
                        Text.literal(String.format("%.1f",  distance))
                                .formatted(distance < closestDistance ? Formatting.GREEN : Formatting.GRAY)
                );

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPosition = infoText.copy();
                }

                context.drawText(textRenderer,  infoText, x, y + yOffset, getTextColor(), false);
                yOffset += 14;
            } catch (NumberFormatException ignored) {

            }
        }

        if (closestPosition != null) {
            context.drawText(textRenderer,
                    closestPosition.append(Text.translatable("huntervsmicemod.hud.closest_indicator")),
                    x, y + yOffset, getHighlightColor(), false);
        }
    }

    private static int[] parseCoordinates(String coordStr) {
        try {
            String clean = coordStr.replaceAll("[^\\d,-]",  "");
            String[] parts = clean.split(",");

            if (parts.length  == 3) {
                return new int[]{
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                };
            }
        } catch (Exception e) {
            LOGGER.error(Text.translatable("huntervsmicemod.hud.coordinate_parse_error",
                    coordStr, e).getString());
        }

        LOGGER.warn(Text.translatable("huntervsmicemod.hud.using_default_coordinates",  coordStr).getString());
        return new int[]{0, 0, 0};
    }

    private static double calculateDistance(BlockPos playerPos, int[] mouseCoords) {
        double dx = playerPos.getX()  - mouseCoords[0];
        double dy = playerPos.getY()  - mouseCoords[1];
        double dz = playerPos.getZ()  - mouseCoords[2];
        return Math.sqrt(dx  * dx + dy * dy + dz * dz);
    }

    public static void updateMousePositions(List<MutableText> positions) {
        cachedMousePositions = positions.stream()
                .map(text -> {

                    String str = text.getString();
                    if (str.contains(":  [")) {
                        return text;
                    }

                    return Text.literal(str.replace(":  ", ": [") + "]");
                })
                .collect(Collectors.toList());
    }

    private static String[] parsePositionText(MutableText text) {
        String str = text.getString().trim();
        LOGGER.debug(Text.translatable("huntervsmicemod.hud.parsing_position_text",  str).getString());

        java.util.regex.Pattern  pattern = java.util.regex.Pattern.compile(
                "(.+?):\\s*\\[\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*]"
        );

        java.util.regex.Matcher  matcher = pattern.matcher(str);
        if (matcher.matches())  {
            String playerName = matcher.group(1).trim();
            String coordPart = String.format("[%s,%s,%s]",
                    matcher.group(2),  matcher.group(3),  matcher.group(4));
            LOGGER.debug(Text.translatable("huntervsmicemod.hud.parse_success",
                    playerName, coordPart).getString());
            return new String[]{playerName, coordPart};
        }

        LOGGER.warn(Text.translatable("huntervsmicemod.hud.parse_failed",  str).getString());
        return null;
    }

}