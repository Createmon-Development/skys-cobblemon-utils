package com.skys.cobblemonutilsmod;

import com.skys.cobblemonutilsmod.archaeology.DailyResetManager;
import com.skys.cobblemonutilsmod.archaeology.ModArchaeologyRegistry;
import com.skys.cobblemonutilsmod.events.BattleAggroHandler;
import com.skys.cobblemonutilsmod.events.PokemonCaptureHandler;
import com.skys.cobblemonutilsmod.voicechat.MicrophoneCommands;
import com.skys.cobblemonutilsmod.voicechat.MicrophoneConfig;
import com.skys.cobblemonutilsmod.voicechat.ModVoiceChatRegistry;
import com.skys.cobblemonutilsmod.voicechat.SkysMicrophonePlugin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SkysCobblemonUtils.MOD_ID)
public class SkysCobblemonUtils {
    public static final String MOD_ID = "skyscobblemonutilsmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(SkysCobblemonUtils.class);

    public SkysCobblemonUtils(IEventBus modEventBus) {
        LOGGER.info("Initializing Sky's Cobblemon Utils");

        // Register archaeology blocks and entities
        ModArchaeologyRegistry.register(modEventBus);

        // Register voice chat items
        ModVoiceChatRegistry.register(modEventBus);

        // Register event handlers
        NeoForge.EVENT_BUS.register(new BattleAggroHandler());
        NeoForge.EVENT_BUS.register(DailyResetManager.class);
        NeoForge.EVENT_BUS.register(MicrophoneCommands.class);
        NeoForge.EVENT_BUS.register(this);
        new PokemonCaptureHandler(); // Registers itself via Cobblemon event system

        LOGGER.info("Sky's Cobblemon Utils initialized successfully");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MicrophoneConfig.init(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SkysMicrophonePlugin.cleanup();
        MicrophoneConfig.clearInstance();
    }
}
