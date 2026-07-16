package com.sevenheadeddragon.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
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
