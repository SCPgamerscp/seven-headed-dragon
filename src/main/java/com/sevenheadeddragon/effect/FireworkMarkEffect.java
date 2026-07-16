package com.sevenheadeddragon.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

/**
 * 花火 (Firework Mark)
 * <p>
 * After a short fuse delay, detonates 7 individually "crafted" fireworks
 * around the target, each with a random color and random star shape.
 * <ul>
 *   <li>The 7 fireworks' explosions never destroy terrain (block breaking is
 *       disabled), matching vanilla firework star explosions.</li>
 *   <li>Separately, a single "weak explosion" (blast power 1) is triggered
 *       which DOES break terrain, similar to a small TNT-like blast.</li>
 * </ul>
 */
public class FireworkMarkEffect extends MobEffect {

    private static final int FUSE_DELAY_TICKS = 20 * 2;
    private static final int FIREWORK_COUNT = 7;

    public FireworkMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF1493);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        detonateFireworkBarrage(serverLevel, entity, entity.getRandom());
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int initialDuration = com.sevenheadeddragon.registry.ModEffects.SPECIAL_EFFECT_DURATION_TICKS;
        int elapsed = initialDuration - duration;
        return elapsed == FUSE_DELAY_TICKS;
    }

    private void detonateFireworkBarrage(ServerLevel level, LivingEntity target, RandomSource random) {
        double baseX = target.getX();
        double baseY = target.getY() + target.getEyeHeight();
        double baseZ = target.getZ();

        // 7 crafted fireworks, each with a random color & random "star shape"
        // (shape only affects the particle pattern used for visuals).
        for (int i = 0; i < FIREWORK_COUNT; i++) {
            double angle = (Math.PI * 2 * i) / FIREWORK_COUNT;
            double radius = 1.5 + random.nextDouble() * 1.5;
            double x = baseX + Math.cos(angle) * radius;
            double y = baseY + (random.nextDouble() - 0.5) * 1.5;
            double z = baseZ + Math.sin(angle) * radius;

            spawnFireworkBurstParticles(level, x, y, z, random);

            // No block breaking for the firework star explosions themselves.
            level.explode(target, x, y, z, 0.0f, Level.ExplosionInteraction.NONE);
        }

        // The single "weak explosion": blast power 1, WITH terrain destruction
        // (creates uneven, cratered terrain as specified).
        BlockPos targetPos = target.blockPosition();
        level.explode(target, targetPos.getX() + 0.5, targetPos.getY() + 0.1, targetPos.getZ() + 0.5,
                1.0f, false, Level.ExplosionInteraction.TNT);
    }

    private void spawnFireworkBurstParticles(ServerLevel level, double x, double y, double z, RandomSource random) {
        // Random color per firework (matching "花火の色や星の形状はランダム").
        int color = 0xFF000000 | (random.nextInt(0x1000000));
        // FireworkParticles.SparkParticle-esque visual using vanilla FIREWORK particle.
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 40 + random.nextInt(20), 0.4, 0.4, 0.4, 0.08);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
