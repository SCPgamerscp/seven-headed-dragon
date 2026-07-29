package com.sevenheadeddragon.entity.dragon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * クリーパー 10秒時限爆発ギミック (Timed Gimmick Creeper).
 * <p>
 * A hard time-limit puzzle rather than a normal creeper fight. Seven of these
 * are summoned in a ring around the player and each one carries a
 * <b>10-second fuse that cannot be escaped by running away</b> - unlike a
 * vanilla creeper, which only primes when a player is nearby and defuses when
 * they retreat. When the fuse hits zero it detonates a
 * <b>power-10 explosion with no terrain damage</b>.
 * <p>
 * The only way out is offence: the player has 10 seconds to kill all seven.
 * A creeper killed before its timer expires is defused and does
 * <em>not</em> explode ({@link #die} suppresses the blast), which is what
 * turns the mechanic into "撃破 vs. 被弾" rather than an unavoidable hit.
 * <p>
 * The remaining fuse time is synced to clients so the renderer can flash the
 * creeper white with increasing urgency as the countdown closes.
 */
public class TimedGimmickCreeperEntity extends Monster {

    /** Explosion power, per spec ("爆発力 10（地形破壊なし）"). */
    public static final float EXPLOSION_POWER = 10.0F;

    /** Fuse length, per spec ("10秒後に即自爆"). */
    public static final int FUSE_TICKS = 20 * 10;

    /** Health - low enough that killing seven in 10 seconds is achievable. */
    public static final double MAX_HEALTH = 20.0D;

    private static final EntityDataAccessor<Integer> DATA_FUSE =
            SynchedEntityData.defineId(TimedGimmickCreeperEntity.class, EntityDataSerializers.INT);

    /** Set while {@link #explode()} runs, so the resulting death does not re-trigger it. */
    private boolean detonating;

    public TimedGimmickCreeperEntity(EntityType<? extends TimedGimmickCreeperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FUSE, FUSE_TICKS);
    }

    @Override
    protected void registerGoals() {
        // Deliberately minimal: it walks toward the player it was summoned
        // against, but its threat comes purely from the fuse, so it needs no
        // swell/attack AI of its own.
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
                net.minecraft.world.entity.player.Player.class, 8.0F));

        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.player.Player.class, true));
    }

    /** Remaining fuse in ticks (synced, used by the renderer's flash effect). */
    public int getRemainingFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    /** 0.0 at spawn → 1.0 at detonation. Drives the client-side flash intensity. */
    public float getFuseProgress() {
        return 1.0F - (getRemainingFuse() / (float) FUSE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        int remaining = getRemainingFuse() - 1;
        this.entityData.set(DATA_FUSE, Math.max(0, remaining));

        // An audible tick that speeds up as the fuse burns down, so the player
        // can feel the deadline without staring at the mob.
        int interval = remaining > 60 ? 20 : (remaining > 20 ? 10 : 4);
        if (remaining > 0 && remaining % interval == 0) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(),
                    SoundSource.HOSTILE, 0.8F, 1.4F + getFuseProgress());
        }

        if (remaining <= 0) {
            explode();
        }
    }

    /** Detonates immediately: power 10, no terrain damage, then removes itself. */
    private void explode() {
        if (this.level().isClientSide || this.detonating) return;
        this.detonating = true;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
        DragonExplosions.explodeNoGrief(this.level(), this, this.position(), EXPLOSION_POWER);
        this.discard();
    }

    /**
     * Killing a gimmick creeper <em>defuses</em> it - this is the whole point
     * of the 10-second race, so death must never fall through to
     * {@link #explode()}.
     */
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (!this.level().isClientSide && !this.detonating) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_BREAK,
                    SoundSource.HOSTILE, 1.0F, 1.6F);
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.CREEPER_PRIMED;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CREEPER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }

    /** Summoned adds should never drop XP - the boss itself pays out 100,000. */
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

    /** Immune to the dragon's own attacks, so a stray missile cannot clear the puzzle for the player. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ApocalypseSevenHeadedRedDragonEntity) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                && source.getEntity() instanceof TimedGimmickCreeperEntity) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuse", getRemainingFuse());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Fuse")) {
            this.entityData.set(DATA_FUSE, tag.getInt("Fuse"));
        }
    }
}
