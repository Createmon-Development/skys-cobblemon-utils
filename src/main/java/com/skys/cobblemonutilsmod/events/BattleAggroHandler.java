package com.skys.cobblemonutilsmod.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles preventing mob aggro on players while they're in Pokemon battles.
 * Players will lose aggro when entering battle and cannot gain new aggro while in battle.
 * Boss monsters (Wither, Ender Dragon, Warden) are exempt from this protection.
 */
public class BattleAggroHandler {
    private static final Set<UUID> playersInBattle = new HashSet<>();

    public BattleAggroHandler() {
        // Register Cobblemon battle events
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, this::onBattleStart);
        CobblemonEvents.BATTLE_FLED.subscribe(Priority.NORMAL, this::onBattleEnd);
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, this::onBattleEnd);
    }

    /**
     * Called when a battle starts. Adds players to the tracking set and clears their aggro.
     */
    private Unit onBattleStart(BattleStartedEvent event) {
        event.getBattle().getActors().forEach(actor -> {
            if (actor instanceof PlayerBattleActor playerActor) {
                ServerPlayer player = playerActor.getEntity();
                if (player != null) {
                    playersInBattle.add(player.getUUID());
                    clearPlayerAggro(player);
                    SkysCobblemonUtils.LOGGER.debug("Player {} entered battle, clearing aggro", player.getName().getString());
                }
            }
        });
        return Unit.INSTANCE;
    }

    /**
     * Called when a battle ends. Removes players from the tracking set.
     */
    private Unit onBattleEnd(Object event) {
        // Extract players from battle end event
        if (event instanceof com.cobblemon.mod.common.api.events.battles.BattleFledEvent fledEvent) {
            fledEvent.getBattle().getActors().forEach(actor -> {
                if (actor instanceof PlayerBattleActor playerActor) {
                    ServerPlayer player = playerActor.getEntity();
                    if (player != null) {
                        playersInBattle.remove(player.getUUID());
                        SkysCobblemonUtils.LOGGER.debug("Player {} fled from battle", player.getName().getString());
                    }
                }
            });
        } else if (event instanceof com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent victoryEvent) {
            victoryEvent.getBattle().getActors().forEach(actor -> {
                if (actor instanceof PlayerBattleActor playerActor) {
                    ServerPlayer player = playerActor.getEntity();
                    if (player != null) {
                        playersInBattle.remove(player.getUUID());
                        SkysCobblemonUtils.LOGGER.debug("Player {} finished battle", player.getName().getString());
                    }
                }
            });
        }
        return Unit.INSTANCE;
    }

    /**
     * Prevents mobs from targeting players who are in battle.
     * Boss monsters (Wither, Ender Dragon, Warden) are exempt.
     * Note: This event doesn't fire for Brain-AI mobs like Piglins/Piglin Brutes - they're handled separately in clearPlayerAggro.
     */
    @SubscribeEvent
    public void onMobTargetChange(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) {
            if (playersInBattle.contains(player.getUUID())) {
                // Allow boss monsters to ignore battle protection
                if (isBossMonster(event.getEntity())) {
                    return;
                }

                event.setCanceled(true);
                SkysCobblemonUtils.LOGGER.debug("Prevented mob from targeting player {} (in battle)", player.getName().getString());
            }
        }
    }

    /**
     * Tick event to continuously clear Brain-AI mob targets while players are in battle.
     * Brain-AI mobs (like Piglin Brutes) don't fire LivingChangeTargetEvent, so we need to actively clear them.
     */
    @SubscribeEvent
    public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (playersInBattle.isEmpty()) {
            return; // No players in battle, skip processing
        }

        event.getServer().getAllLevels().forEach(level -> {
            playersInBattle.forEach(playerUUID -> {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
                if (player != null && player.serverLevel() == level) {
                    // Check for Brain-AI mobs (Piglins, Piglin Brutes) targeting the player
                    level.getEntitiesOfClass(AbstractPiglin.class, player.getBoundingBox().inflate(16.0D))
                        .forEach(piglin -> {
                            if (piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == player) {
                                piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                                piglin.setTarget(null);
                                SkysCobblemonUtils.LOGGER.debug("Cleared Brain-AI targeting from {} on player {} (in battle)",
                                    piglin.getType().getDescription().getString(),
                                    player.getName().getString());
                            }
                        });
                }
            });
        });
    }

    /**
     * Removes aggro protection when a player dies.
     */
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (playersInBattle.remove(player.getUUID())) {
                SkysCobblemonUtils.LOGGER.debug("Player {} died - removed from battle protection", player.getName().getString());
            }
        }
    }

    /**
     * Check if an entity is a boss monster that should ignore battle protection.
     */
    private boolean isBossMonster(net.minecraft.world.entity.Entity entity) {
        return entity instanceof WitherBoss ||
               entity instanceof EnderDragon ||
               entity instanceof Warden;
    }

    /**
     * Clears all mob aggro targeting the specified player.
     * Boss monsters are exempt and will keep their targets.
     */
    private void clearPlayerAggro(ServerPlayer player) {
        // Get all nearby mobs and clear their target if it's this player (except bosses)
        player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0D))
            .forEach(mob -> {
                if (!isBossMonster(mob)) {
                    if (mob.getTarget() == player) {
                        mob.setTarget(null);
                        SkysCobblemonUtils.LOGGER.debug("Cleared aggro from {} on player {}",
                            mob.getType().getDescription().getString(),
                            player.getName().getString());
                    }

                    // Special handling for neutral mobs (like piglins) - clear their anger
                    if (mob instanceof NeutralMob neutralMob) {
                        neutralMob.stopBeingAngry();
                        SkysCobblemonUtils.LOGGER.debug("Cleared neutral mob anger from {} towards player {}",
                            mob.getType().getDescription().getString(),
                            player.getName().getString());
                    }

                    // Special handling for Brain-AI mobs (Piglins, Piglin Brutes)
                    // They use Brain memory instead of traditional goals
                    if (mob instanceof AbstractPiglin piglin) {
                        piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                        SkysCobblemonUtils.LOGGER.debug("Cleared Brain-AI attack target from {} towards player {}",
                            mob.getType().getDescription().getString(),
                            player.getName().getString());
                    }
                }
            });
    }

    /**
     * Check if a player is currently in battle.
     */
    public static boolean isPlayerInBattle(UUID playerUUID) {
        return playersInBattle.contains(playerUUID);
    }
}
