package com.sevenheadeddragon.entity;

import com.sevenheadeddragon.entity.boss.FangKingAttackPatternManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * The Fang King boss ("牙の王").
 * <p>
 * Undertale-inspired dodge boss: alternates between a 60-second attack turn
 * (fully invulnerable, weaving geometric EvokerFangs bullet-hell patterns
 * and summoning Vex reinforcements) and a 5-second "your turn" player phase
 * (fully defenseless and immobile, broadcasting a "your turn" title). Spawned
 * automatically whenever a vanilla Raid ends in victory (see
 * {@code event.RaidVictoryHandler}).
 */
public class FangKingEntity extends Monster {

    private static final EntityDataAccessor<Boolean> DATA_PLAYER_TURN =
            SynchedEntityData.defineId(FangKingEntity.class, EntityDataSerializers.BOOLEAN);

    /** Boss attack-turn duration: 60 seconds. */
    public static final int BOSS_TURN_TICKS = 20 * 60;
    /** Player dodge-turn ("your turn") duration: 5 seconds. */
    public static final int PLAYER_TURN_TICKS = 20 * 5;
    /** Do not start a brand-new attack pattern if less than this many ticks remain in the boss turn (avoids overlapping into "your turn"). */
    private static final int MIN_TICKS_FOR_NEW_PATTERN = 40;

    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    private int turnTimer;
    private boolean patternActive = false;

    /** Simple delayed-callback queue used by {@link FangKingAttackPatternManager} to stagger telegraph -> fire timing. */
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();

    public FangKingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.turnTimer = BOSS_TURN_TICKS;
    }

    /** Queues {@code task} to run after {@code delayTicks} server ticks have elapsed. */
    public void scheduleIn(int delayTicks, Runnable task) {
        scheduledTasks.add(new ScheduledTask(Math.max(0, delayTicks), task));
    }

    /** Called by {@link FangKingAttackPatternManager} once a pattern has fully finished, so the next one can begin immediately (no cooldown). */
    public void onPatternFinished() {
        this.patternActive = false;
    }

    private void tickScheduledTasks() {
        Iterator<ScheduledTask> it = scheduledTasks.iterator();
        while (it.hasNext()) {
            ScheduledTask t = it.next();
            if (t.ticksLeft-- <= 0) {
                it.remove();
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

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PLAYER_TURN, false);
    }

    public boolean isPlayerTurn() {
        return this.entityData.get(DATA_PLAYER_TURN);
    }

    protected void setPlayerTurn(boolean value) {
        this.entityData.set(DATA_PLAYER_TURN, value);
    }

    /** The player currently being focused for this attack turn (also used as the Vex summon's shared target via vanilla's own owner-target copy AI). */
    @Nullable
    public LivingEntity getFocusedTarget() {
        return this.getTarget();
    }

    @Override
    protected void registerGoals() {
        // Approaches when the target is far, retreats when the target is close (10-14 block neutral zone); only active during the boss's own attack turn.
        this.goalSelector.addGoal(2, new FangKingKiteGoal(this, 1.0D, 10.0F, 14.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0D));

        // Nearest player, or whichever entity (including mobs like Iron Golems) attacks it first.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
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
        if (this.level().isClientSide) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        tickScheduledTasks();

        if (isPlayerTurn()) {
            // "your turn": completely frozen and defenseless.
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else if (!patternActive && turnTimer > MIN_TICKS_FOR_NEW_PATTERN) {
            // No forced interval - as soon as the previous pattern finished (onPatternFinished), start the next one immediately.
            patternActive = true;
            FangKingAttackPatternManager.startRandomAttack(this);
        }

        if (--turnTimer <= 0) {
            boolean nextIsPlayerTurn = !isPlayerTurn();
            setPlayerTurn(nextIsPlayerTurn);
            turnTimer = nextIsPlayerTurn ? PLAYER_TURN_TICKS : BOSS_TURN_TICKS;
            patternActive = false;
            scheduledTasks.clear();

            if (nextIsPlayerTurn) {
                broadcastYourTurnTitle();
            }
        }
    }

    /** Displays the large center-screen "your turn" title text to the currently focused target. */
    private void broadcastYourTurnTitle() {
        LivingEntity target = getFocusedTarget();
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("your turn").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Fully invulnerable during its own attack turn - attacks are deflected, not just reduced.
        if (!isPlayerTurn()) {
            if (!this.level().isClientSide) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0f, 1.2f);
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
        // The Fang King never performs direct melee attacks - only fang barrages and Vex summons.
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
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

    /** Grants "Hero of the Village" Level 10 for 100 minutes to everyone who fought the boss (tracked via the boss bar), per spec. */
    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide) {
            for (ServerPlayer player : this.bossEvent.getPlayers()) {
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 20 * 60 * 100, 9, false, true, true));
            }
        }
        super.die(damageSource);
    }

    /**
     * Drops the Fang King's death loot directly in code (no loot table
     * datapack): 64 Emerald Blocks, 10 Totems of Undying, 20 Enchanted
     * Golden Apples, scaled up by the Looting enchantment level ("ドロップ増加対応").
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        if (this.level().isClientSide) return;

        spawnStackedLoot(Items.EMERALD_BLOCK, 64 + looting * 8);
        spawnStackedLoot(Items.TOTEM_OF_UNDYING, 10 + looting * 2);
        spawnStackedLoot(Items.ENCHANTED_GOLDEN_APPLE, 20 + looting * 4);
    }

    /** Spawns {@code totalCount} of {@code item}, automatically splitting across multiple stacks if it exceeds the item's max stack size (e.g. Totems, which are not stackable). */
    private void spawnStackedLoot(Item item, int totalCount) {
        int maxStack = new ItemStack(item).getMaxStackSize();
        int remaining = totalCount;
        while (remaining > 0) {
            int chunk = Math.min(maxStack, remaining);
            this.spawnAtLocation(new ItemStack(item, chunk));
            remaining -= chunk;
        }
    }

    /**
     * Kiting movement AI: approaches the target while it is far away, but
     * retreats once the target gets within {@code keepDistance} - only
     * active during the boss's own attack turn.
     */
    private class FangKingKiteGoal extends Goal {
        private final FangKingEntity mob;
        private final double speedModifier;
        private final float keepDistance;
        private final float chaseDistance;

        public FangKingKiteGoal(FangKingEntity mob, double speedModifier, float keepDistance, float chaseDistance) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.keepDistance = keepDistance;
            this.chaseDistance = chaseDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target.isAlive() && !this.mob.isPlayerTurn();
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double distanceSq = this.mob.distanceToSqr(target);
            if (distanceSq < this.keepDistance * this.keepDistance) {
                if (this.mob.getNavigation().isDone()) {
                    Vec3 retreatPos = DefaultRandomPos.getPosAway(this.mob, 16, 7, target.position());
                    if (retreatPos != null) {
                        this.mob.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, this.speedModifier * 1.2);
                    }
                }
            } else if (distanceSq > this.chaseDistance * this.chaseDistance) {
                if (this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(target, this.speedModifier);
                }
            } else if (!this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().stop();
            }
        }
    }
}
