package com.skys.cobblemonutilsmod;

import com.skys.cobblemonutilsmod.events.BattleAggroHandler;
import com.skys.cobblemonutilsmod.events.PokemonCaptureHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SkysCobblemonUtils.MOD_ID)
public class SkysCobblemonUtils {
    public static final String MOD_ID = "skyscobblemonutilsmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(SkysCobblemonUtils.class);

    public SkysCobblemonUtils(IEventBus modEventBus) {
        LOGGER.info("Initializing Sky's Cobblemon Utils");

        // Register event handlers
        NeoForge.EVENT_BUS.register(new BattleAggroHandler());
        new PokemonCaptureHandler(); // Registers itself via Cobblemon event system

        LOGGER.info("Sky's Cobblemon Utils initialized successfully");
    }
}
