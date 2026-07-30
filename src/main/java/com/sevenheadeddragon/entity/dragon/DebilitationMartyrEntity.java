package com.sevenheadeddragon.entity.dragon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 衰弱の殉教者 (Debilitation Martyr).
 * <p>
 * 固定設置型のエンティティ。召喚位置の地面にバニラの毒雲（{@link AreaEffectCloud}）を
 * 実際に生成し、周囲にウィザー効果を付与する。
 */
public class DebilitationMartyrEntity extends Monster implements GeoEntity {

    /** HP 20. */
    public static final double MAX_HEALTH = 20.0D;

    /** 毒雲の半径 (4ブロック). */
    public static final float CLOUD_RADIUS = 4.0F;

    private static final int CLOUD_REFRESH_INTERVAL = 20;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public DebilitationMartyrEntity(EntityType<? extends DebilitationMartyrEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        // 設置型エンティティのため移動・追尾AIは使用しない
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // 設置型のため押し出し無効
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.tickCount % CLOUD_REFRESH_INTERVAL == 1) {
            spawnVanillaAreaEffectCloud();
        }
    }

    /**
     * 地面にバニラの AreaEffectCloud（毒雲エンティティ）を実際生成する
     */
    private void spawnVanillaAreaEffectCloud() {
        AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setRadiusOnUse(0.0F);
        cloud.setWaitTime(0);
        cloud.setDuration(40); // 2秒持続（毎秒再生成して常に新鮮な雲を保持）
        cloud.setRadiusPerTick(0.0F);
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 4, false, true, true));
        cloud.setParticle(ParticleTypes.ENTITY_EFFECT);
        cloud.setOwner(this);
        this.level().addFreshEntity(cloud);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_DEATH,
                    SoundSource.HOSTILE, 1.0F, 1.1F);
        }
        super.die(damageSource);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    public int getExperienceReward() {
        return 0;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ApocalypseSevenHeadedRedDragonEntity) return false;
        return super.hurt(source, amount);
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<DebilitationMartyrEntity> state) {
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
