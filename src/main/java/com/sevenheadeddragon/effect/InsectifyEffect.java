package com.sevenheadeddragon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 昆虫化 (Insectify)
 * Purely an internal status marker - the target's appearance and hitbox are
 * NOT changed. Its only purpose is to be checked by the "Insecticide" effect
 * to determine whether that effect deals damage. Also, while active, this
 * effect grants NO benefit against cobweb slowdown (spiders normally ignore
 * cobwebs, but that vanilla behavior is intentionally NOT granted here).
 */
public class InsectifyEffect extends MobEffect {

    public InsectifyEffect() {
        super(MobEffectCategory.HARMFUL, 0x556B2F);
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
