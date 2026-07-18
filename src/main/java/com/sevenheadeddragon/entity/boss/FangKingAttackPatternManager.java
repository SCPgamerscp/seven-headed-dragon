package com.sevenheadeddragon.entity.boss;

import com.sevenheadeddragon.entity.FangConductorEntity;
import com.sevenheadeddragon.entity.FangKingEntity;
import com.sevenheadeddragon.entity.MagicCircleEntity;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Vex;

/**
 * Chooses and drives the Fang King's attack patterns: eight geometric
 * EvokerFangs bullet-hell arrangements (delegated to
 * {@link FangConductorEntity}) plus a Vex-summon burst sequence, picked with
 * equal random weight and chained back-to-back with no cooldown for the
 * duration of the boss's 60-second attack turn (see
 * {@code FangKingEntity#onPatternFinished()}).
 */
public final class FangKingAttackPatternManager {

    private static final int PATTERN_COUNT = 9; // 8 fang patterns + 1 Vex summon
    private static final int VEX_SUMMON_PATTERN = 8;

    /** How long the magic-circle telegraph lingers behind the boss before a fang pattern actually begins (stage-1 telegraph). */
    private static final int MAGIC_CIRCLE_TELEGRAPH_TICKS = 15;

    private static final int VEX_BURST_COUNT = 5;
    private static final int VEX_BURST_INTERVAL_TICKS = 100; // 5 seconds, per spec
    private static final int VEX_PER_BURST = 3; // same as vanilla Evoker

    private FangKingAttackPatternManager() {}

    public static void startRandomAttack(FangKingEntity boss) {
        LivingEntity target = boss.getFocusedTarget();
        if (target == null) {
            // Nothing to attack right now - retry next tick once a target is reacquired.
            boss.onPatternFinished();
            return;
        }

        boss.setCastingAnimation(true);

        int choice = boss.getRandom().nextInt(PATTERN_COUNT);
        if (choice == VEX_SUMMON_PATTERN) {
            startVexSummon(boss, target);
        } else {
            startFangPattern(boss, target, choice);
        }
    }

    private static void startFangPattern(FangKingEntity boss, LivingEntity target, int patternType) {
        spawnMagicCircleTelegraph(boss);
        boss.scheduleIn(MAGIC_CIRCLE_TELEGRAPH_TICKS, () -> {
            if (!boss.isAlive() || !target.isAlive()) {
                boss.onPatternFinished();
                return;
            }
            if (!(boss.level() instanceof ServerLevel serverLevel)) return;

            FangConductorEntity conductor = ModEntities.FANG_CONDUCTOR.get().create(serverLevel);
            if (conductor == null) {
                boss.onPatternFinished();
                return;
            }
            conductor.moveTo(target.getX(), target.getY(), target.getZ());
            conductor.setup(boss, target, patternType);
            serverLevel.addFreshEntity(conductor);
        });
    }

    /** Spawns a one-off {@link MagicCircleEntity} behind the boss, facing outward and floating vertically, as the stage-1 "an attack is coming" telegraph. */
    private static void spawnMagicCircleTelegraph(FangKingEntity boss) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;
        MagicCircleEntity circle = ModEntities.MAGIC_CIRCLE.get().create(serverLevel);
        if (circle == null) return;

        circle.setLifetime(MAGIC_CIRCLE_TELEGRAPH_TICKS);
        circle.setOrientationPitch(90.0f); // Make it stand vertically
        circle.startTracking(boss); // Follow the boss dynamically
        
        serverLevel.addFreshEntity(circle);
    }

    // ------------------------------------------------------------------
    // Vex summon "attack" - 5 bursts of 3 Vex each, 5 seconds apart,
    // mirroring vanilla Evoker's own summon spell (owner set so the Vexes
    // automatically copy the Fang King's current target via vanilla's own
    // owner-target-copy AI, and given a limited lifespan so they naturally
    // disappear over time).
    // ------------------------------------------------------------------

    private static void startVexSummon(FangKingEntity boss, LivingEntity target) {
        summonVexBurst(boss, target, 0);
    }

    private static void summonVexBurst(FangKingEntity boss, LivingEntity target, int burstIndex) {
        if (!boss.isAlive()) {
            boss.onPatternFinished();
            return;
        }
        if (boss.level() instanceof ServerLevel serverLevel) {
            spawnVexes(serverLevel, boss);
        }

        int nextBurst = burstIndex + 1;
        if (nextBurst < VEX_BURST_COUNT) {
            boss.scheduleIn(VEX_BURST_INTERVAL_TICKS, () -> summonVexBurst(boss, target, nextBurst));
        } else {
            boss.scheduleIn(1, boss::onPatternFinished);
        }
    }

    private static void spawnVexes(ServerLevel serverLevel, FangKingEntity boss) {
        for (int i = 0; i < VEX_PER_BURST; i++) {
            BlockPos blockPos = boss.blockPosition().offset(-2 + boss.getRandom().nextInt(5), 1, -2 + boss.getRandom().nextInt(5));
            Vex vex = EntityType.VEX.create(serverLevel);
            if (vex == null) continue;
            vex.moveTo(blockPos, 0.0f, 0.0f);
            vex.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPos), MobSpawnType.MOB_SUMMONED, null, null);
            vex.setOwner(boss);
            vex.setBoundOrigin(blockPos);
            vex.setLimitedLife(20 * (30 + boss.getRandom().nextInt(90)));
            serverLevel.addFreshEntityWithPassengers(vex);
        }
    }
}
