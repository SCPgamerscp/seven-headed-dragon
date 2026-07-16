package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.util.EffectUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.RandomSource;

/**
 * テレポート (Teleport)
 * Every 5 seconds, teleports the target 20 blocks straight up into the air
 * (along with a small random horizontal offset).
 * <p>
 * This effect intentionally skips safety checks (so it can teleport the player
 * into a ceiling if underground) to maximize lethality.
 */
public class TeleportMarkEffect extends MobEffect {

    private static final int TELEPORT_INTERVAL_TICKS = 20 * 5; // trigger every 5s
    private static final double RADIUS = 10.0;

    public TeleportMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B008B);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        RandomSource random = entity.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = random.nextDouble() * RADIUS;
        double dx = Math.cos(angle) * dist;
        double dz = Math.sin(angle) * dist;

        // Teleport 20 blocks up into the air, intentionally ignoring safety checks
        entity.teleportTo(entity.getX() + dx, entity.getY() + 20.0, entity.getZ() + dz);
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TELEPORT_INTERVAL_TICKS == 0;
    }
}
