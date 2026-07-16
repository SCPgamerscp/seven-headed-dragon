package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 割合毒 (Percent Poison)
 * Deals damage equal to 5% (1/20) of the target's MAX health each tick
 * interval. Unlike vanilla poison, this CAN kill the target (HP can reach 0).
 * Uses a dedicated custom DamageType so deaths show a unique death message.
 */
public class PercentPoisonEffect extends MobEffect {

    /** 5% of MAX HP per tick interval (1/20). */
    private static final float PERCENT_PER_TICK = 0.05f;
    private static final int TICK_INTERVAL = 20; // once per second

    public PercentPoisonEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A2BE2);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        float maxHealth = entity.getMaxHealth();
        float dmg = maxHealth * PERCENT_PER_TICK * (amplifier + 1);
        entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.PERCENT_POISON), dmg);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}
