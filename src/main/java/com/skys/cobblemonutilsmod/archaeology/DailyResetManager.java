package com.skys.cobblemonutilsmod.archaeology;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class DailyResetManager {
    private static LocalDate lastCheckedDate = null;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL_TICKS = 20; // Check once per second

    // Track all daily brushable block entities using thread-safe list
    private static final List<DailyBrushableBlockEntity> trackedBlocks = new CopyOnWriteArrayList<>();

    public static void registerBlock(DailyBrushableBlockEntity blockEntity) {
        if (!trackedBlocks.contains(blockEntity)) {
            trackedBlocks.add(blockEntity);
        }
    }

    public static void unregisterBlock(DailyBrushableBlockEntity blockEntity) {
        trackedBlocks.remove(blockEntity);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        // Clean up unloaded blocks
        trackedBlocks.removeIf(block -> block.isRemoved() || block.getLevel() == null);

        MinecraftServer server = event.getServer();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        if (lastCheckedDate == null) {
            lastCheckedDate = today;
            return;
        }

        if (today.isAfter(lastCheckedDate)) {
            SkysCobblemonUtils.LOGGER.info("Midnight detected! Resetting all daily brushable blocks...");
            resetAllBrushableBlocks(server, true);
            lastCheckedDate = today;
        }
    }

    public static void resetAllBrushableBlocks(MinecraftServer server, boolean showParticles) {
        int resetCount = 0;

        // Clean up unloaded blocks first
        trackedBlocks.removeIf(block -> block.isRemoved() || block.getLevel() == null);

        for (DailyBrushableBlockEntity brushable : new ArrayList<>(trackedBlocks)) {
            if (brushable.getLevel() instanceof ServerLevel serverLevel) {
                brushable.forceReset();
                resetCount++;

                if (showParticles) {
                    spawnResetParticles(serverLevel, brushable.getBlockPos());
                }
            }
        }

        SkysCobblemonUtils.LOGGER.info("Reset {} daily brushable blocks", resetCount);
    }

    public static void resetBrushableBlocksForPlayer(MinecraftServer server, UUID playerUUID, boolean showParticles) {
        int resetCount = 0;

        // Clean up unloaded blocks first
        trackedBlocks.removeIf(block -> block.isRemoved() || block.getLevel() == null);

        for (DailyBrushableBlockEntity brushable : new ArrayList<>(trackedBlocks)) {
            if (brushable.getLevel() instanceof ServerLevel serverLevel) {
                if (brushable.hasBrushedToday(playerUUID)) {
                    brushable.resetForPlayer(playerUUID);
                    resetCount++;

                    if (showParticles) {
                        spawnResetParticles(serverLevel, brushable.getBlockPos());
                    }
                }
            }
        }

        SkysCobblemonUtils.LOGGER.info("Reset {} daily brushable blocks for player {}", resetCount, playerUUID);
    }

    private static void spawnResetParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                ParticleTypes.WAX_ON,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                15,
                0.4, 0.4, 0.4,
                0.02
        );
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("dailybrushable")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                .then(Commands.literal("reset")
                        .then(Commands.literal("all")
                                .executes(DailyResetManager::resetAllCommand))
                        .then(Commands.literal("me")
                                .executes(DailyResetManager::resetMeCommand))
                        .then(Commands.literal("player")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(DailyResetManager::resetPlayerCommand))))
                .then(Commands.literal("status")
                        .executes(DailyResetManager::statusCommand)));
    }

    private static int resetAllCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        resetAllBrushableBlocks(server, true);
        source.sendSuccess(() -> Component.literal("Reset all daily brushable blocks!"), true);

        return 1;
    }

    private static int resetMeCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        if (source.getEntity() instanceof ServerPlayer player) {
            resetBrushableBlocksForPlayer(server, player.getUUID(), true);
            source.sendSuccess(() -> Component.literal("Reset daily brushable blocks for yourself!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("This command must be run by a player!"));
            return 0;
        }
    }

    private static int resetPlayerCommand(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            MinecraftServer server = source.getServer();
            ServerPlayer target = EntityArgument.getPlayer(context, "target");

            resetBrushableBlocksForPlayer(server, target.getUUID(), true);
            source.sendSuccess(() -> Component.literal("Reset daily brushable blocks for " + target.getName().getString() + "!"), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to reset: " + e.getMessage()));
            return 0;
        }
    }

    private static int statusCommand(CommandContext<CommandSourceStack> context) {
        // Clean up unloaded blocks first
        trackedBlocks.removeIf(block -> block.isRemoved() || block.getLevel() == null);

        int totalCount = trackedBlocks.size();
        int brushedCount = 0;

        for (DailyBrushableBlockEntity brushable : trackedBlocks) {
            if (!brushable.getBrushedPlayers().isEmpty()) {
                brushedCount++;
            }
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        final int finalTotal = totalCount;
        final int finalBrushed = brushedCount;

        context.getSource().sendSuccess(() -> Component.literal(
                "Daily Brushable Blocks Status:\n" +
                "- Current date: " + today + "\n" +
                "- Timezone: " + ZoneId.systemDefault() + "\n" +
                "- Loaded blocks: " + finalTotal + "\n" +
                "- Blocks with activity today: " + finalBrushed
        ), false);

        return 1;
    }
}
