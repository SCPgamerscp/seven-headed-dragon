package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.EffectUtil;
import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 酸欠 (Asphyxiation)
 * The target's air supply depletes even on dry land, causing drowning
 * damage just like being underwater without air. Can be prevented by
 * Water Breathing potion or wearing a Turtle Shell helmet.
 */
public class AsphyxiationEffect extends MobEffect {

    private static final int TICK_INTERVAL = 1;

    public AsphyxiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x1E90FF);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (EffectUtil.canBreathe(entity)) {
            // Protected: refill air instead of draining it.
            if (entity.getAirSupply() < entity.getMaxAirSupply()) {
                entity.setAirSupply(Math.min(entity.getMaxAirSupply(), entity.getAirSupply() + 4));
            }
            return;
        }

        int air = entity.getAirSupply();
        if (air <= -20) {
            entity.setAirSupply(0);
            entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.ASPHYXIATION), 2.0f);
        } else {
            entity.setAirSupply(air - 1);
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}
