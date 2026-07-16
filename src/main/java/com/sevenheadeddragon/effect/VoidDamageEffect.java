package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 奈落ダメージ (Void Damage)
 * Deals void damage (bypasses armor and enchantments) periodically.
 */
public class VoidDamageEffect extends MobEffect {

    private static final float VOID_DAMAGE_AMOUNT = 4.0f;
    private static final int VOID_DAMAGE_INTERVAL_TICKS = 20 * 5; // Every 5 seconds

    public VoidDamageEffect() {
        super(MobEffectCategory.HARMFUL, 0x111111);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.VOID_DAMAGE), VOID_DAMAGE_AMOUNT * (amplifier + 1));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % VOID_DAMAGE_INTERVAL_TICKS == 0;
    }
}
