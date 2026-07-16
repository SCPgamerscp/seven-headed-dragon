package com.sevenheadeddragon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 毒ダメージ倍化 (Poison Amplify)
 * Marker effect only. Only functions (multiplies damage) while the target
 * is simultaneously affected by vanilla Poison. Handled centrally in
 * ModCombatEvents#onLivingDamage.
 */
public class PoisonAmplifyEffect extends MobEffect {

    public PoisonAmplifyEffect() {
        super(MobEffectCategory.HARMFUL, 0x228B22);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
