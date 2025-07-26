package com.huntervsmicemod.gui;

import com.huntervsmicemod.HunterVsMiceMod;
import com.huntervsmicemod.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;

import static com.huntervsmicemod.HunterVsMiceMod.*;

public class TeamUIScreen extends Screen {
    private final MinecraftClient client;
    private final Map<String, PlayerData.Role> playersRoles;
    private static final int HEADER_COLOR = 0xFFF5A623;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int BORDER_COLOR = 0xFF555555;

    public TeamUIScreen(Map<String, PlayerData.Role> playersRoles) {
        super(Text.translatable("huntervsmicemod.ui.title").formatted(Formatting.BOLD));
        this.client  = MinecraftClient.getInstance();
        this.playersRoles  = playersRoles;
    }

    private boolean isOp(PlayerEntity player) {
        return player.hasPermissionLevel(2);
    }

    private PlayerData.Role getOppositeRole(String playerName) {
        PlayerData.Role currentRole = playersRoles.get(playerName);
        return currentRole == PlayerData.Role.HUNTER ? PlayerData.Role.MOUSE : PlayerData.Role.HUNTER;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(width  - 40, 400);
        int panelHeight = Math.min(height  - 60, 300);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("huntervsmicemod.ui.close").formatted(Formatting.WHITE),
                        button -> close())
                .dimensions(panelX + panelWidth - 60, panelY + 10, 50, 20)
                .build());

        int listStartY = panelY + 50;
        int hunterColumnX = panelX + 20;
        int mouseColumnX = panelX + panelWidth / 2 + 10;
        int currentHunterY = listStartY;
        int currentMouseY = listStartY;

        for (Map.Entry<String, PlayerData.Role> entry : playersRoles.entrySet())  {
            String playerName = entry.getKey();
            PlayerData.Role role = entry.getValue();

            if (role == PlayerData.Role.HUNTER) {
                addPlayerRow(playerName, hunterColumnX, currentHunterY);
                currentHunterY += 25;
            } else if (role == PlayerData.Role.MOUSE) {
                addPlayerRow(playerName, mouseColumnX, currentMouseY);
                currentMouseY += 25;
            }
        }

        MutableText buttonText = isGamePreparing() ?
                Text.translatable("huntervsmicemod.ui.confirm_ready")  :
                Text.translatable("huntervsmicemod.ui.start_game");

        ButtonWidget confirmButton = ButtonWidget.builder(
                        buttonText.formatted(isGamePreparing()  ? Formatting.GREEN : Formatting.DARK_GRAY),
                        button -> {
                            if (client.player  != null) {
                                if (isGamePreparing()) {
                                    sendConfirmation();
                                    close();
                                } else {
                                    startGame();
                                }
                            }
                        })
                .dimensions(width / 2 - 60, panelY + panelHeight - 35, 120, 25)
                .build();

        confirmButton.active  = isGamePreparing();

        addDrawableChild(confirmButton);
    }

    private boolean isGamePreparing() {
        return HunterVsMiceMod.getInstance().isInPreparePhase();
    }

    private void sendConfirmation() {
        if (client.player  == null) return;

        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(client.player.getUuid());
            ClientPlayNetworking.send(CONFIRM_START,  buf);
            close();
        } catch (Exception e) {
            if (client.player  != null) {
                client.player.sendMessage(
                        Text.translatable("huntervsmicemod.message.confirmation_failed",  e.getMessage())
                                .formatted(Formatting.RED),
                        false
                );
            }
            LOGGER.error(Text.translatable("huntervsmicemod.error.confirmation_packet_failed").getString(),  e);
        }
    }

    private void startGame() {
        if (client.player  == null || !isOp(client.player))  {
            if (client.player  != null) {
                client.player.sendMessage(
                        Text.translatable("huntervsmicemod.message.op_required")
                                .formatted(Formatting.RED),
                        false);
            }
            return;
        }

        try {
            ClientPlayNetworking.send(REQUEST_START,  PacketByteBufs.empty());
        } catch (Exception e) {
            client.player.sendMessage(
                    Text.translatable("huntervsmicemod.message.start_game_failed")
                            .formatted(Formatting.RED),
                    false);
        }
    }

    private void addPlayerRow(String playerName, int x, int y) {
        ButtonWidget changeTeamBtn = ButtonWidget.builder(
                        Text.translatable("huntervsmicemod.ui.change_team").formatted(Formatting.YELLOW),
                        button -> onChangeTeam(playerName))
                .dimensions(x + 120, y - 3, 60, 18)
                .build();

        if (client.player  != null) {
            changeTeamBtn.active  = isOp(client.player);
        }
        addDrawableChild(changeTeamBtn);
    }

    private void onChangeTeam(String playerName) {
        if (client.player  == null || !isOp(client.player))  {
            return;
        }

        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(playerName);
            buf.writeString(getOppositeRole(playerName).toString());
            ClientPlayNetworking.send(TEAM_CHANGE,  buf);

            PlayerData.Role newRole = getOppositeRole(playerName);
            playersRoles.put(playerName,  newRole);
            close();
            client.setScreen(new  TeamUIScreen(playersRoles));
        } catch (Exception e) {
            client.player.sendMessage(
                    Text.translatable("huntervsmicemod.message.change_team_failed")
                            .formatted(Formatting.RED),
                    false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int panelWidth = Math.min(width  - 40, 400);
        int panelHeight = Math.min(height  - 60, 300);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        context.fill(panelX,  panelY, panelX + panelWidth, panelY + panelHeight, BACKGROUND_COLOR);
        context.drawBorder(panelX,  panelY, panelWidth, panelHeight, BORDER_COLOR);

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("huntervsmicemod.ui.title").formatted(Formatting.BOLD),
                width / 2, panelY + 15, HEADER_COLOR
        );

        int titleY = panelY + 35;
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("huntervsmicemod.team.hunters").formatted(Formatting.RED,  Formatting.BOLD),
                panelX + 20, titleY, 0xFFFFFFFF);

        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("huntervsmicemod.team.mice").formatted(Formatting.GREEN,  Formatting.BOLD),
                panelX + panelWidth / 2 + 10, titleY, 0xFFFFFFFF);

        int listStartY = panelY + 50;
        int hunterColumnX = panelX + 20;
        int mouseColumnX = panelX + panelWidth / 2 + 10;
        int currentHunterY = listStartY;
        int currentMouseY = listStartY;

        for (Map.Entry<String, PlayerData.Role> entry : playersRoles.entrySet())  {
            String playerName = entry.getKey();
            PlayerData.Role role = entry.getValue();
            Text playerText = Text.literal("•  " + playerName)
                    .formatted(role == PlayerData.Role.HUNTER ? Formatting.RED : Formatting.GREEN);

            if (role == PlayerData.Role.HUNTER) {
                context.drawTextWithShadow(textRenderer,  playerText,
                        hunterColumnX, currentHunterY, 0xFFFFFFFF);
                currentHunterY += 25;
            } else {
                context.drawTextWithShadow(textRenderer,  playerText,
                        mouseColumnX, currentMouseY, 0xFFFFFFFF);
                currentMouseY += 25;
            }
        }

        super.render(context,  mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(null);
    }
}