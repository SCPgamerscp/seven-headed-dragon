package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * マグマダメージ (Magma Damage)
 * While active, deals damage each tick equivalent to standing in lava
 * (fire damage type, 4 damage every 0.5 seconds), regardless
 * of the target's actual surroundings.
 */
public class MagmaEffect extends MobEffect {

    private static final float DAMAGE_PER_TICK = 4.0f; // matches vanilla lava damage
    private static final int TICK_INTERVAL = 10; // 0.5 seconds

    public MagmaEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF8C00);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.MAGMA), DAMAGE_PER_TICK * (amplifier + 1));
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}
