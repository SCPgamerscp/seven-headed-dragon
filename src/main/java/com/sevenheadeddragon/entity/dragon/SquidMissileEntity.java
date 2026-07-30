package com.sevenheadeddragon.entity.dragon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 🦑 Squid Missile (イカミサイル).
 * <p>
 * Where the goat missile is a dumb straight-line machine gun, the squid is the
 * dragon's <b>追尾型ホーミング</b> weapon: fired as a 5-missile volley
 * (一斉発射) that curves through the air chasing its victim, so the player
 * cannot simply sidestep once and be safe. On impact it detonates a
 * <b>power-3 explosion with no terrain damage</b>.
 * <p>
 * Homing is implemented as a per-tick steering blend rather than a hard
 * "always point at the player" snap: the velocity is lerped toward the ideal
 * direction by {@link #TURN_RATE}, which gives the squid a believable arcing
 * flight path and - crucially for playability - a turn radius the player can
 * out-manoeuvre by making sharp direction changes.
 * <p>
 * Uses the provided {@code squid.geo.json} / {@code squid.png} /
 * {@code squid.animation.json} assets, with the tentacle-flapping animation
 * looping the whole time it flies.
 */
public class SquidMissileEntity extends Projectile implements GeoEntity {

    /** Explosion power, per spec ("爆発力 3 / 地形破壊なし"). */
    public static final float EXPLOSION_POWER = 3.0F;

    /** Cruise speed in blocks/tick - slower than the goat, because it homes. */
    public static final double SPEED = 0.62D;

    /**
     * How aggressively the missile re-aims each tick (0 = never turns,
     * 1 = perfect instant tracking). 0.14 gives a wide, dodgeable arc.
     */
    private static final double TURN_RATE = 0.14D;

    /** Homing only begins after this many ticks, so the volley visibly fans out first. */
    private static final int HOMING_DELAY_TICKS = 5;

    /** Hard lifetime cap (15 seconds) so a missile that lost its target is cleaned up. */
    private static final int MAX_LIFETIME_TICKS = 300;

    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID targetUuid;
    @Nullable
    private LivingEntity cachedTarget;

    public SquidMissileEntity(EntityType<? extends SquidMissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public SquidMissileEntity(Level level, LivingEntity owner) {
        this(com.sevenheadeddragon.registry.ModEntities.SQUID_MISSILE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.2D, owner.getZ());
    }

    /** Sets the entity this missile will home in on. */
    public void setHomingTarget(@Nullable LivingEntity target) {
        this.cachedTarget = target;
        this.targetUuid = target == null ? null : target.getUUID();
    }

    @Nullable
    private LivingEntity resolveTarget() {
        if (this.cachedTarget != null && this.cachedTarget.isAlive()) return this.cachedTarget;
        if (this.targetUuid != null && this.level() instanceof ServerLevel serverLevel) {
            Entity found = serverLevel.getEntity(this.targetUuid);
            if (found instanceof LivingEntity living && living.isAlive()) {
                this.cachedTarget = living;
                return living;
            }
        }
        return null;
    }

    /**
     * Launches this missile outward along {@code initialDirection}, which the
     * caller fans across the volley so all 5 squids leave the dragon on
     * distinctly different vectors before curving back in.
     */
    public void launch(Vec3 from, Vec3 initialDirection) {
        this.setPos(from.x, from.y, from.z);
        this.setDeltaMovement(initialDirection.normalize().scale(SPEED));
        updateRotationFromMotion();
    }

    private void updateRotationFromMotion() {
        Vec3 motion = this.getDeltaMovement();
        this.setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        this.setXRot((float) -Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void defineSynchedData() {
        // Motion-driven only; no extra synched data needed.
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.tickCount >= HOMING_DELAY_TICKS) {
            steerTowardTarget();
        }

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.BUBBLE_POP, this.getX(), this.getY() + 0.5, this.getZ(), 0, 0, 0);
            return;
        }

        updateRotationFromMotion();

        net.minecraft.world.phys.HitResult hit = this.level().clip(new net.minecraft.world.level.ClipContext(
                new Vec3(this.xo, this.yo, this.zo), this.position(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            detonate();
            return;
        }

        for (Entity entity : this.level().getEntities(this,
                this.getBoundingBox().inflate(0.5D), this::canHitEntity)) {
            onHitEntity(new EntityHitResult(entity));
            return;
        }

        if (this.tickCount > MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    /**
     * Blends the current velocity toward the direction of the target, keeping
     * the magnitude fixed at {@link #SPEED} so homing changes heading without
     * ever accelerating the missile into something undodgeable.
     */
    private void steerTowardTarget() {
        LivingEntity target = resolveTarget();
        if (target == null) return;

        Vec3 desired = new Vec3(
                target.getX() - this.getX(),
                target.getY(0.5D) - this.getY(),
                target.getZ() - this.getZ()).normalize();

        Vec3 current = this.getDeltaMovement().normalize();
        Vec3 blended = current.scale(1.0D - TURN_RATE).add(desired.scale(TURN_RATE));
        if (blended.lengthSqr() < 1.0E-6D) {
            blended = desired;
        }
        this.setDeltaMovement(blended.normalize().scale(SPEED));
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == this.getOwner()) return false;
        if (entity instanceof GoatMissileEntity || entity instanceof SquidMissileEntity
                || entity instanceof LonginusSpearEntity) return false;
        if (entity instanceof ApocalypseSevenHeadedRedDragonEntity) return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        detonate();
    }

    private void detonate() {
        if (this.level().isClientSide) return;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        DragonExplosions.explodeNoGrief(this.level(),
                this.getOwner() != null ? this.getOwner() : this, this.position(), EXPLOSION_POWER);
        this.discard();
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("HomingTarget")) {
            this.targetUuid = tag.getUUID("HomingTarget");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetUuid != null) {
            tag.putUUID("HomingTarget", this.targetUuid);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0D * 128.0D;
    }

    // ------------------------------------------------------------------
    // GeckoLib - loops the provided squid tentacle animation while in flight
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<SquidMissileEntity> state) {
        state.getController().setAnimation(SWIM);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
