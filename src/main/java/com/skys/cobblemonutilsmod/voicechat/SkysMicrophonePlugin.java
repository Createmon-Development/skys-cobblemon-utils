package com.skys.cobblemonutilsmod.voicechat;

import com.skys.cobblemonutilsmod.SkysCobblemonUtils;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoiceDistanceEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class SkysMicrophonePlugin implements VoicechatPlugin {

    private static VoicechatServerApi serverApi;
    private static VoicechatApi voicechatApi;

    // Cache encoders/decoders per player to avoid recreation overhead
    private static final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private static final Map<UUID, OpusEncoder> encoders = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return SkysCobblemonUtils.MOD_ID + "_microphone";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
        SkysCobblemonUtils.LOGGER.info("Simple Voice Chat Microphone Plugin initialized!");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(VoiceDistanceEvent.class, this::onVoiceDistance);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
        SkysCobblemonUtils.LOGGER.info("Voice chat server started - Microphone plugin ready");
    }

    /**
     * Handles microphone packets to boost volume when player is using the microphone item.
     */
    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (voicechatApi == null) return;

        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null) return;

        // Get the player from the connection
        if (!(senderConnection.getPlayer().getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        // Check if player is holding an enabled microphone
        if (!MicrophoneItem.isPlayerUsingMicrophone(player)) {
            return;
        }

        // Get the volume multiplier from config
        float volumeMultiplier = MicrophoneConfig.getVolumeMultiplier();
        if (volumeMultiplier <= 1.0f) {
            return; // No boost needed
        }

        try {
            // Get or create decoder/encoder for this player
            // Important: Opus is a stateful codec - do NOT reset state between packets
            // The codec maintains internal state for optimal audio quality across the stream
            UUID playerId = player.getUUID();

            OpusDecoder decoder = decoders.computeIfAbsent(playerId, id -> voicechatApi.createDecoder());
            OpusEncoder encoder = encoders.computeIfAbsent(playerId, id -> voicechatApi.createEncoder());

            // Check if codecs are still valid
            if (decoder.isClosed() || encoder.isClosed()) {
                // Recreate if closed
                if (decoder.isClosed()) {
                    decoder = voicechatApi.createDecoder();
                    decoders.put(playerId, decoder);
                }
                if (encoder.isClosed()) {
                    encoder = voicechatApi.createEncoder();
                    encoders.put(playerId, encoder);
                }
            }

            // Get the opus encoded audio data
            byte[] opusData = event.getPacket().getOpusEncodedData();
            if (opusData == null || opusData.length == 0) {
                return;
            }

            // Decode to raw PCM audio (16-bit samples)
            short[] pcmData = decoder.decode(opusData);
            if (pcmData == null || pcmData.length == 0) {
                return;
            }

            // Amplify the audio samples
            amplifyAudio(pcmData, volumeMultiplier);

            // Re-encode to opus
            byte[] amplifiedOpusData = encoder.encode(pcmData);
            if (amplifiedOpusData != null && amplifiedOpusData.length > 0) {
                // Replace the packet data with the amplified version
                event.getPacket().setOpusEncodedData(amplifiedOpusData);
            }

        } catch (Exception e) {
            SkysCobblemonUtils.LOGGER.warn("Error processing microphone audio for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Amplifies audio samples by the given multiplier with hard clipping.
     */
    private void amplifyAudio(short[] samples, float multiplier) {
        for (int i = 0; i < samples.length; i++) {
            // Amplify the sample
            int amplified = (int) (samples[i] * multiplier);

            // Hard clipping to prevent overflow
            if (amplified > Short.MAX_VALUE) {
                amplified = Short.MAX_VALUE;
            } else if (amplified < Short.MIN_VALUE) {
                amplified = Short.MIN_VALUE;
            }

            samples[i] = (short) amplified;
        }
    }

    /**
     * Handles voice distance events to extend range when player is using the microphone item.
     */
    private void onVoiceDistance(VoiceDistanceEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        if (senderConnection == null) return;

        // Get the player from the connection
        if (!(senderConnection.getPlayer().getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        // Check if player is holding an enabled microphone
        if (!MicrophoneItem.isPlayerUsingMicrophone(player)) {
            return;
        }

        // Get the range multiplier from config
        float rangeMultiplier = MicrophoneConfig.getRangeMultiplier();
        if (rangeMultiplier <= 1.0f) {
            return; // No boost needed
        }

        // Extend the voice distance
        float currentDistance = event.getDistance();
        float newDistance = currentDistance * rangeMultiplier;
        event.setDistance(newDistance);
    }

    /**
     * Cleanup method to release encoder/decoder resources when a player disconnects.
     */
    public static void cleanupPlayer(UUID playerId) {
        OpusDecoder decoder = decoders.remove(playerId);
        if (decoder != null && !decoder.isClosed()) {
            decoder.close();
        }

        OpusEncoder encoder = encoders.remove(playerId);
        if (encoder != null && !encoder.isClosed()) {
            encoder.close();
        }
    }

    /**
     * Cleanup all resources when the server stops.
     */
    public static void cleanup() {
        decoders.values().forEach(decoder -> {
            if (!decoder.isClosed()) decoder.close();
        });
        decoders.clear();

        encoders.values().forEach(encoder -> {
            if (!encoder.isClosed()) encoder.close();
        });
        encoders.clear();

        serverApi = null;
        voicechatApi = null;
    }
}
