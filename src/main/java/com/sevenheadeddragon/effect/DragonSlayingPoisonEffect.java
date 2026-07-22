package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * "ドラゴン殺しの毒" (Dragon-Slaying Poison) - the Centipede Boss's bite debuff.
 * Deals 10 magic (armor-piercing) damage once per second, but - like vanilla
 * Poison - will never reduce the victim below 1 HP on its own.
 */
public class DragonSlayingPoisonEffect extends MobEffect {

    private static final int DAMAGE_PER_TICK = 10;

    public DragonSlayingPoisonEffect() {
        super(MobEffectCategory.HARMFUL, 0x2E0854);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() > 1.0F) {
            float newHealth = Math.max(1.0F, entity.getHealth() - 10.0F);
            entity.setHealth(newHealth);
            entity.level().broadcastEntityEvent(entity, (byte) 2);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Every 20 ticks (1 second), regardless of amplifier.
        return duration % 20 == 0;
    }
}
