package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 落下ダメージ倍化 (Fall Damage Amplify)
 * <p>
 * Behavior:
 * 1. Actual fall damage taken by the target is multiplied (the fall SPEED
 *    itself is not changed - this is a pure damage multiplier, handled
 *    centrally in ModCombatEvents#onLivingDamage by checking for the FALL
 *    damage source while this effect is active).
 * 2. Additionally, while active, the target periodically takes "void"
 *    (奈落) damage that bypasses armor entirely and cannot be reduced by any
 *    item/enchantment - matching vanilla out-of-world damage rules.
 *    NOTE: per design adjustment, this void-damage component only ticks once
 *    every 5 seconds (100 ticks) rather than every tick, since vanilla void
 *    damage ticking every tick would otherwise kill the target almost
 *    instantly.
 */
public class FallDamageAmplifyEffect extends MobEffect {

    /** Multiplier applied to actual fall damage while this effect is active. */
    public static final float FALL_DAMAGE_MULTIPLIER = 3.0f;

    /** Void-like damage dealt periodically, bypasses armor completely. */
    private static final float VOID_DAMAGE_AMOUNT = 4.0f;
    /** Tick interval for the void damage component: 5 seconds. */
    private static final int VOID_DAMAGE_INTERVAL_TICKS = 20 * 5;

    public FallDamageAmplifyEffect() {
        super(MobEffectCategory.HARMFUL, 0x704214);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Void damage bypasses armor: uses a dedicated custom DamageType
        // (tagged #minecraft:bypasses_armor / #minecraft:bypasses_invulnerability,
        // matching vanilla out-of-world damage rules) so it can never be
        // reduced by armor/enchantments/absorption, and shows its own death
        // message on kill.
        entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.FALL_AMPLIFY_VOID), VOID_DAMAGE_AMOUNT * (amplifier + 1));
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % VOID_DAMAGE_INTERVAL_TICKS == 0;
    }
}
