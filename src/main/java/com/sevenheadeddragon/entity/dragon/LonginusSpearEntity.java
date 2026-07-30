package com.sevenheadeddragon.entity.dragon;

import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
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
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * ロンギヌスの槍 (Longinus Spear) - the spear that killed a god, turned on the
 * player.
 * <p>
 * 50+ of these rain straight down from the sky in a 20-block-wide cross (＋)
 * pattern centred on the target, telegraphed a moment earlier by a golden
 * 魔法陣 on the ground. Each spear:
 * <ul>
 *   <li>detonates a <b>power-5 explosion that does not break terrain</b>
 *       when it lands or hits something;</li>
 *   <li>applies the <b>神殺し (God Slaying)</b> debuff to anything it hits
 *       <em>directly</em> - the debuff then eats 10% of the victim's maximum
 *       HP every second (see {@link com.sevenheadeddragon.effect.GodSlayingEffect}).</li>
 * </ul>
 * Only a <em>direct</em> hit inflicts 神殺し; being caught in the blast radius
 * of a neighbouring spear deals explosion damage only. That distinction is
 * what makes the cross pattern's gaps genuinely worth aiming for.
 */
public class LonginusSpearEntity extends Projectile implements GeoEntity {

    /** Explosion power, per spec ("爆発力 5 / 地形破壊なし"). */
    public static final float EXPLOSION_POWER = 5.0F;

    /** Direct-impact damage, before the 神殺し damage-over-time begins. */
    public static final float IMPACT_DAMAGE = 20.0F;

    /** 神殺し duration applied on a direct hit: 10 seconds = 10 ticks of 10% max HP. */
    public static final int GOD_SLAYING_DURATION_TICKS = 20 * 10;

    /** Terminal fall speed in blocks/tick. */
    public static final double FALL_SPEED = 1.6D;

    /** Hard lifetime cap (10 seconds). */
    private static final int MAX_LIFETIME_TICKS = 200;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public LonginusSpearEntity(EntityType<? extends LonginusSpearEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public LonginusSpearEntity(Level level, LivingEntity owner) {
        this(com.sevenheadeddragon.registry.ModEntities.LONGINUS_SPEAR.get(), level);
        this.setOwner(owner);
    }

    /**
     * Drops this spear point-first from {@code (x, y, z)}. The spears always
     * fall vertically: the cross pattern is created by the <em>spawn
     * positions</em>, not by varying trajectories, so the safe gaps stay
     * exactly where the telegraph showed them.
     */
    public void dropFrom(double x, double y, double z) {
        this.setPos(x, y, z);
        this.setDeltaMovement(0.0D, -FALL_SPEED, 0.0D);
        this.setXRot(0.0F); // 垂直（まっすぐ立てて落下させる）
        this.setYRot(0.0F);
        this.xRotO = 0.0F;
        this.yRotO = 0.0F;
    }

    @Override
    protected void defineSynchedData() {
        // Motion-driven only.
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.0, this.getZ(), 0, 0, 0);
            return;
        }

        // Ground / block impact.
        net.minecraft.world.phys.HitResult hit = this.level().clip(new net.minecraft.world.level.ClipContext(
                new Vec3(this.xo, this.yo, this.zo), this.position(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            detonate();
            return;
        }

        // Direct hit - inflate generously on Y because the spear model is a
        // long vertical lance, so its whole shaft should count as a hit.
        for (Entity entity : this.level().getEntities(this,
                this.getBoundingBox().inflate(0.5D, 1.5D, 0.5D), this::canHitEntity)) {
            onHitEntity(new EntityHitResult(entity));
            return;
        }

        if (this.tickCount > MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == this.getOwner()) return false;
        if (entity instanceof GoatMissileEntity || entity instanceof SquidMissileEntity
                || entity instanceof LonginusSpearEntity) return false;
        if (entity instanceof ApocalypseSevenHeadedRedDragonEntity) return false;
        return super.canHitEntity(entity);
    }

    /**
     * A direct spear hit: impact damage, then the 神殺し percentage
     * damage-over-time, then the explosion.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (result.getEntity() instanceof LivingEntity victim) {
            Entity owner = this.getOwner();
            victim.hurt(ModDamageTypes.source(victim, ModDamageTypes.LONGINUS_SPEAR), IMPACT_DAMAGE);
            victim.addEffect(new MobEffectInstance(ModEffects.GOD_SLAYING.get(),
                    GOD_SLAYING_DURATION_TICKS, 0, false, true, true),
                    owner instanceof LivingEntity livingOwner ? livingOwner : null);
        }
        detonate();
    }

    private void detonate() {
        if (this.level().isClientSide) return;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.5, this.getZ(),
                    12, 0.4, 0.6, 0.4, 0.05);
        }
        DragonExplosions.explodeNoGrief(this.level(),
                this.getOwner() != null ? this.getOwner() : this, this.position(), EXPLOSION_POWER);
        this.discard();
    }

    @Override
    public boolean isNoGravity() {
        // Constant terminal velocity instead of accelerating gravity, so the
        // telegraph -> impact timing is identical for every spear in the volley.
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
        return distanceSqr < 160.0D * 160.0D;
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<LonginusSpearEntity> state) {
        // Static model - the provided longinus asset set has no animation file.
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
