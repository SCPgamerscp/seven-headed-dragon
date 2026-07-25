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
    public void setSecondsOnFire(int seconds) {
        this.parentMob.setSecondsOnFire(seconds);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        float finalAmount = amount;
        Entity attacker = source.getEntity();
        if (attacker instanceof net.minecraft.world.entity.LivingEntity livingAttacker) {
            net.minecraft.world.item.ItemStack mainHand = livingAttacker.getMainHandItem();

            // 1. Bane of Arthropods (虫特効) bonus damage & Slowness IV
            int arthropodLvl = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS, mainHand);
            if (arthropodLvl > 0) {
                finalAmount += arthropodLvl * 2.5F;
                int duration = 20 + livingAttacker.getRandom().nextInt(10 * arthropodLvl);
                this.parentMob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 3));
            }

            // 2. Fire Aspect (火属性) ignition
            int fireAspectLvl = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                    net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, mainHand);
            if (fireAspectLvl > 0) {
                this.parentMob.setSecondsOnFire(fireAspectLvl * 4);
            }
        }

        // 3. Tipped Arrow / Projectile Potion Effects
        Entity directEntity = source.getDirectEntity();
        if (directEntity instanceof net.minecraft.world.entity.projectile.Arrow tippedArrow) {
            net.minecraft.nbt.CompoundTag arrowTag = new net.minecraft.nbt.CompoundTag();
            tippedArrow.saveWithoutId(arrowTag);
            net.minecraft.world.item.alchemy.Potion p = net.minecraft.world.item.alchemy.PotionUtils.getPotion(arrowTag);
            for (net.minecraft.world.effect.MobEffectInstance effect : p.getEffects()) {
                this.parentMob.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
            }
            for (net.minecraft.world.effect.MobEffectInstance effect : net.minecraft.world.item.alchemy.PotionUtils.getCustomEffects(arrowTag)) {
                this.parentMob.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
            }
        }

        return this.parentMob.hurt(source, finalAmount);
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
