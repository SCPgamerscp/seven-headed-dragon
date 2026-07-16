package com.sevenheadeddragon.entity;

import com.sevenheadeddragon.entity.boss.AttackPatternManager;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.EnumSet;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * The Potion Master boss ("ポーションマスター").
 * <p>
 * This is a foundational implementation covering: vanilla-Witch-sized
 * hitbox/attributes, 1000 max HP, ground-walking movement matching a
 * vanilla Witch's speed, kiting movement AI (keeps distance while
 * approaching), and the data-driven "turn" flag used by the turn system.
 * <p>
 * STILL TO BE IMPLEMENTED (see project TODO / PR description for full list):
 * <ul>
 *   <li>Full turn-based attack/dodge state machine (60s boss turn with
 *       invulnerability + bullet-hell attacks, 5s "your turn" player phase
 *       with boss frozen/defenseless and a Title-text broadcast)</li>
 *   <li>Geometric bullet-hell attack patterns (rain-from-sky, magic-circle
 *       radial bursts, geometric shapes) mixing the 14 custom potions with
 *       vanilla debuff potions</li>
 *   <li>Multiplayer single-target focus logic with target hand-off on death</li>
 *   <li>Death loot table wiring (enchanted golden apples, nether wart,
 *       diamond blocks, one of each of the 14 custom potions, massive XP)</li>
 * </ul>
 */
public class PotionMasterEntity extends Monster {

    private static final EntityDataAccessor<Boolean> DATA_PLAYER_TURN =
            SynchedEntityData.defineId(PotionMasterEntity.class, EntityDataSerializers.BOOLEAN);

    /** Boss attack-turn duration: 60 seconds. */
    public static final int BOSS_TURN_TICKS = 20 * 60;
    /** Player dodge-turn ("your turn") duration: 5 seconds. */
    public static final int PLAYER_TURN_TICKS = 20 * 5;

    /** Ticks between successive attack-pattern selections during a single boss turn. */
    private static final int ATTACK_INTERVAL_TICKS = 20 * 4; // new pattern every 4 seconds

    private final ServerBossEvent bossEvent = (ServerBossEvent)(new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    private int turnTimer = 0;
    private int attackCooldown = 0;

    /** Simple delayed-callback queue, used by {@link AttackPatternManager} to
     * stagger telegraph -> fire timing within a single server tick loop
     * without needing a full scheduler service. */
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();

    public PotionMasterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.turnTimer = BOSS_TURN_TICKS;
    }

    /** Queues {@code task} to run after {@code delayTicks} server ticks have elapsed. */
    public void scheduleIn(int delayTicks, Runnable task) {
        scheduledTasks.add(new ScheduledTask(Math.max(0, delayTicks), task));
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
                .add(Attributes.MOVEMENT_SPEED, 0.25D) // matches vanilla Witch
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 0.0D);
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

    /**
     * The player currently being focused for this attack turn. Uses
     * vanilla's own target-selection (see {@code NearestAttackableTargetGoal}
     * registered in {@link #registerGoals()}) which automatically re-picks a
     * new nearest target once the current target dies/is removed -
     * satisfying the spec's mid-turn target hand-off requirement
     * ("攻撃ターン中にターゲットとなっているプレイヤーが死亡した場合、次の
     * ターゲットに切り替えて残りの攻撃ターンを継続します").
     */
    @Nullable
    public LivingEntity getFocusedTarget() {
        return this.getTarget();
    }

    @Override
    protected void registerGoals() {
        // Kiting movement: keep distance while still slowly approaching -
        // full geometric bullet-hell AI is handled by a dedicated goal to be
        // added; this baseline keeps the boss mobile and ground-walking.
        this.goalSelector.addGoal(2, new KiteTargetGoal(this, 1.0D, 8.0F, 16.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0D));

        // Uses vanilla's standard hostile-targeting selection logic to pick
        // a single focused player target, per spec ("特定の1人をターゲットに
        // して攻撃するはバニラの敵対処理を使う").
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
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

        // Invulnerable during the boss's own attack turn; defenseless
        // ("sandbag") and immobile during the player's turn.
        if (isPlayerTurn()) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else {
            // During the boss's own 60s attack turn, periodically kick off a
            // new randomly-chosen geometric bullet-hell attack pattern so the
            // full turn is filled with telegraphed attacks rather than firing
            // only once.
            if (--attackCooldown <= 0) {
                attackCooldown = ATTACK_INTERVAL_TICKS;
                AttackPatternManager.startRandomAttack(this);
            }
        }

        if (--turnTimer <= 0) {
            boolean nextIsPlayerTurn = !isPlayerTurn();
            setPlayerTurn(nextIsPlayerTurn);
            turnTimer = nextIsPlayerTurn ? PLAYER_TURN_TICKS : BOSS_TURN_TICKS;

            if (nextIsPlayerTurn) {
                broadcastYourTurnTitle();
                scheduledTasks.clear();
            } else {
                // Immediately open the new attack turn with a pattern instead
                // of waiting a full ATTACK_INTERVAL_TICKS.
                attackCooldown = ATTACK_INTERVAL_TICKS;
                AttackPatternManager.startRandomAttack(this);
            }
        }
    }

    /**
     * Displays the large center-screen "your turn" title text to the
     * currently focused target, per spec ("その際にyour turnと表示される" /
     * "画面中央の大きな文字（タイトル表示）").
     */
    private void broadcastYourTurnTitle() {
        LivingEntity target = getFocusedTarget();
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("your turn").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)));
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Fully invulnerable to all damage while it is the boss's attack
        // turn - attacks are completely negated, not just reduced.
        if (!isPlayerTurn()) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // The Potion Master never performs direct melee/physical attacks.
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return net.minecraft.sounds.SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.WITCH_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.WITCH_DEATH;
    }

    /**
     * Massive XP payout on death, matching the Ender Dragon's approach of
     * dropping a large lump of experience directly rather than relying on
     * the generic per-kill XP value, per spec ("経験値の処理はエンドラと同じ
     * で大量に出る" / "経験値は10000落とす").
     */
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

    private class KiteTargetGoal extends Goal {
        private final PotionMasterEntity mob;
        private final double speedModifier;
        private final float keepDistance;
        private final float chaseDistance;

        public KiteTargetGoal(PotionMasterEntity mob, double speedModifier, float keepDistance, float chaseDistance) {
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

            double distanceSq = this.mob.distanceToSqr(target);
            if (distanceSq < this.keepDistance * this.keepDistance) {
                net.minecraft.world.phys.Vec3 away = net.minecraft.world.phys.Vec3.atBottomCenterOf(this.mob.blockPosition()).subtract(target.position()).normalize().scale(5);
                this.mob.getNavigation().moveTo(this.mob.getX() + away.x, this.mob.getY(), this.mob.getZ() + away.z, this.speedModifier);
            } else if (distanceSq > this.chaseDistance * this.chaseDistance) {
                this.mob.getNavigation().moveTo(target, this.speedModifier);
            } else {
                this.mob.getNavigation().stop();
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
    }
}
