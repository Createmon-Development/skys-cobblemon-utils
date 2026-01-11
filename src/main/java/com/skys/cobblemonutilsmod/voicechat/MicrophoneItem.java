package com.skys.cobblemonutilsmod.voicechat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MicrophoneItem extends Item {

    public static final String ENABLED_TAG = "microphone_enabled";

    public MicrophoneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // Only run on server side and for players
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        // Check if this microphone is enabled and being held (main hand or offhand)
        if (!isEnabled(stack)) {
            return;
        }

        // Only show message if this stack is in main hand or offhand
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (stack != mainHand && stack != offHand) {
            return;
        }

        // Show status message every 20 ticks (1 second) to keep it visible
        if (level.getGameTime() % 20 == 0) {
            int volumeBoost = MicrophoneConfig.getVolumeBoostPercent();
            int rangeBoost = MicrophoneConfig.getRangeBoostPercent();
            Component message = Component.literal("§aMicrophone ON §8| §7Vol: §f+" + volumeBoost + "% §8| §7Range: §f" + rangeBoost + "%");
            player.displayClientMessage(message, true);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            boolean currentState = isEnabled(stack);
            setEnabled(stack, !currentState);

            if (!currentState) {
                player.displayClientMessage(Component.literal("Microphone: ON - Voice boosted!"), true);
            } else {
                player.displayClientMessage(Component.literal("Microphone: OFF"), true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isEnabled(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        boolean enabled = isEnabled(stack);
        if (enabled) {
            tooltipComponents.add(Component.literal("§aEnabled"));
            tooltipComponents.add(Component.literal("§7Volume Boost: §f" + MicrophoneConfig.getVolumeBoostPercent() + "%"));
            tooltipComponents.add(Component.literal("§7Range Boost: §f" + MicrophoneConfig.getRangeBoostPercent() + "%"));
        } else {
            tooltipComponents.add(Component.literal("§cDisabled"));
        }
        tooltipComponents.add(Component.literal("§8Right-click to toggle"));
    }

    public static boolean isEnabled(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MicrophoneItem)) {
            return false;
        }
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getBoolean(ENABLED_TAG);
    }

    public static void setEnabled(ItemStack stack, boolean enabled) {
        if (stack.isEmpty()) return;

        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                data -> {
                    var tag = data.copyTag();
                    tag.putBoolean(ENABLED_TAG, enabled);
                    return net.minecraft.world.item.component.CustomData.of(tag);
                });
    }

    /**
     * Checks if a player is holding an enabled microphone in either hand.
     */
    public static boolean isPlayerUsingMicrophone(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return isEnabled(mainHand) || isEnabled(offHand);
    }
}
