package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 割合毒 (Percent Poison)
 * Deals damage equal to a percentage of the target's CURRENT health each tick
 * interval. Unlike vanilla poison, this CAN kill the target (HP can reach 0).
 * Uses a dedicated custom DamageType so deaths show a unique death message.
 */
public class PercentPoisonEffect extends MobEffect {

    /** 6% of current HP per tick interval. */
    private static final float PERCENT_PER_TICK = 0.06f;
    private static final int TICK_INTERVAL = 20; // once per second

    public PercentPoisonEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A2BE2);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        float current = entity.getHealth();
        if (current > 0) {
            float dmg = Math.max(0.5f, current * PERCENT_PER_TICK * (amplifier + 1));
            entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.PERCENT_POISON), dmg);
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}
