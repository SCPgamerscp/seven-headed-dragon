package com.sevenheadeddragon.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;

/**
 * 雷 (Lightning Mark)
 * Periodically strikes a lightning bolt directly on the target. Unlike
 * vanilla lightning, this strike pierces roofs/ceilings - it will hit the
 * target even indoors (there is a block overhead).
 */
public class LightningMarkEffect extends MobEffect {

    private static final int STRIKE_INTERVAL_TICKS = 20 * 4; // every 4 seconds

    public LightningMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFF66);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt != null) {
                bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
                bolt.setVisualOnly(false);
                serverLevel.addFreshEntity(bolt);
            }
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % STRIKE_INTERVAL_TICKS == 0;
    }
}
