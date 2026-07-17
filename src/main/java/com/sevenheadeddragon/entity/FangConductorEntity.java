package com.sevenheadeddragon.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
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

    private void spawnFangTelegraph(double x, double z) {
        if (!(this.level() instanceof ServerLevel serverLevel) || boss == null) return;
        
        double y = this.getY();
        
        // Stage 2 telegraph: Enchant particles exactly where the fang will appear
        serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y + 0.1, z, 15, 0.3, 0.0, 0.3, 0.05);

        // Schedule the actual fang spawn 10 ticks later (0.5s)
        boss.scheduleIn(ENCHANT_LEAD_TICKS, () -> {
            if (!boss.isAlive()) return;
            EvokerFangs fangs = new EvokerFangs(serverLevel, x, y, z, 0.0f, FANG_WARMUP, boss);
            serverLevel.addFreshEntity(fangs);
        });
    }

    private void tickCircleConstrict() {
        // Every 4 ticks (0.2 seconds), spawn a shrinking circle around the boss
        // 25 steps total (100 ticks / 4). Radius goes from 30 down to 0.
        if (elapsedTicks % 4 == 0) {
            int step = elapsedTicks / 4;
            double radius = 30.0 - (step * 1.2); // Shrinks 1.2 blocks per step
            
            if (radius > 0) {
                int count = Math.max(16, (int)(radius * 2.5)); // Dynamic count to form a solid wall
                
                double cx = boss.getX();
                double cz = boss.getZ();
                
                for (int i = 0; i < count; i++) {
                    double angle = (Math.PI * 2 * i) / count;
                    double fx = cx + Math.cos(angle) * radius;
                    double fz = cz + Math.sin(angle) * radius;
                    spawnFangTelegraph(fx, fz);
                }
            }
        }
        
        if (elapsedTicks >= 100) finishPattern();
    }

    private void tickCrossRotate() {
        // Rotating cross centered on the player's INITIAL position
        if (elapsedTicks % 2 == 0) {
            double cx = this.getX();
            double cz = this.getZ();
            
            // Slower rotation: 5 degrees per 2 ticks
            double angle = Math.toRadians((elapsedTicks / 2) * 5.0);
            
            for (int branch = 0; branch < 4; branch++) {
                double branchAngle = angle + (Math.PI / 2) * branch;
                
                // Spawn a line of fangs from r=1 to r=30 (Attack range 30)
                for (int r = 1; r <= 30; r++) {
                    double fx = cx + Math.cos(branchAngle) * r;
                    double fz = cz + Math.sin(branchAngle) * r;
                    spawnFangTelegraph(fx, fz);
                }
            }
        }
        
        if (elapsedTicks >= 100) finishPattern();
    }

    private void tickSpiralOutward() {
        // Boss centered spiral - dense solid line!
        double cx = boss.getX();
        double cz = boss.getZ();
        
        // We want to simulate high frequency, so we interpolate 5 sub-steps per tick
        for (int i = 0; i < 5; i++) {
            double subTick = elapsedTicks + (i / 5.0);
            
            // 20 degrees per 2 ticks -> 10 degrees per tick
            double angle = Math.toRadians(subTick * 10.0);
            double radius = subTick * 0.3; // max 30 radius at 100 ticks
            
            // Spawn 3 interlocking arms
            for (int s = 0; s < 3; s++) {
                double spiralAngle = angle + (Math.PI * 2 / 3) * s;
                double fx = cx + Math.cos(spiralAngle) * radius;
                double fz = cz + Math.sin(spiralAngle) * radius;
                spawnFangTelegraph(fx, fz);
            }
        }
        
        if (elapsedTicks >= 100) finishPattern();
    }

    private void tickHomingTracker() {
        // Tracks current player position - 1 exact fang every tick!
        if (target != null && target.isAlive()) {
            spawnFangTelegraph(target.getX(), target.getZ());
        }
        
        if (elapsedTicks >= 100) finishPattern();
    }

    private void tickWideRandom() {
        // Random bomb within R=30 of player current pos
        if (target != null && target.isAlive()) {
            double cx = target.getX();
            double cz = target.getZ();
            
            net.minecraft.util.RandomSource rand = this.level().getRandom();
            // 10 fangs per tick over huge 30 block area
            for (int i = 0; i < 10; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(rand.nextDouble()) * 30.0; // uniform distribution in circle
                double fx = cx + Math.cos(angle) * r;
                double fz = cz + Math.sin(angle) * r;
                spawnFangTelegraph(fx, fz);
            }
        }
        
        if (elapsedTicks >= 100) finishPattern();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Purely transient control logic - no persistent state across saves.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Purely transient control logic - no persistent state across saves.
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
