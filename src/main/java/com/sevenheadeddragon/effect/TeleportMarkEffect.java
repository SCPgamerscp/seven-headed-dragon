package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * テレポート (Teleport)
 * Every 5 seconds, teleports the target 20 blocks straight up into the air
 * (along with a small random horizontal offset).
 * <p>
 * If there is a block at the target destination, it will teleport the player
 * immediately below it instead.
 */
public class TeleportMarkEffect extends MobEffect {

    private static final int TELEPORT_INTERVAL_TICKS = 20 * 5; // trigger every 5s
    private static final double RADIUS = 10.0;

    public TeleportMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B008B);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        net.minecraft.util.RandomSource random = entity.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = random.nextDouble() * RADIUS;
        double dx = Math.cos(angle) * dist;
        double dz = Math.sin(angle) * dist;

        // Target 20 blocks up into the air
        net.minecraft.core.BlockPos desired = entity.blockPosition().offset((int) dx, 20, (int) dz);
        net.minecraft.core.BlockPos safe = com.sevenheadeddragon.util.EffectUtil.resolveTeleportDestination(serverLevel, desired);

        entity.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TELEPORT_INTERVAL_TICKS == 0;
    }
}
