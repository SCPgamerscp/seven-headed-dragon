package com.sevenheadeddragon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 爆発 (Explosion)
 * Every 3 seconds, causes a power-1 explosion at the target's position.
 * This explosion deals damage and destroys terrain.
 */
public class ExplosionEffect extends MobEffect {

    private static final int TICK_INTERVAL = 20 * 3; // 3 seconds

    public ExplosionEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500); // OrangeRed
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            // Power 1 explosion centered on entity (null exploder ensures target receives full explosion damage)
            float power = 1.0F + (amplifier * 0.5F);
            entity.level().explode(null, entity.getX(), entity.getY(), entity.getZ(), power, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}
