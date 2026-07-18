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
    public static final int PATTERN_PINCER = 5;
    public static final int PATTERN_WOBBLY = 6;
    public static final int PATTERN_DNA = 7;

    /** Ticks between an enchant-particle telegraph and the real EvokerFangs spawning at that spot. */
    private static final int ENCHANT_LEAD_TICKS = 10;
    /** Warmup delay passed to each EvokerFangs so its own ground-rise animation still plays after our particle telegraph. */
    private static final int FANG_WARMUP = 12;

    private FangKingEntity boss;
    private LivingEntity target;
    private int patternType;
    private int elapsedTicks;
    private List<Vec3> wideRandomPositions;

    // Captured once at the start of a pattern (elapsedTicks == 0) for patterns whose
    // shape is defined along a fixed axis rather than continuously tracking the target.
    private Vec3 axisOrigin;
    private Vec3 axisDir;
    private double axisLength;

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
            case PATTERN_PINCER -> tickPincer();
            case PATTERN_WOBBLY -> tickWobbly();
            case PATTERN_DNA -> tickDna();
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

    // ------------------------------------------------------------------
    // Pattern 6: Pincer (挟み撃ち) - two 30-block-long walls of fangs,
    // perpendicular to the target's facing axis, closing in on the target
    // simultaneously from the front and the back. 5 layered sub-positions
    // per wave trigger for 5x density.
    // ------------------------------------------------------------------

    private void tickPincer() {
        if (elapsedTicks == 0) {
            axisOrigin = target.position();
            float yawRad = (float) Math.toRadians(target.getYRot());
            axisDir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
        }

        if (elapsedTicks % 4 == 0) {
            double baseDistance = 15.0 - (elapsedTicks / 4) * 0.7; // closes in from 15 blocks down to ~0
            if (baseDistance > 0) {
                Vec3 perp = new Vec3(-axisDir.z, 0, axisDir.x);
                for (int layer = 0; layer < 5; layer++) {
                    double distance = baseDistance - layer * 0.25;
                    if (distance <= 0) continue;
                    for (int side = -1; side <= 1; side += 2) {
                        Vec3 wallCenter = axisOrigin.add(axisDir.scale(side * distance));
                        for (int i = 0; i <= 15; i++) {
                            double t = -15.0 + i * 2.0; // -15..+15 across the wall, 30 blocks total
                            Vec3 pos = wallCenter.add(perp.scale(t));
                            spawnFangTelegraph(pos.x, pos.z);
                        }
                    }
                }
            }
        }

        if (elapsedTicks >= 100) finishPattern();
    }

    // ------------------------------------------------------------------
    // Pattern 7: Wobbly Fang (へにょりファング) - 20 separate wavy fang-lines
    // (30 blocks long each), fanned out across a 45-degree arc, each one
    // individually undulating within a small 10-degree wobble, all growing
    // outward simultaneously. 5 sub-ticks per tick for dense fangs.
    // ------------------------------------------------------------------

    private void tickWobbly() {
        if (elapsedTicks == 0) {
            axisOrigin = boss.position();
            Vec3 fromBoss = target.position().subtract(boss.position());
            axisDir = fromBoss.lengthSqr() > 0.001 ? fromBoss.normalize() : new Vec3(0, 0, 1);
        }

        double baseAngle = Math.atan2(axisDir.x, axisDir.z);

        for (int i = 0; i < 5; i++) {
            double subTick = elapsedTicks + (i / 5.0);
            double dist = subTick * 0.3; // reaches the full 30 blocks around subTick 100
            if (dist > 30.0) continue;

            for (int line = 0; line < 15; line++) {
                double fanOffset = Math.toRadians(-22.5 + line * (45.0 / 14.0)); // 15 lines spread across a 45-degree fan
                // 40 ticks (2 seconds) per cycle: 2 * PI / 40 = 0.157
                double wobble = Math.toRadians(5.0) * Math.sin(subTick * 0.157 + line * 0.4);
                double angle = baseAngle + fanOffset + wobble;
                double fx = axisOrigin.x + Math.sin(angle) * dist;
                double fz = axisOrigin.z + Math.cos(angle) * dist;
                spawnFangTelegraph(fx, fz);
            }
        }

        if (elapsedTicks >= 100) finishPattern();
    }

    // ------------------------------------------------------------------
    // Pattern 8: DNA Fang (DNAファング) - 8 DNA strands shooting out radially
    // in all directions (360 degrees). Each strand has 2 arms twisting
    // around its axis, growing up to 30 blocks long.
    // ------------------------------------------------------------------

    private void tickDna() {
        if (elapsedTicks == 0) {
            axisOrigin = boss.position();
        }

        for (int i = 0; i < 5; i++) {
            double subTick = elapsedTicks + (i / 5.0);
            double dist = subTick * 0.3; // always a fixed 30-block strand
            if (dist > 30.0) continue;

            double twist = Math.toRadians(subTick * 10.0);

            // 8 strands shooting out in 360 degrees
            for (int strand = 0; strand < 8; strand++) {
                double strandAngle = Math.toRadians(strand * 45.0);
                Vec3 dir = new Vec3(Math.cos(strandAngle), 0, Math.sin(strandAngle));
                Vec3 perp = new Vec3(-dir.z, 0, dir.x);

                // 2 arms per strand (DNA double helix)
                for (int arm = 0; arm < 2; arm++) {
                    double armPhase = twist + arm * Math.PI; // opposite sides
                    double offset = Math.sin(armPhase) * 2.5; // 2.5 block width
                    
                    Vec3 pos = axisOrigin.add(dir.scale(dist)).add(perp.scale(offset));
                    spawnFangTelegraph(pos.x, pos.z);
                }
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
