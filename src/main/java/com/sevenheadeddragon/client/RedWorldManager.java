package com.sevenheadeddragon.client;

import net.minecraft.util.Mth;

/**
 * Client-side holder for the "Red World" ({@code ワールド全体が赤く染まる}) state,
 * driven by {@link com.sevenheadeddragon.network.RedWorldPacket} and consumed by
 * {@link ClientRedWorldHandler}.
 *
 * <p>The transition is smoothed over {@link #FADE_TICKS} so the sky does not pop
 * from blue to red (and back) in a single frame.</p>
 */
public final class RedWorldManager {

    /** How long (in ticks) the fade in / fade out of the red tint takes. */
    public static final int FADE_TICKS = 40;

    /** Target fog / sky colour while the dragon lives. */
    public static final float RED_R = 0.55f;
    public static final float RED_G = 0.03f;
    public static final float RED_B = 0.05f;

    private static boolean active = false;
    private static float strength = 0.0f;
    private static float previousStrength = 0.0f;

    /** Turns the effect on/off (packet driven). */
    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }

    /** Advances the fade; called once per client tick. */
    public static void tick() {
        previousStrength = strength;
        float step = 1.0f / FADE_TICKS;
        if (active) {
            strength = Math.min(1.0f, strength + step);
        } else {
            strength = Math.max(0.0f, strength - step);
        }
    }

    /** Interpolated 0..1 strength of the red tint for the current frame. */
    public static float getStrength(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0f, 1.0f), previousStrength, strength);
    }

    /** True while there is anything at all to render (active or still fading out). */
    public static boolean hasEffect(float partialTick) {
        return getStrength(partialTick) > 0.001f;
    }

    /** Hard reset - used when disconnecting so the tint does not leak between worlds. */
    public static void reset() {
        active = false;
        strength = 0.0f;
        previousStrength = 0.0f;
    }

    private RedWorldManager() {}
}
