package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 落下ダメージ倍化 (Fall Damage Amplify)
 * <p>
 * Behavior:
 * Actual fall damage taken by the target is multiplied (the fall SPEED
 * itself is not changed - this is a pure damage multiplier, handled
 * centrally in ModCombatEvents#onLivingDamage by checking for the FALL
 * damage source while this effect is active).
 */
public class FallDamageAmplifyEffect extends MobEffect {

    /** Multiplier applied to actual fall damage while this effect is active. */
    public static final float FALL_DAMAGE_MULTIPLIER = 3.0f;

    public FallDamageAmplifyEffect() {
        super(MobEffectCategory.HARMFUL, 0x704214);
    }
}
