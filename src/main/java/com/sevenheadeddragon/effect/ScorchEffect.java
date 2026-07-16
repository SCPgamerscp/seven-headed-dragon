package com.sevenheadeddragon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 炎上 (Scorch)
 * Marker effect only. While active, any fire damage the target takes is
 * multiplied x5 (handled centrally in ModCombatEvents#onLivingDamage).
 * The x5 multiplier can be prevented/reduced by vanilla Fire Resistance
 * potion or the Fire Protection enchantment (see EffectUtil#hasFireProtection).
 */
public class ScorchEffect extends MobEffect {

    public ScorchEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);
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
