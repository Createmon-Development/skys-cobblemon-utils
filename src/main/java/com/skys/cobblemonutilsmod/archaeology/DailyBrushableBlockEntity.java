package com.skys.cobblemonutilsmod.archaeology;

import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class DailyBrushableBlockEntity extends BlockEntity implements IBrushable {
    public static final ResourceKey<LootTable> DEFAULT_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(SkysCobblemonUtils.MOD_ID, "archaeology/daily_brushable")
    );

    private static final int BRUSH_COOLDOWN_TICKS = 10;
    private static final int BRUSH_RESET_TICKS = 40;
    private static final int REQUIRED_BRUSHES = 10;

    // Per-player tracking
    private final Set<UUID> brushedPlayers = new HashSet<>();
    private LocalDate lastResetDate = LocalDate.now(ZoneId.systemDefault());

    // Brushing state
    private int brushCount = 0;
    private long brushCountResetsAtTick = 0;
    private long coolDownEndsAtTick = 0;
    @Nullable
    private UUID brushingPlayerUUID;
    @Nullable
    private Direction hitDirection;
    private ItemStack currentItem = ItemStack.EMPTY;

    // Loot table (configurable via datapack)
    private ResourceKey<LootTable> lootTable = DEFAULT_LOOT_TABLE;
    private long lootTableSeed = 0;

    public DailyBrushableBlockEntity(BlockPos pos, BlockState state) {
        super(ModArchaeologyRegistry.DAILY_BRUSHABLE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            DailyResetManager.registerBlock(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        DailyResetManager.unregisterBlock(this);
    }

    public boolean hasLootAvailable(ServerPlayer player) {
        checkAndResetForNewDay();
        return !brushedPlayers.contains(player.getUUID());
    }

    private void checkAndResetForNewDay() {
        if (level == null) return;
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (today.isAfter(lastResetDate)) {
            resetForNewDay();
        }
    }

    public void resetForNewDay() {
        brushedPlayers.clear();
        lastResetDate = LocalDate.now(ZoneId.systemDefault());
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void forceReset() {
        resetForNewDay();
        SkysCobblemonUtils.LOGGER.info("Force reset brushable block at {}", worldPosition);
    }

    public void resetForPlayer(UUID playerUUID) {
        if (brushedPlayers.remove(playerUUID)) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public boolean brushableBlock$brush(long gameTime, Player player, Direction direction) {
        if (level == null || level.isClientSide()) {
            return false;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        // Check if this player can brush (hasn't brushed today)
        if (!hasLootAvailable(serverPlayer)) {
            return false;
        }

        // Check if another player is currently brushing
        if (brushingPlayerUUID != null && !brushingPlayerUUID.equals(player.getUUID())) {
            return false;
        }

        // Set the brushing player
        if (brushingPlayerUUID == null) {
            brushingPlayerUUID = player.getUUID();
        }

        if (hitDirection == null) {
            hitDirection = direction;
        }

        brushCountResetsAtTick = gameTime + BRUSH_RESET_TICKS;

        if (gameTime >= coolDownEndsAtTick && level instanceof ServerLevel serverLevel) {
            coolDownEndsAtTick = gameTime + BRUSH_COOLDOWN_TICKS;
            int previousState = getCompletionState();

            if (++brushCount >= REQUIRED_BRUSHES) {
                brushingCompleted(serverPlayer);
                return true;
            } else {
                level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
                int newState = getCompletionState();
                if (previousState != newState) {
                    BlockState newBlockState = getBlockState().setValue(BlockStateProperties.DUSTED, newState);
                    level.setBlock(worldPosition, newBlockState, 3);
                }
                return false;
            }
        }

        return false;
    }

    private void brushingCompleted(ServerPlayer player) {
        if (level == null || level.isClientSide()) return;

        // Generate and drop the loot
        ItemStack lootItem = generateLoot(player);
        if (!lootItem.isEmpty()) {
            dropItem(lootItem);
        }

        // Mark this player as having brushed today
        brushedPlayers.add(player.getUUID());

        // Reset brushing state but keep the block visually "depleted" (dusted state 3)
        resetBrushingStateKeepVisual();

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private ItemStack generateLoot(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return ItemStack.EMPTY;
        }

        LootTable table = serverLevel.getServer().reloadableRegistries()
                .getLootTable(lootTable);

        LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withLuck(player.getLuck())
                .withParameter(LootContextParams.THIS_ENTITY, player);

        LootParams params = builder.create(LootContextParamSets.CHEST);

        for (ItemStack stack : table.getRandomItems(params, lootTableSeed != 0 ? lootTableSeed : serverLevel.getRandom().nextLong())) {
            if (!stack.isEmpty()) {
                return stack; // Return only the first item (archaeology gives 1 item)
            }
        }

        return ItemStack.EMPTY;
    }

    private void dropItem(ItemStack item) {
        if (level == null || item.isEmpty()) return;

        Direction dir = Objects.requireNonNullElse(hitDirection, Direction.UP);
        BlockPos dropPos = worldPosition.relative(dir);

        double x = dropPos.getX() + 0.5;
        double y = dropPos.getY() + 0.5 + (EntityType.ITEM.getHeight() / 2.0);
        double z = dropPos.getZ() + 0.5;

        ItemEntity itemEntity = new ItemEntity(level, x, y, z, item);
        itemEntity.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(itemEntity);
    }

    public void checkReset() {
        if (level == null) return;

        // If any players have brushed today, keep the block at dusted state 3
        // Don't reset the visual appearance until the daily reset
        if (!brushedPlayers.isEmpty()) {
            // Just reset the brushing state variables, but keep visual at state 3
            brushingPlayerUUID = null;
            hitDirection = null;
            brushCount = 0;
            brushCountResetsAtTick = 0;
            coolDownEndsAtTick = 0;
            currentItem = ItemStack.EMPTY;

            // Ensure block stays at dusted state 3 (harvested appearance)
            if (!level.isClientSide()) {
                BlockState state = getBlockState();
                if (state.hasProperty(BlockStateProperties.DUSTED) && state.getValue(BlockStateProperties.DUSTED) != 3) {
                    level.setBlock(worldPosition, state.setValue(BlockStateProperties.DUSTED, 3), 3);
                }
            }
            return;
        }

        if (brushCount != 0 && level.getGameTime() >= brushCountResetsAtTick) {
            int previousState = getCompletionState();
            brushCount = Math.max(0, brushCount - 2);
            int newState = getCompletionState();

            if (previousState != newState) {
                level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.DUSTED, newState), 3);
            }

            brushCountResetsAtTick = level.getGameTime() + 4L;
        }

        if (brushCount == 0) {
            resetBrushingState();
        } else {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
        }
    }

    private void resetBrushingState() {
        brushingPlayerUUID = null;
        hitDirection = null;
        brushCount = 0;
        brushCountResetsAtTick = 0;
        coolDownEndsAtTick = 0;
        currentItem = ItemStack.EMPTY;

        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(BlockStateProperties.DUSTED) && state.getValue(BlockStateProperties.DUSTED) != 0) {
                level.setBlock(worldPosition, state.setValue(BlockStateProperties.DUSTED, 0), 3);
            }
        }
    }

    /**
     * Resets the brushing state but keeps the visual "depleted" appearance (dusted = 3).
     * Used when a player completes brushing so the block shows it's been harvested.
     */
    private void resetBrushingStateKeepVisual() {
        brushingPlayerUUID = null;
        hitDirection = null;
        brushCount = 0;
        brushCountResetsAtTick = 0;
        coolDownEndsAtTick = 0;
        currentItem = ItemStack.EMPTY;

        // Keep the block at dusted state 3 to show it's been harvested
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(BlockStateProperties.DUSTED) && state.getValue(BlockStateProperties.DUSTED) != 3) {
                level.setBlock(worldPosition, state.setValue(BlockStateProperties.DUSTED, 3), 3);
            }
        }
    }

    private int getCompletionState() {
        if (brushCount == 0) return 0;
        if (brushCount < 3) return 1;
        if (brushCount < 6) return 2;
        return 3;
    }

    public ItemStack getItem(Player player) {
        if (brushingPlayerUUID != null && brushingPlayerUUID.equals(player.getUUID())) {
            if (currentItem.isEmpty() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                currentItem = generateLoot(serverPlayer);
            }
            return currentItem;
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    public Direction getHitDirection() {
        return hitDirection;
    }

    public boolean hasBrushedToday(UUID playerUUID) {
        checkAndResetForNewDay();
        return brushedPlayers.contains(playerUUID);
    }

    public Set<UUID> getBrushedPlayers() {
        return new HashSet<>(brushedPlayers);
    }

    public void setLootTable(ResourceKey<LootTable> lootTable, long seed) {
        this.lootTable = lootTable;
        this.lootTableSeed = seed;
        setChanged();
    }

    // IBrushable interface methods

    @Override
    @Nullable
    public net.minecraft.network.chat.Component brushableBlock$getUnavailableMessage(Player player) {
        if (!hasBrushedToday(player.getUUID())) {
            return null; // Player can brush, no message needed
        }

        // Calculate time until reset
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now(zone).plusDays(1), LocalTime.MIDNIGHT);
        Duration timeUntilReset = Duration.between(now, midnight);

        long hours = timeUntilReset.toHours();
        long minutes = timeUntilReset.toMinutesPart();
        long seconds = timeUntilReset.toSecondsPart();

        String countdown = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return net.minecraft.network.chat.Component.literal("Already harvested today! Resets in: " + countdown);
    }

    @Override
    public boolean brushableBlock$hasPlayerBrushedToday(Player player) {
        return hasBrushedToday(player.getUUID());
    }

    // NBT Serialization
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Save loot table
        tag.putString("LootTable", lootTable.location().toString());
        if (lootTableSeed != 0) {
            tag.putLong("LootTableSeed", lootTableSeed);
        }

        // Save brushed players
        ListTag playersTag = new ListTag();
        for (UUID uuid : brushedPlayers) {
            playersTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put("BrushedPlayers", playersTag);

        // Save last reset date
        tag.putString("LastResetDate", lastResetDate.toString());

        // Save current brushing state
        if (hitDirection != null) {
            tag.putInt("HitDirection", hitDirection.ordinal());
        }
        if (brushingPlayerUUID != null) {
            tag.putUUID("BrushingPlayer", brushingPlayerUUID);
        }
        tag.putInt("BrushCount", brushCount);

        // Add countdown data for display mods (Jade/WAILA/TheOneProbe)
        addCountdownData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Load loot table
        if (tag.contains("LootTable")) {
            lootTable = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.parse(tag.getString("LootTable")));
        }
        if (tag.contains("LootTableSeed")) {
            lootTableSeed = tag.getLong("LootTableSeed");
        }

        // Load brushed players
        brushedPlayers.clear();
        if (tag.contains("BrushedPlayers")) {
            ListTag playersTag = tag.getList("BrushedPlayers", Tag.TAG_INT_ARRAY);
            for (Tag playerTag : playersTag) {
                brushedPlayers.add(NbtUtils.loadUUID(playerTag));
            }
        }

        // Load last reset date
        if (tag.contains("LastResetDate")) {
            try {
                lastResetDate = LocalDate.parse(tag.getString("LastResetDate"));
            } catch (Exception e) {
                lastResetDate = LocalDate.now(ZoneId.systemDefault());
            }
        }

        // Load current brushing state
        if (tag.contains("HitDirection")) {
            hitDirection = Direction.values()[tag.getInt("HitDirection")];
        }
        if (tag.contains("BrushingPlayer")) {
            brushingPlayerUUID = tag.getUUID("BrushingPlayer");
        }
        brushCount = tag.getInt("BrushCount");

        // Check if we need to reset for a new day
        checkAndResetForNewDay();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);

        if (hitDirection != null) {
            tag.putInt("HitDirection", hitDirection.ordinal());
        }
        if (brushingPlayerUUID != null) {
            tag.putUUID("BrushingPlayer", brushingPlayerUUID);
            if (!currentItem.isEmpty()) {
                tag.put("Item", currentItem.save(registries));
            }
        }

        // Send brushed players to client for visual feedback
        ListTag playersTag = new ListTag();
        for (UUID uuid : brushedPlayers) {
            playersTag.add(NbtUtils.createUUID(uuid));
        }
        tag.put("BrushedPlayers", playersTag);

        // Add countdown information for display mods (WAILA/Jade/TheOneProbe)
        addCountdownData(tag);

        return tag;
    }

    /**
     * Adds countdown data to the NBT tag for display by mods like Jade/WAILA/TheOneProbe.
     */
    private void addCountdownData(CompoundTag tag) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now(zone).plusDays(1), LocalTime.MIDNIGHT);
        Duration timeUntilReset = Duration.between(now, midnight);

        // Total seconds until reset
        long secondsUntilReset = timeUntilReset.getSeconds();
        tag.putLong("SecondsUntilReset", secondsUntilReset);

        // Human-readable breakdown
        long hours = timeUntilReset.toHours();
        long minutes = timeUntilReset.toMinutesPart();
        long seconds = timeUntilReset.toSecondsPart();
        tag.putInt("HoursUntilReset", (int) hours);
        tag.putInt("MinutesUntilReset", (int) minutes);
        tag.putInt("SecondsPartUntilReset", (int) seconds);

        // Formatted string for easy display
        String countdown = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        tag.putString("ResetCountdown", countdown);

        // Number of players who have brushed today
        tag.putInt("PlayersBrushedToday", brushedPlayers.size());

        // Timezone info
        tag.putString("Timezone", zone.getId());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
