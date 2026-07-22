package com.sevenheadeddragon.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.entity.PartEntity;

/**
 * One segment of the Centipede Boss's whole-body hit detection, following
 * the same design as vanilla's {@code EnderDragonPart} / Ice and Fire's
 * {@code EntityDragonPart}: a lightweight sub-entity with its own hitbox
 * that forwards any damage it receives to the parent boss.
 * <p>
 * Positioned every tick by {@link CentipedeBossEntity} along its own
 * recorded movement-history trail (a "snake body follows the path" model),
 * rather than trying to read GeckoLib's client-side animated bone
 * transforms (which aren't reliably available server-side).
 */
public class CentipedePart extends PartEntity<CentipedeBossEntity> {

    public final CentipedeBossEntity parentMob;
    public final int segmentIndex;
    private final EntityDimensions size;

    public CentipedePart(CentipedeBossEntity parent, int segmentIndex, float width, float height) {
        super(parent);
        this.parentMob = parent;
        this.segmentIndex = segmentIndex;
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    public void setPosAndOld(double x, double y, double z) {
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.xOld = x;
        this.yOld = y;
        this.zOld = z;
        this.setPos(x, y, z);
    }

    public void updatePosWithOld(double newX, double newY, double newZ) {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.setPos(newX, newY, newZ);
    }

    @Override
    protected void defineSynchedData() {
        // No independent state - purely a positioned hitbox.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Not persisted independently - recreated by the parent on spawn.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Not persisted independently - recreated by the parent on spawn.
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.isInvulnerableTo(source) ? false : this.parentMob.hurt(source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || this.parentMob == entity;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
