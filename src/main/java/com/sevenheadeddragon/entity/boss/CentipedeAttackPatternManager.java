package com.sevenheadeddragon.entity.boss;

import com.sevenheadeddragon.entity.CentipedeBossEntity;
import com.sevenheadeddragon.entity.MagicCircleEntity;
import com.sevenheadeddragon.network.ModNetworking;
import com.sevenheadeddragon.network.ScreenShakePacket;
import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.registry.ModEntities;
import com.sevenheadeddragon.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * Chooses and drives the Centipede Boss's three attack patterns: circling
 * walk-damage, poisonous bite, and a raining-potion magic attack. Picked
 * with equal random weight, chained back-to-back for the boss's 60-second
 * attack turn (see {@code CentipedeBossEntity#onPatternFinished()}).
 */
public final class CentipedeAttackPatternManager {

    private static final int PATTERN_COUNT = 3;
    private static final int PATTERN_CIRCLE_WALK = 0;
    private static final int PATTERN_BITE = 1;
    private static final int PATTERN_POISON_RAIN = 2;

    private CentipedeAttackPatternManager() {}

    public static void startRandomAttack(CentipedeBossEntity boss) {
        LivingEntity target = boss.getFocusedTarget();
        if (target == null) {
            boss.onPatternFinished();
            return;
        }

        int choice = boss.getRandom().nextInt(PATTERN_COUNT);
        switch (choice) {
            case PATTERN_CIRCLE_WALK -> startCircleWalk(boss, target);
            case PATTERN_BITE -> startBite(boss, target);
            default -> startPoisonRain(boss, target);
        }
    }

    // ------------------------------------------------------------------
    // Pattern 1: circling walk-damage - orbits the target at 15 blocks,
    // pulsing 5 damage in a 20-block radius (hitting mobs too) every 0.5s,
    // with ground crit particles and a screen-shake packet each pulse.
    // ------------------------------------------------------------------

    private static final int WALK_DAMAGE_DURATION_TICKS = 20 * 12; // ~12 seconds of circling per activation
    private static final int WALK_DAMAGE_INTERVAL_TICKS = 10; // every 0.5s
    private static final double WALK_DAMAGE_RADIUS = 20.0;
    private static final float WALK_DAMAGE_AMOUNT = 5.0F;

    private static void startCircleWalk(CentipedeBossEntity boss, LivingEntity target) {
        boss.setCirclingActive(true);
        tickWalkDamage(boss, target, 0);
    }

    private static void tickWalkDamage(CentipedeBossEntity boss, LivingEntity target, int elapsed) {
        if (!boss.isAlive() || boss.isPlayerTurn()) {
            boss.setCirclingActive(false);
            boss.onPatternFinished();
            return;
        }

        boss.playSound(ModSounds.CENTIPEDE_WALK.get(), 1.0F, 1.0F);
        dealWalkAoeDamage(boss);

        int next = elapsed + WALK_DAMAGE_INTERVAL_TICKS;
        if (next < WALK_DAMAGE_DURATION_TICKS) {
            boss.scheduleIn(WALK_DAMAGE_INTERVAL_TICKS, () -> tickWalkDamage(boss, target, next));
        } else {
            boss.setCirclingActive(false);
            boss.onPatternFinished();
        }
    }

    private static void dealWalkAoeDamage(CentipedeBossEntity boss) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;

        AABB area = boss.getBoundingBox().inflate(WALK_DAMAGE_RADIUS);
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != boss && e.isAlive() && e.distanceTo(boss) <= WALK_DAMAGE_RADIUS);
        for (LivingEntity entity : nearby) {
            entity.hurt(boss.damageSources().mobAttack(boss), WALK_DAMAGE_AMOUNT);
        }

        // Spawn a massive burst of critical particles across the ground in the 20-block AoE radius
        double centerX = boss.getX();
        double centerY = boss.getY() + 0.1;
        double centerZ = boss.getZ();
        for (int i = 0; i < 150; i++) {
            double angle = boss.getRandom().nextDouble() * Math.PI * 2.0;
            double dist = boss.getRandom().nextDouble() * WALK_DAMAGE_RADIUS;
            double px = centerX + Math.cos(angle) * dist;
            double pz = centerZ + Math.sin(angle) * dist;
            serverLevel.sendParticles(ParticleTypes.CRIT, px, centerY, pz, 1, 0.1, 0.05, 0.1, 0.05);
        }

        ScreenShakePacket packet = new ScreenShakePacket(2.0F, 6);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceTo(boss) <= 32.0) {
                ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    // ------------------------------------------------------------------
    // Pattern 2: bite - lunges at the target; on contact deals 20 damage
    // and applies Dragon-Slaying Poison for 10 seconds.
    // ------------------------------------------------------------------

    private static final float BITE_DAMAGE = 20.0F;
    private static final int BITE_POISON_DURATION_TICKS = 20 * 10; // 10 seconds

    private static void startBite(CentipedeBossEntity boss, LivingEntity target) {
        boss.setActionState(CentipedeBossEntity.ACTION_BITING);
        boss.getNavigation().moveTo(target, 1.2);

        boss.scheduleIn(15, () -> {
            if (boss.isAlive() && target.isAlive() && boss.distanceTo(target) < 6.0) {
                target.hurt(boss.damageSources().mobAttack(boss), BITE_DAMAGE);
                target.addEffect(new MobEffectInstance(ModEffects.DRAGON_SLAYING_POISON.get(), BITE_POISON_DURATION_TICKS, 0));
            }
            boss.scheduleIn(10, () -> {
                boss.setActionState(CentipedeBossEntity.ACTION_IDLE_OR_WALK);
                boss.onPatternFinished();
            });
        });
    }

    // ------------------------------------------------------------------
    // Pattern 3: poison rain - a magic circle appears at the target,
    // the boss rears up and casts, and 50 splash potions of Poison V
    // (5 minutes) rain down around the magic circle.
    // ------------------------------------------------------------------

    private static final int RAIN_POTION_COUNT = 50;
    private static final int RAIN_POTION_INTERVAL_TICKS = 3;
    private static final double RAIN_AREA_RADIUS = 8.0;
    private static final double RAIN_HEIGHT = 15.0;
    private static final int POISON_RAIN_DURATION_TICKS = 20 * 60 * 5; // 5 minutes, per spec
    private static final int MAGIC_GETUP_TICKS = 20;
    private static final int MAGIC_GETDOWN_TICKS = 20;

    private static void startPoisonRain(CentipedeBossEntity boss, LivingEntity target) {
        boss.setActionState(CentipedeBossEntity.ACTION_MAGIC_GETUP);
        Vec3 rainOrigin = target.position();
        spawnMagicCircle(boss, rainOrigin);

        boss.scheduleIn(MAGIC_GETUP_TICKS, () -> {
            if (!boss.isAlive()) {
                boss.onPatternFinished();
                return;
            }
            boss.setActionState(CentipedeBossEntity.ACTION_MAGIC_CASTING);
            rainPotions(boss, rainOrigin, 0);
        });
    }

    private static void spawnMagicCircle(CentipedeBossEntity boss, Vec3 rainOrigin) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;
        MagicCircleEntity circle = ModEntities.MAGIC_CIRCLE.get().create(serverLevel);
        if (circle == null) return;
        circle.moveTo(rainOrigin.x, rainOrigin.y, rainOrigin.z, 0.0F, 0.0F);
        circle.setLifetime(MAGIC_GETUP_TICKS + RAIN_POTION_COUNT * RAIN_POTION_INTERVAL_TICKS + MAGIC_GETDOWN_TICKS);
        circle.setOrientationYaw(0.0F);
        circle.setOrientationPitch(90.0F);
        serverLevel.addFreshEntity(circle);
    }

    private static void rainPotions(CentipedeBossEntity boss, Vec3 rainOrigin, int spawned) {
        if (!boss.isAlive()) {
            boss.onPatternFinished();
            return;
        }

        if (spawned < RAIN_POTION_COUNT) {
            spawnRainingPotion(boss, rainOrigin);
            boss.scheduleIn(RAIN_POTION_INTERVAL_TICKS, () -> rainPotions(boss, rainOrigin, spawned + 1));
        } else {
            boss.setActionState(CentipedeBossEntity.ACTION_MAGIC_GETDOWN);
            boss.scheduleIn(MAGIC_GETDOWN_TICKS, () -> {
                boss.setActionState(CentipedeBossEntity.ACTION_IDLE_OR_WALK);
                boss.onPatternFinished();
            });
        }
    }

    private static void spawnRainingPotion(CentipedeBossEntity boss, Vec3 rainOrigin) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;

        double x = rainOrigin.x + (boss.getRandom().nextDouble() - 0.5) * RAIN_AREA_RADIUS * 2.0;
        double z = rainOrigin.z + (boss.getRandom().nextDouble() - 0.5) * RAIN_AREA_RADIUS * 2.0;
        double y = rainOrigin.y + RAIN_HEIGHT;

        ThrownPotion potion = new ThrownPotion(serverLevel, boss);
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setCustomEffects(stack, List.of(new MobEffectInstance(MobEffects.POISON, POISON_RAIN_DURATION_TICKS, 4)));
        potion.setItem(stack);
        potion.setPos(x, y, z);
        potion.setDeltaMovement(0, -0.2, 0);
        serverLevel.addFreshEntity(potion);
    }
}
