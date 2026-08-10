package com.sevenheadeddragon.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 魔法陣 (Magic Circle) telegraph entity.
 * <p>
 * A flat, transparent-textured plane model used both when the Potion
 * Master boss is summoned and whenever it telegraphs an incoming bullet-hell
 * attack (giving the player time to react/dodge before potions are
 * launched/rained down).
 * <p>
 * TODO: wire up a client renderer (flat quad using a transparent PNG
 * texture) and hook this entity into the boss's attack pattern logic so
 * attacks only fire once the telegraph has finished (see project TODO).
 */
public class MagicCircleEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME_TICKS =
            SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.INT);

    /** Pitch rotation in degrees (0 = flat/horizontal, 90 = vertical). */
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.FLOAT);

    /** Yaw rotation in degrees (rotation around the Y axis). */
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.FLOAT);

    /** Default telegraph duration before the circle disappears (in ticks). */
    private static final int DEFAULT_LIFETIME = 40;

    private LivingEntity ownerToTrack = null;
    private LivingEntity targetToFace = null;

    public MagicCircleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME_TICKS, DEFAULT_LIFETIME);
        this.entityData.define(DATA_PITCH, 0.0f);
        this.entityData.define(DATA_YAW, 0.0f);
    }

    public void setLifetime(int ticks) {
        this.entityData.set(DATA_LIFETIME_TICKS, ticks);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME_TICKS);
    }

    public void setOrientationPitch(float pitch) {
        this.entityData.set(DATA_PITCH, pitch);
    }

    public float getOrientationPitch() {
        return this.entityData.get(DATA_PITCH);
    }

    public void setOrientationYaw(float yaw) {
        this.entityData.set(DATA_YAW, yaw);
    }

    public float getOrientationYaw() {
        return this.entityData.get(DATA_YAW);
    }

    public void startTracking(LivingEntity target) {
        this.ownerToTrack = target;
        this.targetToFace = target;
    }

    public void setOwnerAndTarget(LivingEntity owner, LivingEntity target) {
        this.ownerToTrack = owner;
        this.targetToFace = target;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (ownerToTrack != null && ownerToTrack.isAlive()) {
                double yawRad = Math.toRadians(ownerToTrack.getYRot());
                double posX = ownerToTrack.getX() - Math.sin(yawRad) * 1.5;
                double posZ = ownerToTrack.getZ() + Math.cos(yawRad) * 1.5;
                this.setPos(posX, ownerToTrack.getY() + 1.8, posZ);
            }

            if (targetToFace != null && targetToFace.isAlive()) {
                double dx = targetToFace.getX() - this.getX();
                double dy = (targetToFace.getY() + targetToFace.getEyeHeight()) - this.getY();
                double dz = targetToFace.getZ() - this.getZ();

                float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                float pitch = (float)(-(Math.atan2(dy, horizontalDist) * (180.0 / Math.PI))) + 90.0F;

                this.setOrientationYaw(-yaw);
                this.setOrientationPitch(pitch);
            }
            
            int remaining = getLifetime() - 1;
            if (remaining <= 0) {
                this.discard();
            } else {
                this.entityData.set(DATA_LIFETIME_TICKS, remaining);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // No persistent state - purely a transient visual telegraph entity.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // No persistent state - purely a transient visual telegraph entity.
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
