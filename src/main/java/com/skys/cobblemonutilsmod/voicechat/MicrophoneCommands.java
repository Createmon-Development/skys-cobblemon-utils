package com.skys.cobblemonutilsmod.voicechat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class MicrophoneCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("microphone")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.literal("volume")
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 500))
                                .executes(MicrophoneCommands::setVolumeCommand))
                        .executes(MicrophoneCommands::getVolumeCommand))
                .then(Commands.literal("range")
                        .then(Commands.argument("percent", IntegerArgumentType.integer(50, 1000))
                                .executes(MicrophoneCommands::setRangeCommand))
                        .executes(MicrophoneCommands::getRangeCommand))
                .then(Commands.literal("status")
                        .executes(MicrophoneCommands::statusCommand)));
    }

    private static int setVolumeCommand(CommandContext<CommandSourceStack> context) {
        int percent = IntegerArgumentType.getInteger(context, "percent");
        MicrophoneConfig.setVolumeBoostPercent(percent);

        context.getSource().sendSuccess(() -> Component.literal(
                "Microphone volume boost set to " + percent + "% (multiplier: " +
                        String.format("%.2f", MicrophoneConfig.getVolumeMultiplier()) + "x)"
        ), true);

        return 1;
    }

    private static int getVolumeCommand(CommandContext<CommandSourceStack> context) {
        int percent = MicrophoneConfig.getVolumeBoostPercent();
        float multiplier = MicrophoneConfig.getVolumeMultiplier();

        context.getSource().sendSuccess(() -> Component.literal(
                "Current microphone volume boost: " + percent + "% (multiplier: " +
                        String.format("%.2f", multiplier) + "x)"
        ), false);

        return 1;
    }

    private static int setRangeCommand(CommandContext<CommandSourceStack> context) {
        int percent = IntegerArgumentType.getInteger(context, "percent");
        MicrophoneConfig.setRangeBoostPercent(percent);

        context.getSource().sendSuccess(() -> Component.literal(
                "Microphone range boost set to " + percent + "% (multiplier: " +
                        String.format("%.2f", MicrophoneConfig.getRangeMultiplier()) + "x)"
        ), true);

        return 1;
    }

    private static int getRangeCommand(CommandContext<CommandSourceStack> context) {
        int percent = MicrophoneConfig.getRangeBoostPercent();
        float multiplier = MicrophoneConfig.getRangeMultiplier();

        context.getSource().sendSuccess(() -> Component.literal(
                "Current microphone range: " + percent + "% (multiplier: " +
                        String.format("%.2f", multiplier) + "x)"
        ), false);

        return 1;
    }

    private static int statusCommand(CommandContext<CommandSourceStack> context) {
        int volumePercent = MicrophoneConfig.getVolumeBoostPercent();
        int rangePercent = MicrophoneConfig.getRangeBoostPercent();

        context.getSource().sendSuccess(() -> Component.literal(
                "Microphone Settings:\n" +
                        "  Volume Boost: " + volumePercent + "% (" +
                        String.format("%.2f", MicrophoneConfig.getVolumeMultiplier()) + "x)\n" +
                        "  Range: " + rangePercent + "% (" +
                        String.format("%.2f", MicrophoneConfig.getRangeMultiplier()) + "x)"
        ), false);

        return 1;
    }
}
