package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.EffectUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * アンデッド化 (Undead Curse)
 * While active, the target burns in sunlight just like a Zombie/Skeleton
 * (exposed to sky, daytime, not raining, not already on fire/in water).
 * Note: while this effect is active, the boss will not throw its
 * "healing potion" attack (handled in boss attack-selection logic).
 */
public class UndeadCurseEffect extends MobEffect {

    public UndeadCurseEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B3621);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.isAlive() && !entity.isOnFire() && !entity.isInWaterRainOrBubble()
                && EffectUtil.isDaytimeSunny(entity.level())
                && EffectUtil.isExposedToSky(entity.level(), entity.blockPosition())) {
            entity.setSecondsOnFire(8);
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
