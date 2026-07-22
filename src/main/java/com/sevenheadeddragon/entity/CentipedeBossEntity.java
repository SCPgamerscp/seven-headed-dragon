package com.sevenheadeddragon.entity;

import com.sevenheadeddragon.entity.boss.CentipedeAttackPatternManager;
import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.AABB;
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
import java.util.List;

/**
 * The Centipede Boss, "竜殺しのオオムカデ" (Black Dragon Eater).
 * <p>
 * A GeckoLib-animated, Undertale-style dodge boss: alternates between a
 * 60-second attack turn (fully invulnerable, randomly picking one of three
 * attacks - a circling walking AoE, a poisonous bite, and a raining-potion
 * magic attack) and a 5-second "your turn" player phase (fully defenseless
 * and immobile). Its long body has whole-body hit detection via
 * {@link CentipedePart} segments following its rendered centre line.
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
    private static final int CENTER_PART_INDEX = PART_COUNT / 2;
    private static final double PART_SPACING = 1.0D;
    private static final double SUPPORT_PROBE_DEPTH = 0.35D;
    private static final double MAX_BRIDGE_STEP_DOWN = 0.6D;
    private static final float BODY_YAW_LERP = 0.35F;

    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int turnTimer;
    private boolean patternActive = false;
    private int youchuCooldown;

    /** Whole-body hit-detection segments following the rendered body's centre line. */
    private final PartEntity<?>[] parts;
    private float currentBodyYaw;

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
        positionBodyParts();

        // Vanilla movement only tests the parent's small central box.  A long
        // creature would therefore fall through a one-block-wide gap even
        // while its body is still supported on both sides.  Treat the body as
        // a bridge for a short downward step, then update all parts again so
        // collision/debug boxes never spend a frame detached from the model.
        if (!this.level().isClientSide && stabilizeOverNarrowGap()) {
            positionBodyParts();
        }

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

    /**
     * Positions hitboxes from tail to head along the rendered body's centre
     * line.  The body yaw is interpolated on both logical sides instead of
     * rotating all 21 boxes instantly when pathfinding changes direction.
     * Previous positions are retained for Minecraft's debug-hitbox renderer,
     * which otherwise appears to teleport or draw boxes at stale locations.
     */
    private void positionBodyParts() {
        if (this.tickCount <= 1) {
            this.currentBodyYaw = this.yBodyRot;
        } else {
            this.currentBodyYaw = Mth.rotLerp(BODY_YAW_LERP, this.currentBodyYaw, this.yBodyRot);
        }

        float yawRad = -this.currentBodyYaw * Mth.DEG_TO_RAD;
        for (int i = 0; i < parts.length; i++) {
            double offsetDistance = (i - CENTER_PART_INDEX) * PART_SPACING;
            double x = this.getX() + Math.sin(yawRad) * offsetDistance;
            double y = this.getY();
            double z = this.getZ() + Math.cos(yawRad) * offsetDistance;
            PartEntity<?> part = parts[i];

            if (this.tickCount <= 1) {
                part.setPos(x, y, z);
                part.xo = x;
                part.yo = y;
                part.zo = z;
            } else {
                part.xo = part.getX();
                part.yo = part.getY();
                part.zo = part.getZ();
                part.setPos(x, y, z);
            }
        }
    }

    /**
     * Stops the central movement box dropping into a hole narrower than the
     * centipede.  Support must exist under at least one segment on each side
     * of the centre, so a single block under a dangling tail cannot suspend
     * the entire boss in mid-air.
     */
    private boolean stabilizeOverNarrowGap() {
        double stepDown = this.yo - this.getY();
        if (stepDown <= 0.0D || stepDown > MAX_BRIDGE_STEP_DOWN || this.getDeltaMovement().y > 0.0D) {
            return false;
        }

        boolean supportBehind = false;
        boolean supportAhead = false;
        for (int i = 0; i < parts.length; i++) {
            if (i == CENTER_PART_INDEX || !hasBlockSupport(parts[i])) {
                continue;
            }
            supportBehind |= i < CENTER_PART_INDEX;
            supportAhead |= i > CENTER_PART_INDEX;
            if (supportBehind && supportAhead) {
                this.setPos(this.getX(), this.yo, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().x, 0.0D, this.getDeltaMovement().z);
                this.fallDistance = 0.0F;
                return true;
            }
        }
        return false;
    }

    private boolean hasBlockSupport(PartEntity<?> part) {
        return this.level().getBlockCollisions(
                this, part.getBoundingBox().move(0.0D, -SUPPORT_PROBE_DEPTH, 0.0D)).iterator().hasNext();
    }

    /**
     * Combines the AABB bounding boxes of all 21 parts so that frustum culling
     * never hides the centipede model when any part (head, body, tail) is visible on screen.
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        AABB combinedBox = this.getBoundingBox();
        if (this.parts != null) {
            for (PartEntity<?> part : this.parts) {
                if (part != null) {
                    combinedBox = combinedBox.minmax(part.getBoundingBox());
                }
            }
        }
        return combinedBox.inflate(2.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double renderDistance = 128.0D * getViewScale();
        return distanceSqr < renderDistance * renderDistance;
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
