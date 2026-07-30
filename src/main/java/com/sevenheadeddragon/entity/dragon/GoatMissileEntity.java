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

/**
 * 🐐 山羊ミサイル (Goat Missile).
 * <p>
 * The dragon's マシンガン (machine-gun) projectile: fired 30 times in rapid
 * succession from the dragon's mouth, flying dead straight at whatever it was
 * aimed at. On any impact - entity or block - it detonates a
 * <b>power-4 explosion that does not break terrain</b> (see
 * {@link DragonExplosions}).
 * <p>
 * Rendered with the provided {@code goat.geo.json} / {@code goat.png}
 * Blockbench model via GeckoLib, spinning end-over-end as it flies so the
 * 30-round barrage reads clearly as a stream of tumbling goats.
 * <p>
 * Because 30 of these can be in flight at once, the entity is deliberately
 * cheap: it self-destructs after {@link #MAX_LIFETIME_TICKS} so a barrage
 * fired at a target that teleported away can never accumulate.
 */
public class GoatMissileEntity extends Projectile implements GeoEntity {

    /** Explosion power, per spec ("爆発力 4 / 地形破壊なし"). */
    public static final float EXPLOSION_POWER = 4.0F;

    /** Flight speed in blocks/tick. */
    public static final double SPEED = 1.4D;

    /** Hard lifetime cap so stray missiles are always cleaned up (10 seconds). */
    private static final int MAX_LIFETIME_TICKS = 200;

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.goat.idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** Client-side visual tumble, advanced every tick. */
    private float tumble;

    public GoatMissileEntity(EntityType<? extends GoatMissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public GoatMissileEntity(Level level, LivingEntity owner) {
        this(com.sevenheadeddragon.registry.ModEntities.GOAT_MISSILE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.2D, owner.getZ());
    }

    /**
     * Aims this missile at {@code target} with a small random spread, giving
     * the 30-round burst its characteristic machine-gun scatter instead of 30
     * projectiles stacked in one perfect line.
     *
     * @param spread maximum angular jitter, in blocks of deviation at the target
     */
    public void aimAt(Vec3 from, Vec3 target, double spread) {
        this.setPos(from.x, from.y, from.z);
        Vec3 dir = target.subtract(from).normalize();
        double jx = (this.random.nextDouble() - 0.5D) * spread;
        double jy = (this.random.nextDouble() - 0.5D) * spread;
        double jz = (this.random.nextDouble() - 0.5D) * spread;
        Vec3 velocity = dir.add(jx, jy, jz).normalize().scale(SPEED);
        this.setDeltaMovement(velocity);
        this.setYRot((float) (Mth_atan2Deg(velocity.x, velocity.z)));
        this.setXRot((float) (-Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance()))));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private static double Mth_atan2Deg(double x, double z) {
        return Math.toDegrees(Math.atan2(x, z));
    }

    @Override
    protected void defineSynchedData() {
        // No extra synched data required - motion is enough to render it.
    }

    @Override
    public void tick() {
        super.tick();

        this.tumble += 27.0F;

        // Straight-line flight: no gravity, no drag, so the "machine gun"
        // stays a flat hitscan-like stream that is readable to dodge.
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            return;
        }

        // Block impact.
        net.minecraft.world.phys.HitResult hit = this.level().clip(new net.minecraft.world.level.ClipContext(
                new Vec3(this.xo, this.yo, this.zo), this.position(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            detonate();
            return;
        }

        // Entity impact - anything except the dragon that fired it.
        for (Entity entity : this.level().getEntities(this,
                this.getBoundingBox().inflate(0.4D), this::canHitEntity)) {
            onHitEntity(new EntityHitResult(entity));
            return;
        }

        if (this.tickCount > MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        // Never blow up on the dragon itself, its own missiles, or its summons.
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

    /** Visual roll used by the renderer. */
    public float getTumble() {
        return this.tumble;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0D * 128.0D;
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<GoatMissileEntity> state) {
        // The provided goat model ships no animation file; the controller is
        // registered anyway so GeckoLib renders the static model correctly
        // and a goat animation can be dropped in later without code changes.
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
