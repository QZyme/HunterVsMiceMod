package com.huntervsmicemod.command;

import com.huntervsmicemod.HunterVsMiceMod;
import com.huntervsmicemod.PlayerData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TeamChangeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("teamchange")
                .requires(source -> source.hasPermissionLevel(2))
                .then(argument("player", StringArgumentType.string())
                        .then(argument("role", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("hunter");
                                    builder.suggest("mouse");
                                    return builder.buildFuture();
                                })
                                .executes(TeamChangeCommand::execute)))
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context,  "player");
        ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);

        if (player == null) {
            context.getSource().sendError(Text.translatable("huntervsmicemod.command.teamchange.offline",  playerName));
            return 0;
        }

        String roleStr = StringArgumentType.getString(context,  "role");
        PlayerData data = PlayerData.get(player);

        if (data == null) {
            data = new PlayerData();
            PlayerData.set(player,  data);
        }

        PlayerData.Role newRole;
        if ("hunter".equalsIgnoreCase(roleStr)) {
            newRole = PlayerData.Role.HUNTER;
            context.getSource().sendMessage(Text.translatable("huntervsmicemod.command.teamchange.success",
                    player.getName(),
                    Text.translatable("huntervsmicemod.role.hunter").formatted(Formatting.RED)
            ));
        } else if ("mouse".equalsIgnoreCase(roleStr)) {
            newRole = PlayerData.Role.MOUSE;
            context.getSource().sendMessage(Text.translatable("huntervsmicemod.command.teamchange.success",
                    player.getName(),
                    Text.translatable("huntervsmicemod.role.mouse").formatted(Formatting.GREEN)
            ));
        } else {
            context.getSource().sendError(Text.translatable("huntervsmicemod.command.teamchange.invalid"));
            return 0;
        }

        data.setRole(newRole);
        player.sendMessage(Text.translatable("huntervsmicemod.message.role_changed",
                newRole == PlayerData.Role.HUNTER ?
                        Text.translatable("huntervsmicemod.role.hunter").formatted(Formatting.RED)  :
                        Text.translatable("huntervsmicemod.role.mouse").formatted(Formatting.GREEN)
        ), false);

        HunterVsMiceMod.applyTeamSettings(player,  data);
        return 1;
    }
}