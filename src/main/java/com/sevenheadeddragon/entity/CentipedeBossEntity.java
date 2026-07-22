package com.sevenheadeddragon.entity;

import com.sevenheadeddragon.entity.boss.CentipedeAttackPatternManager;
import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * The Centipede Boss, "竜殺しのオオムカデ" (Black Dragon Eater).
 * <p>
 * A GeckoLib-animated, Undertale-style dodge boss: alternates between a
 * 60-second attack turn (fully invulnerable, randomly picking one of three
 * attacks - a circling walking AoE, a poisonous bite, and a raining-potion
 * magic attack) and a 5-second "your turn" player phase (fully defenseless
 * and immobile). Its long body has whole-body hit detection via
 * {@link CentipedePart} segments trailing its recorded movement path.
 * Spawned when a player kills 10 spiders while under Bad Omen (see
 * {@code event.CentipedeSpawnHandler}).
 */
public class CentipedeBossEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> DATA_PLAYER_TURN =
            SynchedEntityData.defineId(CentipedeBossEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int BOSS_TURN_TICKS = 20 * 60;
    public static final int PLAYER_TURN_TICKS = 20 * 5;
    private static final int MIN_TICKS_FOR_NEW_PATTERN = 40;

    /** Number of body segments used for whole-body hit detection. */
    public static final int PART_COUNT = 21;
    /** How many ticks apart (along the recorded movement trail) each body segment sits from the next. */
    private static final int PART_TRAIL_SPACING_TICKS = 4;
    private static final int TRAIL_HISTORY_LENGTH = PART_COUNT * PART_TRAIL_SPACING_TICKS + 10;

    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int turnTimer;
    private boolean patternActive = false;
    private int youchuCooldown;

    /** Whole-body hit-detection segments, and the recorded movement trail they follow. */
    private final PartEntity<?>[] parts;
    private final Vec3[] trail = new Vec3[TRAIL_HISTORY_LENGTH];
    private int trailPointer = -1;
    private float currentBodyYaw = 0.0F;

    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();

    public CentipedeBossEntity(EntityType<? extends CentipedeBossEntity> type, Level level) {
        super(type, level);
        this.turnTimer = BOSS_TURN_TICKS;
        this.parts = new PartEntity<?>[PART_COUNT];
        for (int i = 0; i < PART_COUNT; i++) {
            this.parts[i] = new CentipedePart(this, i, 2.0F, 2.0F);
        }
        this.youchuCooldown = 100 + this.random.nextInt(400);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D) // same as a vanilla Spider
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D);
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD; // so Bane of Arthropods works, per spec ("虫特効が効く")
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < this.parts.length; i++) {
            this.parts[i].setId(id + i + 1);
        }
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        for (int i = 0; i < this.parts.length; i++) {
            this.parts[i].setId(packet.getId() + i + 1);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PLAYER_TURN, false);
        this.entityData.define(DATA_ACTION_STATE, ACTION_IDLE_OR_WALK);
    }

    public boolean isPlayerTurn() {
        return this.entityData.get(DATA_PLAYER_TURN);
    }

    protected void setPlayerTurn(boolean value) {
        this.entityData.set(DATA_PLAYER_TURN, value);
    }

    @Nullable
    public LivingEntity getFocusedTarget() {
        return this.getTarget();
    }

    public void scheduleIn(int delayTicks, Runnable task) {
        scheduledTasks.add(new ScheduledTask(Math.max(0, delayTicks), task));
    }

    public void onPatternFinished() {
        this.patternActive = false;
    }

    private void tickScheduledTasks() {
        // Iterate backwards by index (not an Iterator) so that a task's own
        // Runnable can safely call scheduleIn() again (e.g. to chain to its
        // next step) without triggering a ConcurrentModificationException -
        // matches the same fix applied in FangKingEntity.
        for (int i = scheduledTasks.size() - 1; i >= 0; i--) {
            ScheduledTask t = scheduledTasks.get(i);
            if (t.ticksLeft-- <= 0) {
                scheduledTasks.remove(i);
                t.task.run();
            }
        }
    }

    private static final class ScheduledTask {
        int ticksLeft;
        final Runnable task;

        ScheduledTask(int ticksLeft, Runnable task) {
            this.ticksLeft = ticksLeft;
            this.task = task;
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new CentipedeCircleGoal(this, 1.4D, 15.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0D));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** True while the "circle & walking damage" attack pattern wants the boss to orbit its target at a fixed distance. */
    private boolean circlingActive = false;

    public void setCirclingActive(boolean circling) {
        this.circlingActive = circling;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        recordTrailAndPositionParts();

        if (this.level().isClientSide) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        tickScheduledTasks();
        tickYouchuSound();

        if (isPlayerTurn()) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else if (!patternActive && turnTimer > MIN_TICKS_FOR_NEW_PATTERN) {
            patternActive = true;
            CentipedeAttackPatternManager.startRandomAttack(this);
        }

        if (--turnTimer <= 0) {
            boolean nextIsPlayerTurn = !isPlayerTurn();
            setPlayerTurn(nextIsPlayerTurn);
            turnTimer = nextIsPlayerTurn ? PLAYER_TURN_TICKS : BOSS_TURN_TICKS;
            patternActive = false;
            setCirclingActive(false);
            scheduledTasks.clear();

            if (nextIsPlayerTurn) {
                broadcastYourTurnTitle();
            }
        }
    }

    /** Positions every body-segment part along the centipede's full length from tail (-10m) to head (+10m) using Forge yBodyRot. */
    private void recordTrailAndPositionParts() {
        float yawRad = -this.yBodyRot * ((float) Math.PI / 180.0F);

        for (int i = 0; i < parts.length; i++) {
            double offsetDist = (i - 10) * 1.0D;
            double offsetX = Math.sin(yawRad) * offsetDist;
            double offsetZ = Math.cos(yawRad) * offsetDist;

            parts[i].setPos(this.getX() + offsetX, this.getY(), this.getZ() + offsetZ);
            parts[i].xo = this.getX() + offsetX;
            parts[i].yo = this.getY();
            parts[i].zo = this.getZ() + offsetZ;
        }
    }

    private void tickYouchuSound() {
        if (--youchuCooldown <= 0) {
            this.playSound(ModSounds.CENTIPEDE_YOUCHU.get(), 1.0F, 1.0F);
            youchuCooldown = 300 + this.random.nextInt(600); // random interval, roughly 15-45 seconds
        }
    }

    private void broadcastYourTurnTitle() {
        LivingEntity target = getFocusedTarget();
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("your turn").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isPlayerTurn()) {
            if (!this.level().isClientSide) {
                this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0f, 1.0f);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
                }
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // No automatic melee - biting is one of the deliberate attack patterns instead.
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return net.minecraft.sounds.SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.SPIDER_DEATH;
    }

    @Override
    public int getExperienceReward() {
        return 10000;
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * Drops the Centipede Boss's death loot directly in code (no loot table
     * datapack): 64 Diamond Blocks + 20 Enchanted Golden Apples scaled by
     * Looting ("ドロップ増加対応"), plus a fixed 5 each of the two splash
     * potions matching the effects it used in combat (Dragon-Slaying Poison,
     * Poison V), unaffected by Looting.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        if (this.level().isClientSide) return;

        spawnStackedLoot(Items.DIAMOND_BLOCK, 64 + looting * 8);
        spawnStackedLoot(Items.ENCHANTED_GOLDEN_APPLE, 20 + looting * 4);

        this.spawnAtLocation(net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new ItemStack(Items.SPLASH_POTION), com.sevenheadeddragon.registry.ModPotions.DRAGON_SLAYING_POISON.get()).copyWithCount(5));
        this.spawnAtLocation(net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new ItemStack(Items.SPLASH_POTION), com.sevenheadeddragon.registry.ModPotions.POISON_5.get()).copyWithCount(5));
    }

    private void spawnStackedLoot(Item item, int totalCount) {
        int maxStack = new ItemStack(item).getMaxStackSize();
        int remaining = totalCount;
        while (remaining > 0) {
            int chunk = Math.min(maxStack, remaining);
            this.spawnAtLocation(new ItemStack(item, chunk));
            remaining -= chunk;
        }
    }

    // ------------------------------------------------------------------
    // GeckoLib animation wiring
    // ------------------------------------------------------------------

    private static final EntityDataAccessor<Byte> DATA_ACTION_STATE =
            SynchedEntityData.defineId(CentipedeBossEntity.class, EntityDataSerializers.BYTE);

    public static final byte ACTION_IDLE_OR_WALK = 0;
    public static final byte ACTION_BITING = 1;
    public static final byte ACTION_MAGIC_GETUP = 2;
    public static final byte ACTION_MAGIC_CASTING = 3;
    public static final byte ACTION_MAGIC_GETDOWN = 4;

    public void setActionState(byte state) {
        this.entityData.set(DATA_ACTION_STATE, state);
    }

    public byte getActionState() {
        return this.entityData.get(DATA_ACTION_STATE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<CentipedeBossEntity> state) {
        switch (this.getActionState()) {
            case ACTION_BITING -> state.getController().setAnimation(RawAnimation.begin().then("animation.biting", software.bernie.geckolib.core.animation.Animation.LoopType.PLAY_ONCE));
            case ACTION_MAGIC_GETUP -> state.getController().setAnimation(RawAnimation.begin().then("animation.getup", software.bernie.geckolib.core.animation.Animation.LoopType.PLAY_ONCE));
            case ACTION_MAGIC_CASTING -> state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.magic.casting"));
            case ACTION_MAGIC_GETDOWN -> state.getController().setAnimation(RawAnimation.begin().then("animation.getdown", software.bernie.geckolib.core.animation.Animation.LoopType.PLAY_ONCE));
            default -> {
                if (state.isMoving()) {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.centipede.walk"));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().thenLoop("animation.centipede.idle"));
                }
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    // ------------------------------------------------------------------
    // Circling movement (for the walking-AoE attack pattern)
    // ------------------------------------------------------------------

    private class CentipedeCircleGoal extends Goal {
        private final CentipedeBossEntity mob;
        private final double speedModifier;
        private final float radius;
        private double angle;

        CentipedeCircleGoal(CentipedeBossEntity mob, double speedModifier, float radius) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.radius = radius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return this.mob.circlingActive && target != null && target.isAlive() && !this.mob.isPlayerTurn();
        }

        @Override
        public void start() {
            LivingEntity target = this.mob.getTarget();
            if (target != null) {
                Vec3 toMob = this.mob.position().subtract(target.position());
                angle = Math.atan2(toMob.z, toMob.x);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            angle += 0.08D; // orbit speed
            double x = target.getX() + radius * Math.cos(angle);
            double z = target.getZ() + radius * Math.sin(angle);
            this.mob.getNavigation().moveTo(x, target.getY(), z, speedModifier);
        }
    }
}
