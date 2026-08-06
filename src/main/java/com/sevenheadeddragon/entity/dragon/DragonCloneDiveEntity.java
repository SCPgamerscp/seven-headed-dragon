package com.sevenheadeddragon.entity.dragon;

import com.sevenheadeddragon.client.dragon.DragonAssetModel;
import com.sevenheadeddragon.entity.boss.RedDragonAttackPatternManager;
import com.sevenheadeddragon.network.ModNetworking;
import com.sevenheadeddragon.network.ScreenShakePacket;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * 🐉 ドラゴンの回転急降下分身 (Dragon Clone Dive Entity).
 * <p>
 * Spawns 25 blocks above a target location, telegraphs for 3 seconds (60 ticks)
 * with a 10-block radius circle of Enchantment particles and red magic circle,
 * then dives down doing 360-degree spin animation ("dragonfallattack") and creates
 * a power 10 explosion (NO terrain destruction) on impact!
 */
public class DragonCloneDiveEntity extends Entity implements GeoEntity {

    private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(DragonCloneDiveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(DragonCloneDiveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(DragonCloneDiveEntity.class, EntityDataSerializers.FLOAT);

    private static final RawAnimation ANIM_SPIN_DIVE = RawAnimation.begin().thenLoop("dragonfallattack");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int ageTicks = 0;
    private static final int TELEGRAPH_TICKS = 60; // 3 seconds grace period
    private static final int DIVE_TICKS = 15; // 0.75 seconds spiral dive

    private LivingEntity owner;

    public DragonCloneDiveEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public DragonCloneDiveEntity(Level level, double targetX, double targetY, double targetZ) {
        this(ModEntities.DRAGON_CLONE_DIVE.get(), level);
        setTargetPos((float) targetX, (float) targetY, (float) targetZ);
        setPos(targetX, targetY + 25.0D, targetZ);
    }

    public DragonCloneDiveEntity(Level level, LivingEntity owner, double targetX, double targetY, double targetZ) {
        this(level, targetX, targetY, targetZ);
        this.owner = owner;
    }

    public LivingEntity getOwner() {
        return this.owner;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_X, 0.0F);
        this.entityData.define(TARGET_Y, 0.0F);
        this.entityData.define(TARGET_Z, 0.0F);
    }

    public void setTargetPos(float x, float y, float z) {
        this.entityData.set(TARGET_X, x);
        this.entityData.set(TARGET_Y, y);
        this.entityData.set(TARGET_Z, z);
    }

    public Vec3 getTargetPos() {
        return new Vec3(this.entityData.get(TARGET_X), this.entityData.get(TARGET_Y), this.entityData.get(TARGET_Z));
    }

    @Override
    public void tick() {
        super.tick();
        this.ageTicks++;

        Vec3 target = getTargetPos();
        if (target.x == 0.0D && target.y == 0.0D && target.z == 0.0D) {
            setTargetPos((float) getX(), (float) (getY() - 25.0D), (float) getZ());
            target = getTargetPos();
        }

        double tx = target.x;
        double ty = target.y;
        double tz = target.z;

        if (this.level() instanceof ServerLevel serverLevel) {
            // Spiral Dive Phase (0 to 15 ticks = 0.75 seconds)
            if (this.ageTicks < DIVE_TICKS) {
                double progress = (double) this.ageTicks / (double) DIVE_TICKS;
                double progressSq = progress * progress; // Exponential downward acceleration
                double currentY = ty + 25.0D * (1.0D - progressSq);
                setPos(tx, currentY, tz);

                // Trail particles during descent
                serverLevel.sendParticles(ParticleTypes.CRIT, tx, currentY, tz, 8, 0.5D, 0.5D, 0.5D, 0.1D);
                serverLevel.sendParticles(ParticleTypes.FLAME, tx, currentY, tz, 5, 0.3D, 0.3D, 0.3D, 0.05D);
            }
            // Impact Phase (at tick 15)
            else {
                setPos(tx, ty, tz);

                // Pure Power 10 explosion with NO terrain destruction (deals pure dragon explosion damage & knockback)
                Entity cause = this.owner != null ? this.owner : this;
                serverLevel.explode(cause, tx, ty, tz, 10.0F, false, Level.ExplosionInteraction.NONE);

                // Screen shake for nearby players
                ScreenShakePacket packet = new ScreenShakePacket(4.0F, 12);
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(tx, ty, tz) <= 64.0D * 64.0D) {
                        ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                    }
                }

                this.discard();
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(ANIM_SPIN_DIVE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {}
}
