package com.skys.cobblemonutilsmod.archaeology;

import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModArchaeologyRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SkysCobblemonUtils.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkysCobblemonUtils.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SkysCobblemonUtils.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SkysCobblemonUtils.MOD_ID);

    // Daily Suspicious Sand
    public static final DeferredBlock<Block> DAILY_SUSPICIOUS_SAND = BLOCKS.register("daily_suspicious_sand",
            () -> new DailyBrushableBlock(
                    Blocks.SAND,
                    SoundEvents.BRUSH_SAND,
                    SoundEvents.BRUSH_SAND_COMPLETED,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SUSPICIOUS_SAND)
            ));

    // Daily Suspicious Gravel
    public static final DeferredBlock<Block> DAILY_SUSPICIOUS_GRAVEL = BLOCKS.register("daily_suspicious_gravel",
            () -> new DailyBrushableBlock(
                    Blocks.GRAVEL,
                    SoundEvents.BRUSH_GRAVEL,
                    SoundEvents.BRUSH_GRAVEL_COMPLETED,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SUSPICIOUS_GRAVEL)
            ));

    // Block Items
    public static final DeferredItem<BlockItem> DAILY_SUSPICIOUS_SAND_ITEM = ITEMS.register("daily_suspicious_sand",
            () -> new BlockItem(DAILY_SUSPICIOUS_SAND.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> DAILY_SUSPICIOUS_GRAVEL_ITEM = ITEMS.register("daily_suspicious_gravel",
            () -> new BlockItem(DAILY_SUSPICIOUS_GRAVEL.get(), new Item.Properties()));

    // Block Entity
    public static final Supplier<BlockEntityType<DailyBrushableBlockEntity>> DAILY_BRUSHABLE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("daily_brushable_block_entity",
                    () -> BlockEntityType.Builder.of(
                            DailyBrushableBlockEntity::new,
                            DAILY_SUSPICIOUS_SAND.get(),
                            DAILY_SUSPICIOUS_GRAVEL.get()
                    ).build(null));

    // Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARCHAEOLOGY_TAB = CREATIVE_TABS.register("archaeology_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SkysCobblemonUtils.MOD_ID + ".archaeology"))
                    .icon(() -> DAILY_SUSPICIOUS_SAND_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(DAILY_SUSPICIOUS_SAND_ITEM.get());
                        output.accept(DAILY_SUSPICIOUS_GRAVEL_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        SkysCobblemonUtils.LOGGER.info("Registered archaeology blocks and entities");
    }
}
