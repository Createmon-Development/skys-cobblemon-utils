package com.skys.cobblemonutilsmod.voicechat;

import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModVoiceChatRegistry {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkysCobblemonUtils.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkysCobblemonUtils.MOD_ID);

    // Microphone Item
    public static final DeferredItem<Item> MICROPHONE = ITEMS.register("microphone",
            () -> new MicrophoneItem(new Item.Properties()));

    // Creative Tab for Voice Chat items
    public static final Supplier<CreativeModeTab> VOICE_CHAT_TAB = CREATIVE_MODE_TABS.register("voice_chat_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SkysCobblemonUtils.MOD_ID + ".voice_chat_tab"))
                    .icon(() -> MICROPHONE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(MICROPHONE.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
