package com.skys.cobblemonutilsmod.items;

import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for all custom items in the mod.
 * Add new items here following the example pattern.
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkysCobblemonUtils.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkysCobblemonUtils.MOD_ID);

    // Custom gym badge items
    public static final DeferredItem<Item> DIESELTON_BADGE = ITEMS.register("dieselton_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    public static final DeferredItem<Item> HEARTBOUND_BADGE = ITEMS.register("heartbound_badge",
        () -> new GymBadgeItem(new Item.Properties()));

    // Creative tab for the mod items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COBBLEMON_UTILS_TAB = CREATIVE_MODE_TABS.register("cobblemon_utils_tab", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.skyscobblemonutilsmod"))
        .icon(() -> new ItemStack(DIESELTON_BADGE.get()))
        .displayItems((parameters, output) -> {
            output.accept(DIESELTON_BADGE.get());
            output.accept(HEARTBOUND_BADGE.get());
        }).build());

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
        SkysCobblemonUtils.LOGGER.info("Registered {} custom items", ITEMS.getEntries().size());
    }
}
