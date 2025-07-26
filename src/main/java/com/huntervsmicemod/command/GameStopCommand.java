package com.huntervsmicemod.command;

import com.huntervsmicemod.HunterVsMiceMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.literal;

public class GameStopCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("hvmstop")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> execute(context.getSource()))
        );
    }

    private static int execute(ServerCommandSource source) {
        HunterVsMiceMod mod = HunterVsMiceMod.getInstance();
        if (mod == null) {
            source.sendError(Text.translatable("huntervsmicemod.command.stop.mod_not_initialized"));
            return 0;
        }

        if (!mod.isGameStarted  && !mod.isInPreparePhase())  {
            source.sendError(Text.translatable("huntervsmicemod.command.stop.not_in_game"));
            return 0;
        }

        mod.stopGame(source.getServer());
        source.sendFeedback(
                () -> Text.translatable("huntervsmicemod.command.stop.success")
                        .formatted(Formatting.GREEN),
                true
        );
        return 1;
    }
}