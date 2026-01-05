package com.skys.cobblemonutilsmod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.skys.cobblemonutilsmod.archaeology.IBrushable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushItem.class)
public class BrushItemMixin {

    // Cooldown to prevent spamming the message every tick
    @Unique
    private static long skyscobblemon$lastMessageTime = 0;
    @Unique
    private static final long MESSAGE_COOLDOWN_MS = 1000; // 1 second cooldown

    /**
     * Injects after the HitResult type check to handle our custom brushable blocks.
     * We inject early and handle our blocks before vanilla tries to process them.
     */
    @Inject(
            method = "onUseTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/BlockHitResult;getBlockPos()Lnet/minecraft/core/BlockPos;"
            ),
            cancellable = true
    )
    private void skyscobblemon$handleCustomBrushableBlocks(
            Level level,
            LivingEntity livingEntity,
            ItemStack stack,
            int remainingUseDuration,
            CallbackInfo ci,
            @Local BlockHitResult blockHitResult
    ) {
        BlockPos pos = blockHitResult.getBlockPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // Only handle our custom brushable blocks
        if (blockEntity instanceof IBrushable brushable && livingEntity instanceof Player player) {
            // Check if the player has already brushed today
            if (brushable.brushableBlock$hasPlayerBrushedToday(player)) {
                // Show the unavailable message on the action bar (with cooldown to prevent spam)
                long currentTime = System.currentTimeMillis();
                if (currentTime - skyscobblemon$lastMessageTime >= MESSAGE_COOLDOWN_MS) {
                    Component message = brushable.brushableBlock$getUnavailableMessage(player);
                    if (message != null && player instanceof ServerPlayer serverPlayer) {
                        // Send as action bar message (overlay = true)
                        serverPlayer.displayClientMessage(message, true);
                    }
                    skyscobblemon$lastMessageTime = currentTime;
                }
                // Cancel - block already harvested by this player today
                ci.cancel();
                return;
            }

            // Calculate ticks used (same logic as vanilla)
            int useDuration = stack.getUseDuration(livingEntity);
            int ticksUsed = useDuration - remainingUseDuration + 1;

            // Only process on the correct tick intervals (same as vanilla: every 10 ticks, offset by 5)
            if (ticksUsed % 10 == 5) {
                boolean completed = brushable.brushableBlock$brush(level.getGameTime(), player, blockHitResult.getDirection());

                if (completed) {
                    // Damage the brush when brushing completes
                    EquipmentSlot slot = stack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND))
                            ? EquipmentSlot.OFFHAND
                            : EquipmentSlot.MAINHAND;
                    stack.hurtAndBreak(1, livingEntity, slot);
                }
            }

            // Cancel to prevent vanilla from trying to process this as a BrushableBlockEntity
            ci.cancel();
        }
    }
}
