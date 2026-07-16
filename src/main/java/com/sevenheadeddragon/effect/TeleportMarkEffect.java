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
 * After a short delay, teleports the target to a random nearby position.
 * If the destination contains a block (including non-transparent/opaque
 * blocks), the target is teleported one block lower instead, repeating
 * downward until a clear spot is found.
 * <p>
 * NOTE: {@code isDurationEffectTick}'s {@code duration} parameter is
 * the REMAINING duration (it counts down from the initial applied duration
 * to 0), so to trigger a fixed delay after the effect was first applied we
 * must compare against (initialDuration - delay), not (delay) itself.
 */
public class TeleportMarkEffect extends MobEffect {

    private static final int TELEPORT_DELAY_TICKS = 20 * 2; // trigger 2s after application
    private static final double RADIUS = 8.0;

    public TeleportMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B008B);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        RandomSource random = entity.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 3.0 + random.nextDouble() * (RADIUS - 3.0);
        double dx = Math.cos(angle) * dist;
        double dz = Math.sin(angle) * dist;

        BlockPos desired = entity.blockPosition().offset((int) dx, 0, (int) dz);
        BlockPos safe = EffectUtil.resolveTeleportDestination(serverLevel, desired);

        entity.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // duration counts DOWN. Trigger once, TELEPORT_DELAY_TICKS after the
        // effect started, as long as the remaining duration is large enough
        // to have that many ticks left when first applied.
        int initialDuration = ModEffects.SPECIAL_EFFECT_DURATION_TICKS;
        int elapsed = initialDuration - duration;
        return elapsed == TELEPORT_DELAY_TICKS;
    }
}
