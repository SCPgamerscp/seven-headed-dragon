package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 殺虫 (Insecticide)
 * Only deals damage if the target currently has the "Insectify" effect
 * active OR if the target's MobType is ARTHROPOD (e.g., spiders, silverfish).
 * Otherwise, damage is completely zero.
 */
public class InsecticideEffect extends MobEffect {

    private static final float DAMAGE_PER_TICK = 2.0f;
    private static final int TICK_INTERVAL = 20;

    public InsecticideEffect() {
        super(MobEffectCategory.HARMFUL, 0x9ACD32);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.hasEffect(ModEffects.INSECTIFY.get()) || entity.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD) {
            entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.INSECTICIDE), DAMAGE_PER_TICK * (amplifier + 1));
        }
        // If not insectified: intentionally no damage at all.
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}
