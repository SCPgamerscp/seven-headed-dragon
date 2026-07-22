package com.sevenheadeddragon.client;

import net.minecraft.util.RandomSource;

/**
 * Client-side holder for the Centipede boss's "screen shake" walking AoE
 * effect. Populated by {@link com.sevenheadeddragon.network.ScreenShakePacket},
 * consumed by {@link ClientScreenShakeHandler} via
 * {@code ViewportEvent.ComputeCameraAngles}.
 */
public final class ScreenShakeManager {

    private static final RandomSource RANDOM = RandomSource.create();

    private static int ticksRemaining = 0;
    private static int totalDuration = 1;
    private static float intensityDegrees = 0.0f;

    /** Starts (or refreshes, if stronger) a camera shake of the given intensity (max degrees of jitter) and duration. */
    public static void trigger(float intensityDegrees, int durationTicks) {
        if (durationTicks <= 0) return;
        if (ticksRemaining <= 0 || intensityDegrees >= ScreenShakeManager.intensityDegrees) {
            ScreenShakeManager.intensityDegrees = intensityDegrees;
            ScreenShakeManager.totalDuration = durationTicks;
            ticksRemaining = durationTicks;
        }
    }

    /** Called once per client tick to decay the shake over time. */
    public static void tickDown() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    /** A small random yaw offset (degrees), scaled down as the shake decays. */
    public static float getYawOffset() {
        if (ticksRemaining <= 0) return 0.0f;
        float decay = ticksRemaining / (float) totalDuration;
        return (RANDOM.nextFloat() - 0.5f) * 2.0f * intensityDegrees * decay;
    }

    /** A small random pitch offset (degrees), scaled down as the shake decays. */
    public static float getPitchOffset() {
        if (ticksRemaining <= 0) return 0.0f;
        float decay = ticksRemaining / (float) totalDuration;
        return (RANDOM.nextFloat() - 0.5f) * 2.0f * intensityDegrees * decay;
    }

    private ScreenShakeManager() {}
}
