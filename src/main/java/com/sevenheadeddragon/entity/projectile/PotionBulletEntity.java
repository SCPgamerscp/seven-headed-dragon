package com.sevenheadeddragon.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ポーション弾 (Potion Bullet)
 * <p>
 * The projectile fired/rained by the Potion Master's magic circles during
 * its bullet-hell attack turns. Follows a gravity-affected parabolic arc
 * just like a vanilla splash potion, and applies a randomly selected effect
 * (drawn from BOTH the mod's 14 custom potion effects AND a pool of vanilla
 * debuff effects) on impact.
 * <p>
 * NOTE: This class intentionally does NOT extend vanilla's ThrownPotion so
 * that its visuals/behavior can be fully custom-tailored to the boss's
 * bullet-hell patterns (e.g. skipping the vanilla "potion break" splash
 * radius logic in favor of a single-target/AoE hit chosen by the attack
 * pattern). See {@link com.sevenheadeddragon.util.PotionEffectPool} for the
 * effect selection pool (custom + vanilla debuffs).
 * <p>
 * TODO: attach a client renderer that reuses the vanilla splash potion item
 * render (tinted per selected effect color) and hook impact into
 * {@code com.sevenheadeddragon.util.PotionEffectPool} for the actual random
 * custom+vanilla effect roll.
 */
public class PotionBulletEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(PotionBulletEntity.class, EntityDataSerializers.INT);

    @Nullable
    private LivingEntity owner;

    public PotionBulletEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_COLOR, 0xFFAA00FF);
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
    }

    public void setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    /**
     * Launches this bullet toward a target position with a parabolic
     * (gravity-affected) arc, matching vanilla splash potion throw physics.
     */
    public void shootToward(double targetX, double targetY, double targetZ, float velocityPower) {
        double dx = targetX - this.getX();
        double dy = targetY - this.getY();
        double dz = targetZ - this.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        this.setDeltaMovement(dx, dy + horizontalDist * 0.2D, dz);
        this.setDeltaMovement(this.getDeltaMovement().scale(velocityPower / this.getDeltaMovement().length()));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        // Gravity, matching vanilla thrown-potion-like arc.
        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.05D, 0));
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.99D));

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EFFECT,
                    this.getX(), this.getY(), this.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
        }

        if (this.tickCount > 200 || this.onGround() || checkImpact()) {
            onHit();
            this.discard();
        }
    }

    private boolean checkImpact() {
        List<Entity> hits = this.level().getEntities(this, this.getBoundingBox().inflate(0.3D),
                e -> e instanceof LivingEntity && e != owner);
        return !hits.isEmpty();
    }

    private void onHit() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        List<Entity> hits = this.level().getEntities(this, this.getBoundingBox().inflate(1.5D),
                e -> e instanceof LivingEntity && e != owner);

        for (Entity e : hits) {
            if (e instanceof LivingEntity living) {
                com.sevenheadeddragon.util.PotionEffectPool.applyRandomEffect(living, this.random);
            }
        }

        serverLevel.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(),
                20, 0.3, 0.1, 0.3, 0.1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
