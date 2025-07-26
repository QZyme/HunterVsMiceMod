package com.huntervsmicemod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.huntervsmicemod.command.GameStopCommand;
import com.huntervsmicemod.command.TeamChangeCommand;
import com.huntervsmicemod.gui.CoordinateHUD;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class HunterVsMiceMod implements ModInitializer {
    private static ModConfig config;
    private int swapTimer = 0;
    private int broadcastTimer = 0;
    private int prepareTimer = -1;
    private int reallocateTimer = -1;
    private final Set<UUID> confirmedPlayers = new HashSet<>();
    private final AtomicBoolean isReallocating = new AtomicBoolean(false);
    public boolean isGameStarted = false;
    private static HunterVsMiceMod instance;
    private boolean inPreparePhase = false;
    public static final org.slf4j.Logger  LOGGER = org.slf4j.LoggerFactory.getLogger("HunterVsMiceMod");
    public static final Identifier MOUSE_POSITIONS = new Identifier("huntervsmicemod", "mouse_positions");
    public static final Identifier MOUSE_HUD_DATA = new Identifier("huntervsmicemod", "mouse_hud_data");
    public static final Identifier CONFIRM_START = new Identifier("huntervsmicemod", "confirm_start");
    public static final Identifier TEAM_CHANGE = new Identifier("huntervsmicemod", "team_change");
    public static final Identifier REQUEST_START = new Identifier("huntervsmicemod", "request_start");
    public static final Identifier REQUEST_TEAMS = new Identifier("huntervsmicemod", "request_teams");
    public static final Identifier SYNC_TEAMS = new Identifier("huntervsmicemod", "sync_teams");

    @Override
    public void onInitialize() {
        LOGGER.info(Text.translatable("huntervsmicemod.message.mod_initializing").getString());
        AutoConfig.register(ModConfig.class,  GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        instance = this;
        ServerWorldEvents.LOAD.register((server,  world) -> {
            if (world.getRegistryKey()  == World.OVERWORLD) {
                initTeams(world.getScoreboard());
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(server  -> {
            swapTimer++;
            broadcastTimer++;

            if (swapTimer >= getConfig().swapInterval) {
                swapTeams(server);
                swapTimer = 0;
            }

            if (broadcastTimer >= getConfig().broadcastInterval) {
                broadcastMouseLocations(server);
                broadcastTimer = 0;
            }

            if (prepareTimer >= 0) {
                prepareTimer--;
                if (prepareTimer == 0) {
                    startGame(server);
                } else if (prepareTimer % 20 == 0) {
                    server.getPlayerManager().broadcast(
                            Text.translatable("huntervsmicemod.message.game_starting_in",  prepareTimer / 20),
                            false
                    );
                }
            }

            if (reallocateTimer >= 0) {
                reallocateTimer--;
                if (reallocateTimer == 0) {
                    swapTeams(server);
                    reallocateTimer = -1;
                    isReallocating.set(false);
                }
            }

            if (isGameStarted && checkAllMiceDead(server)) {
                announceHuntersVictory(server);
            }
            if (isGameStarted && getConfig().enableDangerAlerts) {
                checkDangerDistances(server);
            }
            if (prepareTimer > 0) {
                CoordinateHUD.updateCountdown(prepareTimer  / 20);
            } else if (reallocateTimer > 0) {
                CoordinateHUD.updateCountdown(reallocateTimer  / 20);
            } else if (isGameStarted && broadcastTimer > 0) {
                int secondsUntilBroadcast = (getConfig().broadcastInterval - broadcastTimer) / 20;
                CoordinateHUD.updateCountdown(secondsUntilBroadcast);
            } else {
                CoordinateHUD.updateCountdown(-1);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler,  sender, server) ->
                handlePlayerJoin(handler.player));

        CommandRegistrationCallback.EVENT.register((dispatcher,  registry, env) -> {
            GameStopCommand.register(dispatcher);
            TeamChangeCommand.register(dispatcher);
        });

        registerNetworkHandlers();

        CommandRegistrationCallback.EVENT.register((dispatcher,  registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("hvmconfig")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        context.getSource().sendFeedback(
                                () -> Text.translatable("huntervsmicemod.message.config_key_hint")
                                        .formatted(Formatting.GOLD),
                                false
                        );
                        return 1;
                    }));

            dispatcher.register(CommandManager.literal("hvmreload")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {
                        reloadConfig();
                        context.getSource().sendFeedback(
                                () -> Text.translatable("huntervsmicemod.message.config_reloaded")
                                        .formatted(Formatting.GREEN),
                                true
                        );
                        return 1;
                    }));
        });
        AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((manager,  newData) -> {
            LOGGER.info(Text.translatable("huntervsmicemod.message.hud_config_updated").getString());
            return ActionResult.SUCCESS;
        });
    }
    public static ModConfig getConfig() {
        return config;
    }
    public static void reloadConfig() {
        try {
            AutoConfig.getConfigHolder(ModConfig.class).load();
            config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
            LOGGER.info(Text.translatable("huntervsmicemod.message.config_reload_success").getString());
        } catch (Exception e) {
            LOGGER.error(Text.translatable("huntervsmicemod.error.config_reload_failed").getString(),  e);
            config = new ModConfig();
        }
    }
    private final Map<UUID, Long> lastAlertTime = new ConcurrentHashMap<>();
    private void checkDangerDistances(MinecraftServer server) {
        List<ServerPlayerEntity> mice = new ArrayList<>();
        List<ServerPlayerEntity> hunters = new ArrayList<>();
        int dangerDistance = getConfig().dangerDistance;

        server.getPlayerManager().getPlayerList().forEach(player  -> {
            PlayerData data = PlayerData.get(player);
            if (data == null) return;

            if (data.getRole()  == PlayerData.Role.MOUSE && !data.isMouseDead())  {
                mice.add(player);
            } else if (data.getRole()  == PlayerData.Role.HUNTER) {
                hunters.add(player);
            }
        });
        long now = System.currentTimeMillis();

        for (ServerPlayerEntity mouse : mice) {
            BlockPos mousePos = mouse.getBlockPos();

            for (ServerPlayerEntity hunter : hunters) {
                BlockPos hunterPos = hunter.getBlockPos();
                double distance = Math.sqrt(mousePos.getSquaredDistance(hunterPos));

                if (distance <= dangerDistance) {
                    if (now - lastAlertTime.getOrDefault(mouse.getUuid(),  0L) > 5000) {
                        Text alert = Text.translatable(
                                "huntervsmicemod.message.danger_alert",
                                mouse == hunter ?
                                        Text.translatable("huntervsmicemod.message.yourself")  :
                                        distance < 5 ?
                                                Text.translatable("huntervsmicemod.message.nearby")  :
                                                Text.translatable("huntervsmicemod.role.hunter"),
                                String.format("%.1f",  distance)
                        ).formatted(Formatting.RED, Formatting.YELLOW);
                        mouse.sendMessage(alert,  false);
                        lastAlertTime.put(mouse.getUuid(),  now);
                    }

                    if (distance < 10 && now - lastAlertTime.getOrDefault(hunter.getUuid(),  0L) > 5000) {
                        hunter.sendMessage(
                                Text.translatable("huntervsmicemod.message.mouse_spotted",
                                                String.format("%.1f",  distance))
                                        .formatted(Formatting.RED, Formatting.YELLOW),
                                false
                        );
                        lastAlertTime.put(hunter.getUuid(),  now);
                    }
                    break;
                }
            }
        }
    }
    private void registerNetworkHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(MOUSE_POSITIONS,
                (server, player, handler, buf, responseSender) -> {
                    try {
                        List<MutableText> positions = new ArrayList<>();
                        int count = buf.readInt();
                        for (int i = 0; i < count; i++) {
                            positions.add(buf.readText().copy());
                        }
                        updateHUDData(positions);
                    } catch (Exception e) {
                        LOGGER.error(Text.translatable("huntervsmicemod.error.mouse_position_process_failed").getString(),  e);
                    }
                });

        ServerPlayNetworking.registerGlobalReceiver(TEAM_CHANGE,  this::handleTeamChangePacket);

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_TEAMS,
                (server, player, handler, buf, responseSender) -> {
                    try {
                        Map<String, PlayerData.Role> roles = getAllPlayersRoles(server);
                        PacketByteBuf responseBuf = PacketByteBufs.create();
                        responseBuf.writeInt(roles.size());
                        roles.forEach((name,  role) -> {
                            responseBuf.writeString(name);
                            responseBuf.writeString(role.name());
                        });
                        ServerPlayNetworking.send(player,  SYNC_TEAMS, responseBuf);
                    } catch (Exception e) {
                        LOGGER.error(Text.translatable("huntervsmicemod.error.team_request_failed").getString(),  e);
                        player.sendMessage(
                                Text.translatable("huntervsmicemod.message.request_process_failed")
                                        .formatted(Formatting.RED),
                                false
                        );
                    }
                });

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_START,  this::handleStartRequest);

        ServerPlayNetworking.registerGlobalReceiver(CONFIRM_START,
                (server, player, handler, buf, responseSender) -> {
                    UUID playerId = buf.readUuid();
                    server.execute(()  -> confirmStart(server, playerId));
                });
    }

    private void handleTeamChangePacket(MinecraftServer server,
                                        ServerPlayerEntity sender,
                                        ServerPlayNetworkHandler handler,
                                        PacketByteBuf buf,
                                        PacketSender responseSender) {

        try {
            String targetName = buf.readString();
            String roleStr = buf.readString();

            server.execute(()  -> {
                if (!sender.hasPermissionLevel(2))  {
                    sender.sendMessage(
                            Text.translatable("huntervsmicemod.message.no_permission")
                                    .formatted(Formatting.RED),
                            false
                    );
                    return;
                }

                ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetName);
                if (targetPlayer == null) {
                    sender.sendMessage(
                            Text.translatable("huntervsmicemod.message.player_offline",  targetName)
                                    .formatted(Formatting.RED),
                            false
                    );
                    return;
                }

                PlayerData.Role newRole;
                try {
                    newRole = PlayerData.Role.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(
                            Text.translatable("huntervsmicemod.message.invalid_role",  roleStr)
                                    .formatted(Formatting.RED),
                            false
                    );
                    return;
                }

                PlayerData data = PlayerData.get(targetPlayer);
                if (data == null) {
                    data = new PlayerData();
                    PlayerData.set(targetPlayer,  data);
                }

                data.setRole(newRole);
                applyTeamSettings(targetPlayer, data);

                sender.sendMessage(
                        Text.translatable(
                                "huntervsmicemod.message.role_updated",
                                targetName,
                                newRole == PlayerData.Role.HUNTER ?
                                        Text.translatable("huntervsmicemod.role.hunter").formatted(Formatting.RED)  :
                                        Text.translatable("huntervsmicemod.role.mouse").formatted(Formatting.GREEN)
                        ),
                        false
                );

                syncTeamDataToAllClients(server);
            });
        } catch (Exception e) {
            LOGGER.error(Text.translatable("huntervsmicemod.error.team_change_packet_failed",  e.getMessage()).getString(),  e);

            if (sender != null) {
                sender.sendMessage(
                        Text.translatable("huntervsmicemod.message.internal_error")
                                .formatted(Formatting.RED),
                        false
                );
            }
        }
    }

    private void handleStartRequest(MinecraftServer server,
                                    ServerPlayerEntity sender,
                                    ServerPlayNetworkHandler handler,
                                    PacketByteBuf buf,
                                    PacketSender responseSender) {
        if (!sender.hasPermissionLevel(2))  {
            sender.sendMessage(Text.translatable("huntervsmicemod.message.need_op_to_start")
                    .formatted(Formatting.RED), false);
            return;
        }

        server.execute(()  -> {
            if (inPreparePhase) {
                prepareTimer = getConfig().prepareTime;
                server.getPlayerManager().broadcast(
                        Text.translatable("huntervsmicemod.message.game_starting_in_seconds",
                                        getConfig().prepareTime/20)
                                .formatted(Formatting.GOLD),
                        false
                );
            } else {
                startPreparePhase(server);
            }
        });
    }

    public static synchronized void syncTeamDataToAllClients(MinecraftServer server) {
        Map<String, PlayerData.Role> roles = getAllPlayersRoles(server);

        server.getPlayerManager().getPlayerList().forEach(p  -> {
            PacketByteBuf buf = PacketByteBufs.create();
            try {
                buf.writeInt(roles.size());
                roles.forEach((name,  role) -> {
                    buf.writeString(name);
                    buf.writeString(role.name());
                });
                ServerPlayNetworking.send(p,  SYNC_TEAMS, buf);
            } catch (Exception e) {
                LOGGER.error(Text.translatable("huntervsmicemod.error.failed_to_sync_teams").getString(),  e);
            }
        });
    }

    public static void updateHUDData(List<MutableText> mousePositions) {
        CoordinateHUD.updateMousePositions(mousePositions);
    }

    private void initTeams(Scoreboard scoreboard) {
        Team hunters = scoreboard.getTeam("hunters");
        if (hunters == null) {
            hunters = scoreboard.addTeam("hunters");
            hunters.setDisplayName(Text.translatable("huntervsmicemod.team.hunters").formatted(Formatting.RED));
            hunters.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.HIDE_FOR_OTHER_TEAMS);
            hunters.setColor(Formatting.RED);
        }

        Team mice = scoreboard.getTeam("mice");
        if (mice == null) {
            mice = scoreboard.addTeam("mice");
            mice.setDisplayName(Text.translatable("huntervsmicemod.team.mice").formatted(Formatting.GREEN));
            mice.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.HIDE_FOR_OTHER_TEAMS);
            mice.setColor(Formatting.GREEN);
        }
    }

    private void handlePlayerJoin(ServerPlayerEntity player) {
        PlayerData data = PlayerData.get(player);
        if (!data.isInitialized())  {
            data.setInitialized(true);
            data.randomizeRole();
            player.sendMessage(
                    Text.translatable("huntervsmicemod.message.assigned_role",
                            data.getRole()  == PlayerData.Role.HUNTER ?
                                    Text.translatable("huntervsmicemod.role.hunter").formatted(Formatting.RED)  :
                                    Text.translatable("huntervsmicemod.role.mouse").formatted(Formatting.GREEN)),
                    false
            );
        }

        if (isInPreparePhase()) {
            player.changeGameMode(GameMode.ADVENTURE);
        } else if (data.getRole()  == PlayerData.Role.MOUSE && data.isMouseDead())  {
            player.changeGameMode(GameMode.SPECTATOR);
        } else {
            player.changeGameMode(GameMode.SURVIVAL);
        }

        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        applyTeamSettings(player, data);

        if (inPreparePhase) {
            player.sendMessage(
                    Text.translatable("huntervsmicemod.message.prepare_instructions")
                            .formatted(Formatting.GOLD),
                    false
            );
        }
    }
    public void stopGame(MinecraftServer server) {
        isGameStarted = false;
        inPreparePhase = false;
        prepareTimer = -1;
        reallocateTimer = -1;
        broadcastTimer = 0;
        swapTimer = 0;
        isReallocating.set(false);
        confirmedPlayers.clear();

        server.getPlayerManager().getPlayerList().forEach(player  -> {
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                data.setMouseDead(false);
                data.randomizeRole();
                applyTeamSettings(player, data);
            }
            player.changeGameMode(GameMode.ADVENTURE);
            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);
        });

        server.getPlayerManager().broadcast(
                Text.translatable("huntervsmicemod.message.game_stopped_by_admin")
                        .formatted(Formatting.RED),
                false
        );

        startPreparePhase(server);
    }
    private void swapTeams(MinecraftServer server) {
        isGameStarted = false;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerData data = PlayerData.get(player);
            if (data == null || (data.getRole() == PlayerData.Role.MOUSE && data.isMouseDead())) continue;

            data.swapRole();

            if (isInPreparePhase()) {
                player.changeGameMode(GameMode.ADVENTURE);
            } else {
                player.changeGameMode(GameMode.SURVIVAL);
            }

            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);
            applyTeamSettings(player, data);
            player.sendMessage(
                    Text.translatable("huntervsmicemod.message.role_changed",
                            data.getRole() == PlayerData.Role.HUNTER ?
                                    Text.translatable("huntervsmicemod.role.hunter").formatted(Formatting.RED) :
                                    Text.translatable("huntervsmicemod.role.mouse").formatted(Formatting.GREEN)),
                    false
            );
        }
        server.getPlayerManager().broadcast(
                Text.translatable("huntervsmicemod.message.teams_swapped")
                        .formatted(Formatting.GOLD),
                false);
    }

    private void broadcastMouseLocations(MinecraftServer server) {
        List<MutableText> mousePositions = new ArrayList<>();
        server.getPlayerManager().getPlayerList().forEach(player  -> {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.getRole()  == PlayerData.Role.MOUSE && !data.isMouseDead())  {
                BlockPos pos = player.getBlockPos();
                mousePositions.add(Text.literal(player.getName().getString())
                        .append(": [")
                        .append(String.format("%d,%d,%d",  pos.getX(),  pos.getY(),  pos.getZ()))
                        .append("]"));
            }
        });

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(mousePositions.size());
        mousePositions.forEach(buf::writeText);

        server.getPlayerManager().getPlayerList().forEach(p  ->
                ServerPlayNetworking.send(p,  MOUSE_HUD_DATA, buf)
        );

        broadcastTimer = 0;
    }

    public static void applyTeamSettings(ServerPlayerEntity player, PlayerData data) {
        if (data == null) return;

        String playerName = player.getName().getString();
        Scoreboard scoreboard = player.getScoreboard();
        scoreboard.clearPlayerTeam(playerName);
        HunterVsMiceMod mod = HunterVsMiceMod.getInstance();

        if (mod != null && mod.isInPreparePhase())  {
            player.changeGameMode(GameMode.ADVENTURE);
            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);
        }

        if (data.getRole()  == PlayerData.Role.HUNTER) {
            Team hunters = scoreboard.getTeam("hunters");
            if (hunters != null) {
                scoreboard.addPlayerToTeam(playerName,  hunters);
            }

            if (mod != null && !mod.isInPreparePhase())  {
                player.changeGameMode(GameMode.SURVIVAL);
            }
        } else {
            Team mice = scoreboard.getTeam("mice");
            if (mice != null) {
                scoreboard.addPlayerToTeam(playerName,  mice);
            }

            if (data.isMouseDead())  {
                player.changeGameMode(GameMode.SPECTATOR);
            }

            else if (mod != null && !mod.isInPreparePhase())  {
                player.changeGameMode(GameMode.SURVIVAL);
            }
        }
    }

    private void startPreparePhase(MinecraftServer server) {
        inPreparePhase = true;
        confirmedPlayers.clear();

        server.getPlayerManager().getPlayerList().forEach(player  -> {
            player.changeGameMode(GameMode.ADVENTURE);
            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);

            PlayerData data = PlayerData.get(player);
            if (data != null && data.getRole()  == PlayerData.Role.MOUSE) {
                data.setMouseDead(false);
            }
        });
        server.getPlayerManager().broadcast(
                Text.translatable("huntervsmicemod.message.prepare_all_confirm")
                        .formatted(Formatting.GOLD),
                false
        );

        server.getPlayerManager().getPlayerList().forEach(player  ->
                player.sendMessage(
                        Text.translatable("huntervsmicemod.message.press_key_to_confirm",
                                Text.literal("H").formatted(Formatting.YELLOW,  Formatting.BOLD)
                        ).formatted(Formatting.GOLD),
                        false
                )
        );

        server.getPlayerManager().getPlayerList().forEach(player  -> {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.getRole()  == PlayerData.Role.MOUSE) {
                data.setMouseDead(false);
                applyTeamSettings(player, data);
            }
        });
    }

    public void confirmStart(MinecraftServer server, UUID playerId) {
        if (!inPreparePhase) return;

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player == null) return;

        if (!confirmedPlayers.contains(playerId))  {
            confirmedPlayers.add(playerId);
            server.getPlayerManager().broadcast(
                    Text.translatable("huntervsmicemod.message.player_confirmed",
                                    player.getName().getString())
                            .formatted(Formatting.GREEN),
                    false
            );

            checkAllConfirmed(server);
        }
    }

    private void checkAllConfirmed(MinecraftServer server) {
        if (confirmedPlayers.size()  >= server.getPlayerManager().getPlayerList().size())  {
            prepareTimer = getConfig().prepareTime;
            server.getPlayerManager().broadcast(
                    Text.translatable("huntervsmicemod.message.all_confirmed_countdown",
                                    getConfig().prepareTime/20)
                            .formatted(Formatting.GOLD),
                    false
            );
        }
    }

    private void startGame(MinecraftServer server) {
        inPreparePhase = false;
        prepareTimer = -1;
        isGameStarted = true;

        server.getPlayerManager().getPlayerList().forEach(player  -> {
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                if (data.getRole()  == PlayerData.Role.HUNTER ||
                        (data.getRole()  == PlayerData.Role.MOUSE && !data.isMouseDead()))  {
                    player.changeGameMode(GameMode.SURVIVAL);
                }
                applyTeamSettings(player, data);
            }
        });

        server.getPlayerManager().broadcast(
                Text.translatable("huntervsmicemod.message.game_start_announcement")
                        .formatted(Formatting.GOLD),
                false
        );
    }

    public static HunterVsMiceMod getInstance() {
        return instance;
    }

    public boolean isInPreparePhase() {
        return inPreparePhase;
    }

    private boolean checkAllMiceDead(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())  {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.getRole()  == PlayerData.Role.MOUSE && !data.isMouseDead())  {
                return false;
            }
        }
        return true;
    }

    private void announceHuntersVictory(MinecraftServer server) {
        if (isReallocating.get()) return;

        isReallocating.set(true);
        server.getPlayerManager().broadcast(
                Text.translatable("huntervsmicemod.message.hunters_win")
                        .formatted(Formatting.GOLD),
                false);

        List<Text> winnerNames = new ArrayList<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.getRole() == PlayerData.Role.HUNTER) {
                winnerNames.add(Text.literal(player.getName().getString()).formatted(Formatting.BOLD));
            }
        }

        if (!winnerNames.isEmpty()) {
            MutableText winnersText = Text.translatable("huntervsmicemod.message.winning_hunters");
            for (int i = 0; i < winnerNames.size(); i++) {
                winnersText.append(winnerNames.get(i));
                if (i < winnerNames.size() - 1) {
                    winnersText.append(", ");
                }
            }
            server.getPlayerManager().broadcast(winnersText, false);
        }

        server.getPlayerManager().getPlayerList().forEach(player  -> {

            player.getInventory().clear();

            player.clearStatusEffects();

            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);

            player.setExperienceLevel(0);
            player.setExperiencePoints(0);

            PlayerData data = PlayerData.get(player);
            if (data != null) {
                data.setInitialized(false);
                data.randomizeRole();
                data.setMouseDead(false);
                applyTeamSettings(player, data);
            }
        });

        reallocateTimer = getConfig().reallocateDelay;
        isGameStarted = false;

        server.getPlayerManager().broadcast(
                Text.translatable("huntervsmicemod.message.returning_to_prepare")
                        .formatted(Formatting.YELLOW),
                false);
        startPreparePhase(server);
    }

    public static Map<String, PlayerData.Role> getAllPlayersRoles(MinecraftServer server) {
        Map<String, PlayerData.Role> roles = new ConcurrentHashMap<>();
        server.getPlayerManager().getPlayerList().forEach(player  -> {
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                roles.put(player.getName().getString(),  data.getRole());
            }
        });
        return roles;
    }
}