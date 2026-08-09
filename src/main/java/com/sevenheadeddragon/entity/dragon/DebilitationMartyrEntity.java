package com.sevenheadeddragon.entity.dragon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * 衰弱の殉教者 (Debilitation Martyr).
 * <p>
 * 非モブ・固定設置型（トーテム）エンティティ。
 * プレイヤー攻撃により撃破可能（HP 20）で、召喚位置にウィザー効果の毒雲を常時発生させる。
 */
public class DebilitationMartyrEntity extends Entity implements GeoEntity, net.minecraft.world.entity.OwnableEntity {

    /** MAX HP 20.0F. */
    public static final float MAX_HEALTH = 20.0F;

    /** 毒雲の半径 (4ブロック). */
    public static final float CLOUD_RADIUS = 4.0F;

    private static final int CLOUD_REFRESH_INTERVAL = 20;

    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(DebilitationMartyrEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private LivingEntity owner;

    public DebilitationMartyrEntity(EntityType<? extends DebilitationMartyrEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }

    @Override
    @Nullable
    public java.util.UUID getOwnerUUID() {
        return this.owner != null ? this.owner.getUUID() : null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEALTH, MAX_HEALTH);
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH, Math.max(0.0F, health));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // 設置型のため押し出し無効
    }

    @Override
    public void tick() {
        super.tick();
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
        if (this.getOwner() != null) {
            cloud.setOwner(this.getOwner());
        }
        this.level().addFreshEntity(cloud);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;
        if (source.getEntity() instanceof ApocalypseSevenHeadedRedDragonEntity) return false;
        if (source.is(DamageTypes.WITHER)) return false;

        float nextHealth = getHealth() - amount;
        setHealth(nextHealth);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_HURT,
                    SoundSource.HOSTILE, 1.0F, 1.1F);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.2D, getZ(),
                    (int) Math.max(1, amount), 0.3D, 0.5D, 0.3D, 0.1D);
        }

        if (nextHealth <= 0.0F) {
            destroy();
        }

        return true;
    }

    private void destroy() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_DEATH,
                    SoundSource.HOSTILE, 1.2F, 0.9F);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(),
                    15, 0.4D, 0.6D, 0.4D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.0D, getZ(),
                    10, 0.3D, 0.5D, 0.3D, 0.05D);
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Health")) {
            setHealth(tag.getFloat("Health"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", getHealth());
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
