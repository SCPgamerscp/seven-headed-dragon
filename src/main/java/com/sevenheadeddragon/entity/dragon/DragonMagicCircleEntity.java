package com.sevenheadeddragon.entity.dragon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 魔法陣 (Magic Circle) telegraph entity for the Red Dragon, with per-instance
 * <em>colour</em> and <em>size</em>.
 * <p>
 * The Potion Master's {@code MagicCircleEntity} is a fixed-size white circle;
 * the Red Dragon needs two different variants driven from the same class:
 * <ul>
 *   <li><b>ロンギヌスの槍</b>: one large golden circle centred on the target,
 *       marking the 20-block cross-shaped spear drop zone.</li>
 *   <li><b>七色の雷</b>: 35+ small circles, each tinted the exact colour of
 *       the lightning bolt that will strike it 0.5 seconds later - this is
 *       the "魔法陣事前警告" that makes the Shocker Breaker dodgeable.</li>
 * </ul>
 * It is a pure visual: no collision, no damage, no persistence. It discards
 * itself once its lifetime runs out so a 35-circle barrage cannot leak
 * entities if the fight ends mid-pattern.
 */
public class DragonMagicCircleEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(DragonMagicCircleEntity.class, EntityDataSerializers.INT);

    /** Packed 0xRRGGBB tint applied to the circle texture. */
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(DragonMagicCircleEntity.class, EntityDataSerializers.INT);

    /** Circle diameter in blocks. */
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(DragonMagicCircleEntity.class, EntityDataSerializers.FLOAT);

    /** Degrees per tick the circle spins while visible. */
    private static final EntityDataAccessor<Float> DATA_SPIN =
            SynchedEntityData.defineId(DragonMagicCircleEntity.class, EntityDataSerializers.FLOAT);

    private static final int DEFAULT_LIFETIME = 10;

    public DragonMagicCircleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFETIME, DEFAULT_LIFETIME);
        this.entityData.define(DATA_COLOR, 0xFFFFFF);
        this.entityData.define(DATA_SIZE, 3.0F);
        this.entityData.define(DATA_SPIN, 4.0F);
    }

    public void setLifetime(int ticks) {
        this.entityData.set(DATA_LIFETIME, ticks);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setColor(int rgb) {
        this.entityData.set(DATA_COLOR, rgb);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setSize(float diameterBlocks) {
        this.entityData.set(DATA_SIZE, diameterBlocks);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSpinDegreesPerTick(float spin) {
        this.entityData.set(DATA_SPIN, spin);
    }

    public float getSpinDegreesPerTick() {
        return this.entityData.get(DATA_SPIN);
    }

    /** Convenience factory used by the attack patterns. */
    public static DragonMagicCircleEntity spawn(net.minecraft.server.level.ServerLevel level,
                                                double x, double y, double z,
                                                int rgb, float size, int lifetimeTicks) {
        DragonMagicCircleEntity circle =
                com.sevenheadeddragon.registry.ModEntities.DRAGON_MAGIC_CIRCLE.get().create(level);
        if (circle == null) return null;
        circle.moveTo(x, y, z, 0.0F, 0.0F);
        circle.setColor(rgb);
        circle.setSize(size);
        circle.setLifetime(lifetimeTicks);
        level.addFreshEntity(circle);
        return circle;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        int remaining = getLifetime() - 1;
        if (remaining <= 0) {
            this.discard();
        } else {
            this.entityData.set(DATA_LIFETIME, remaining);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Purely transient telegraph - intentionally not persisted.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Purely transient telegraph - intentionally not persisted.
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0D * 128.0D;
    }
}
