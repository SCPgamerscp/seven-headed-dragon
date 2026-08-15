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
 * Individual hitbox part of the Worm Dragon (地中の竜王) for whole-body and
 * animation-synced combat hit detection, mirroring vanilla's EnderDragonPart.
 */
public class WormDragonPart extends PartEntity<WormDragonEntity> {

    public final WormDragonEntity parentMob;
    public final String partName;
    private final EntityDimensions size;

    public WormDragonPart(WormDragonEntity parent, String partName, float width, float height) {
        super(parent);
        this.parentMob = parent;
        this.partName = partName;
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
        // Purely a positioned hitbox.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Recreated on spawn by parent.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Recreated on spawn by parent.
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void setSecondsOnFire(int seconds) {
        this.parentMob.setSecondsOnFire(seconds);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        return this.parentMob.hurt(source, amount);
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
