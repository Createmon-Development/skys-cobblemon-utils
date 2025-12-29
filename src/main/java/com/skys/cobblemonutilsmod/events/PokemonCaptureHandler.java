package com.skys.cobblemonutilsmod.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitEvent;
import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import kotlin.Unit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Prevents players from catching Pokémon that they're not actively in battle with.
 * Pokéballs can still be thrown, but they'll just drop as items instead of starting a capture.
 */
public class PokemonCaptureHandler {

    public PokemonCaptureHandler() {
        // Register Cobblemon pokeball hit event
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.HIGHEST, this::onPokeballHit);
    }

    /**
     * Intercepts when a thrown Pokéball hits something.
     * If not in battle, prevents the capture.
     * The ball will naturally drop as an item when the capture is cancelled.
     */
    private Unit onPokeballHit(ThrownPokeballHitEvent event) {
        // Check if the thrower is a player
        if (event.getPokeBall().getOwner() instanceof ServerPlayer player) {
            // Check if player is currently in a battle
            if (!BattleAggroHandler.isPlayerInBattle(player.getUUID())) {
                // Cancel the capture process
                // The pokeball entity will handle dropping itself when cancelled
                event.cancel();

                // Send action bar message to player (appears above hotbar, less intrusive)
                player.displayClientMessage(
                    Component.literal("You can only catch Pokémon during battle!")
                        .withStyle(style -> style.withColor(0xFF5555)),
                    true  // Display as action bar
                );

                SkysCobblemonUtils.LOGGER.debug(
                    "Prevented {} from catching Pokémon outside of battle",
                    player.getName().getString()
                );
            }
        }

        return Unit.INSTANCE;
    }
}
