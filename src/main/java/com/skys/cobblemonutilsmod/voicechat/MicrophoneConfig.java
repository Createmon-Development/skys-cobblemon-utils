package com.skys.cobblemonutilsmod.voicechat;

import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

/**
 * Server-side configuration for the microphone item.
 * Stores volume and range boost settings that persist across server restarts.
 */
public class MicrophoneConfig extends SavedData {

    private static final String DATA_NAME = SkysCobblemonUtils.MOD_ID + "_microphone_config";

    // Default values
    private static final int DEFAULT_VOLUME_BOOST = 20;  // 20% volume boost
    private static final int DEFAULT_RANGE_BOOST = 150;  // 150% range (1.5x normal)

    // Config values
    private int volumeBoostPercent = DEFAULT_VOLUME_BOOST;
    private int rangeBoostPercent = DEFAULT_RANGE_BOOST;

    // Singleton instance for easy access
    private static MicrophoneConfig instance;

    public MicrophoneConfig() {
    }

    public static MicrophoneConfig create() {
        return new MicrophoneConfig();
    }

    public static MicrophoneConfig load(CompoundTag tag, HolderLookup.Provider registries) {
        MicrophoneConfig config = new MicrophoneConfig();
        config.volumeBoostPercent = tag.getInt("VolumeBoostPercent");
        config.rangeBoostPercent = tag.getInt("RangeBoostPercent");

        // Apply defaults if values are 0 (first load)
        if (config.volumeBoostPercent == 0) {
            config.volumeBoostPercent = DEFAULT_VOLUME_BOOST;
        }
        if (config.rangeBoostPercent == 0) {
            config.rangeBoostPercent = DEFAULT_RANGE_BOOST;
        }

        return config;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putInt("VolumeBoostPercent", volumeBoostPercent);
        tag.putInt("RangeBoostPercent", rangeBoostPercent);
        return tag;
    }

    public static void init(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        instance = storage.computeIfAbsent(
                new SavedData.Factory<>(MicrophoneConfig::create, MicrophoneConfig::load),
                DATA_NAME
        );
        SkysCobblemonUtils.LOGGER.info("Loaded microphone config: Volume={}%, Range={}%",
                instance.volumeBoostPercent, instance.rangeBoostPercent);
    }

    public static void clearInstance() {
        instance = null;
    }

    // Static accessors for easy access from other classes

    public static int getVolumeBoostPercent() {
        return instance != null ? instance.volumeBoostPercent : DEFAULT_VOLUME_BOOST;
    }

    public static int getRangeBoostPercent() {
        return instance != null ? instance.rangeBoostPercent : DEFAULT_RANGE_BOOST;
    }

    public static float getVolumeMultiplier() {
        return 1.0f + (getVolumeBoostPercent() / 100.0f);
    }

    public static float getRangeMultiplier() {
        return getRangeBoostPercent() / 100.0f;
    }

    public static void setVolumeBoostPercent(int percent) {
        if (instance != null) {
            instance.volumeBoostPercent = Math.max(0, Math.min(500, percent)); // Cap at 500%
            instance.setDirty();
        }
    }

    public static void setRangeBoostPercent(int percent) {
        if (instance != null) {
            instance.rangeBoostPercent = Math.max(50, Math.min(1000, percent)); // 50% to 1000%
            instance.setDirty();
        }
    }
}
