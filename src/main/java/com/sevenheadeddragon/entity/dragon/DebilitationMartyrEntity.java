package com.sevenheadeddragon.entity.dragon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 衰弱の殉教者 (Debilitation Martyr).
 * <p>
 * Seven of these are summoned in a ring around the player. Individually they
 * are trivial (20 HP), but each one continuously emits a
 * <b>ウィザー状態を付与する毒雲 (a poison cloud that inflicts Wither)</b> in a
 * radius around itself. Seven overlapping clouds will melt a player who
 * ignores them, so the summon is a <em>soft</em> DPS check: the clouds stop
 * the instant the martyrs die ("殉教者を撃破することで毒雲の発生を阻止可能").
 * <p>
 * The cloud is applied directly as an area effect each interval rather than as
 * a vanilla {@code AreaEffectCloud} entity, for two reasons: it guarantees the
 * cloud disappears the same tick the martyr dies (an AreaEffectCloud would
 * outlive its summoner), and it keeps seven simultaneous emitters cheap.
 * <p>
 * Rendered with the provided {@code wither_skeleton.geo.json} /
 * {@code wither_skeleton.png} model via GeckoLib.
 */
public class DebilitationMartyrEntity extends Monster implements GeoEntity {

    /** HP, per spec ("HP 20"). */
    public static final double MAX_HEALTH = 20.0D;

    /** Radius of the wither poison cloud, in blocks. */
    public static final double CLOUD_RADIUS = 4.0D;

    /** How often the cloud reapplies its debuff. */
    private static final int CLOUD_INTERVAL_TICKS = 20;

    /** Wither level and duration applied by the cloud. */
    private static final int WITHER_DURATION_TICKS = 20 * 5;
    private static final int WITHER_AMPLIFIER = 1; // Wither II

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public DebilitationMartyrEntity(EntityType<? extends DebilitationMartyrEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Undead, so Smite works and healing potions harm it - it is a wither-skeleton martyr. */
    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            spawnClientCloudParticles();
            return;
        }

        if (this.tickCount % CLOUD_INTERVAL_TICKS == 0) {
            emitWitherCloud();
        }
        if (this.tickCount % 4 == 0 && this.level() instanceof ServerLevel serverLevel) {
            spawnServerCloudParticles(serverLevel);
        }
    }

    /**
     * Applies Wither to every living entity inside {@link #CLOUD_RADIUS}.
     * Other martyrs and the dragon are exempt, so a pack of seven does not
     * wither itself to death and hand the player a free win.
     */
    private void emitWitherCloud() {
        AABB area = this.getBoundingBox().inflate(CLOUD_RADIUS);
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this && e.isAlive()
                        && !(e instanceof DebilitationMartyrEntity)
                        && !(e instanceof ApocalypseSevenHeadedRedDragonEntity)
                        && e.distanceToSqr(this) <= CLOUD_RADIUS * CLOUD_RADIUS)) {
            if (victim.getMobType() == MobType.UNDEAD) continue; // undead are immune to Wither in vanilla
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER,
                    WITHER_DURATION_TICKS, WITHER_AMPLIFIER, false, true, true), this);
        }
    }

    private void spawnServerCloudParticles(ServerLevel serverLevel) {
        for (int i = 0; i < 6; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = this.random.nextDouble() * CLOUD_RADIUS;
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX() + Math.cos(angle) * dist,
                    this.getY() + 0.3D + this.random.nextDouble() * 1.2D,
                    this.getZ() + Math.sin(angle) * dist,
                    1, 0.05D, 0.02D, 0.05D, 0.005D);
        }
    }

    private void spawnClientCloudParticles() {
        if (this.random.nextInt(2) != 0) return;
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double dist = this.random.nextDouble() * CLOUD_RADIUS;
        this.level().addParticle(ParticleTypes.SMOKE,
                this.getX() + Math.cos(angle) * dist,
                this.getY() + 0.4D,
                this.getZ() + Math.sin(angle) * dist,
                0.0D, 0.01D, 0.0D);
    }

    @Override
    public void die(DamageSource damageSource) {
        // Killing a martyr silences its cloud instantly (the cloud is emitted
        // from aiStep, so removal is automatic) - play a cue to make the
        // cause-and-effect legible to the player.
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_DEATH,
                    SoundSource.HOSTILE, 1.0F, 1.1F);
        }
        super.die(damageSource);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    /** Summoned adds give no XP - the boss itself pays out 100,000. */
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

    /** The dragon cannot friendly-fire its own martyrs. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ApocalypseSevenHeadedRedDragonEntity) return false;
        return super.hurt(source, amount);
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::predicate));
    }

    private PlayState predicate(AnimationState<DebilitationMartyrEntity> state) {
        // The provided wither_skeleton asset set ships no animation file, so
        // the static model is rendered; a future animation JSON can be wired
        // in here without touching anything else.
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
