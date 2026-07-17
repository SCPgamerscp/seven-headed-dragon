package com.sevenheadeddragon.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EvokerFangs;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Invisible, non-solid "conductor" entity that drives the Fang King's
 * geometric EvokerFangs bullet-hell patterns.
 * <p>
 * Rather than giving the custom fangs their own hitbox/damage logic, this
 * entity spawns real vanilla {@link EvokerFangs} at computed positions/times
 * (owned by the Fang King boss), so the damage amount, ground-rise
 * animation, and bite timing are all identical to vanilla's Evoker - only
 * the *arrangement* of where/when the fangs appear is custom.
 * <p>
 * Each spawn is preceded by an "enchant particle" burst a short time
 * beforehand (stage-2 telegraph; stage-1 is the {@link MagicCircleEntity}
 * spawned behind the boss by {@code FangKingAttackPatternManager} at the
 * start of the whole pattern).
 */
public class FangConductorEntity extends Entity {

    public static final int PATTERN_CIRCLE_CONSTRICT = 0;
    public static final int PATTERN_CROSS_ROTATE = 1;
    public static final int PATTERN_SPIRAL_OUTWARD = 2;
    public static final int PATTERN_HOMING_TRACKER = 3;
    public static final int PATTERN_WIDE_RANDOM = 4;

    /** Ticks between an enchant-particle telegraph and the real EvokerFangs spawning at that spot. */
    private static final int ENCHANT_LEAD_TICKS = 10;
    /** Warmup delay passed to each EvokerFangs so its own ground-rise animation still plays after our particle telegraph. */
    private static final int FANG_WARMUP = 12;

    private FangKingEntity boss;
    private LivingEntity target;
    private int patternType;
    private int elapsedTicks;
    private List<Vec3> wideRandomPositions;

    public FangConductorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    /** Must be called immediately after spawning, before {@link net.minecraft.world.level.Level#addFreshEntity}. */
    public void setup(FangKingEntity boss, LivingEntity target, int patternType) {
        this.boss = boss;
        this.target = target;
        this.patternType = patternType;
        this.elapsedTicks = 0;
    }

    @Override
    protected void defineSynchedData() {
        // No client-visible state - this entity is never rendered.
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (boss == null || !boss.isAlive() || target == null || !target.isAlive()) {
            finishPattern();
            return;
        }

        switch (patternType) {
            case PATTERN_CIRCLE_CONSTRICT -> tickCircleConstrict();
            case PATTERN_CROSS_ROTATE -> tickCrossRotate();
            case PATTERN_SPIRAL_OUTWARD -> tickSpiralOutward();
            case PATTERN_HOMING_TRACKER -> tickHomingTracker();
            case PATTERN_WIDE_RANDOM -> tickWideRandom();
            default -> finishPattern();
        }

        elapsedTicks++;
    }

    private void finishPattern() {
        if (boss != null) {
            boss.onPatternFinished();
        }
        this.discard();
    }
