package com.huntervsmicemod;

import com.huntervsmicemod.gui.HUDRenderer;
import com.huntervsmicemod.gui.ModConfigScreen;
import com.huntervsmicemod.gui.TeamUIScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import com.huntervsmicemod.gui.CoordinateHUD;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.huntervsmicemod.HunterVsMiceMod.MOUSE_HUD_DATA;
import static com.mojang.text2speech.Narrator.LOGGER;

public class HunterVsMiceModClient implements ClientModInitializer {
    private static KeyBinding openTeamUIKey;
    private static KeyBinding configKey;
    private static final ConcurrentMap<String, PlayerData.Role> cachedRoles = new ConcurrentHashMap<>();
    public static final Identifier REQUEST_TEAMS = new Identifier("huntervsmicemod", "request_teams");
    public static final Identifier SYNC_TEAMS = new Identifier("huntervsmicemod", "sync_teams");
    private long lastRequestTime = 0;

    @Override
    public void onInitializeClient() {
        CoordinateHUD.registerClientReceiver();
        HudRenderCallback.EVENT.register(new  HUDRenderer());

        openTeamUIKey = KeyBindingHelper.registerKeyBinding(new  KeyBinding(
                "UI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.huntervsmicemod.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client  -> {
            while (openTeamUIKey.wasPressed())  {
                if (client.player  != null) {
                    requestTeamDataFromServer(client.player);
                }
            }
        });

        configKey = KeyBindingHelper.registerKeyBinding(new  KeyBinding(
                "Config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.huntervsmicemod.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client  -> {
            while (configKey.wasPressed())  {
                if (client.player  != null && client.player.hasPermissionLevel(2))  {
                    client.setScreen(ModConfigScreen.getScreen(null));
                }
            }
        });
        registerClientNetworkHandlers();
    }
    public static Map<String, PlayerData.Role> getCachedRoles() {
        return new HashMap<>(cachedRoles);
    }
    private void registerClientNetworkHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_TEAMS,
                (client, handler, buf, responseSender) -> {
                    try {
                        Map<String, PlayerData.Role> newRoles = new HashMap<>();
                        int count = buf.readInt();
                        for (int i = 0; i < count; i++) {
                            String name = buf.readString();
                            PlayerData.Role role = PlayerData.Role.valueOf(buf.readString());
                            newRoles.put(name,  role);
                        }

                        cachedRoles.clear();
                        cachedRoles.putAll(newRoles);

                        client.execute(()  -> {
                            if (client.currentScreen  == null) {
                                client.setScreen(new  TeamUIScreen(getCachedRoles()));
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.error(Text.translatable("huntervsmicemod.error.team_data_parse_failed").getString(),  e);
                        if (client.player  != null) {
                            client.player.sendMessage(
                                    Text.translatable("huntervsmicemod.message.team_data_receive_failed")
                                            .formatted(Formatting.RED),
                                    false
                            );
                        }
                    }
                });

        ClientPlayNetworking.registerGlobalReceiver(MOUSE_HUD_DATA,
                (client, handler, buf, responseSender) -> {
                    try {
                        List<MutableText> positions = new ArrayList<>();
                        int count = buf.readInt();
                        for (int i = 0; i < count; i++) {
                            positions.add(buf.readText().copy());
                        }
                        CoordinateHUD.updateMousePositions(positions);
                    } catch (Exception e) {
                        LOGGER.error(Text.translatable("huntervsmicemod.error.mouse_hud_data_failed").getString(),  e);
                    }
                });
    }
    private void requestTeamDataFromServer(PlayerEntity player) {
        long now = System.currentTimeMillis();
        if (now - lastRequestTime < 1000) {
            player.sendMessage(
                    Text.translatable("huntervsmicemod.message.request_too_frequent")
                            .formatted(Formatting.YELLOW),
                    false
            );
            return;
        }
        lastRequestTime = now;

        try {
            ClientPlayNetworking.send(REQUEST_TEAMS,  PacketByteBufs.empty());
            player.sendMessage(
                    Text.translatable("huntervsmicemod.message.requesting_team_data")
                            .formatted(Formatting.GRAY),
                    false
            );
        } catch (Exception e) {
            LOGGER.error(Text.translatable("huntervsmicemod.error.team_data_request_failed").getString(),  e);
            player.sendMessage(
                    Text.translatable("huntervsmicemod.message.team_data_request_failed")
                            .formatted(Formatting.RED),
                    false
            );
        }
    }
}