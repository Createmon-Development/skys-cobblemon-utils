package com.skys.cobblemonutilsmod.archaeology;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for custom brushable block entities.
 * Allows the BrushItemMixin to detect and handle our custom blocks.
 */
public interface IBrushable {
    /**
     * Called when a player brushes this block entity.
     *
     * @param gameTime The current game time
     * @param player The player brushing
     * @param direction The direction the player is brushing from
     * @return true if brushing completed (loot dropped), false otherwise
     */
    boolean brushableBlock$brush(long gameTime, Player player, Direction direction);

    /**
     * Gets a message to display when the player cannot brush this block.
     * Returns null if the player can brush (no message needed).
     *
     * @param player The player attempting to brush
     * @return A Component message to display, or null if brushing is allowed
     */
    @Nullable
    Component brushableBlock$getUnavailableMessage(Player player);

    /**
     * Checks if the player has already harvested this block today.
     *
     * @param player The player to check
     * @return true if the player has already brushed today
     */
    boolean brushableBlock$hasPlayerBrushedToday(Player player);
}
