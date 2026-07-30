package com.sevenheadeddragon.entity.dragon;

import com.sevenheadeddragon.entity.boss.RedDragonAttackPatternManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.Animation;
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
 * 終末の七つ頭の赤い竜 (Apocalypse Seven Headed Red Dragon).
 * <p>
 * The mod's ultimate boss - the "god's eternal enemy" from Revelation, and by
 * far the hardest encounter in the pack. Structurally it follows the same
 * Undertale-inspired turn rhythm the mod's other bosses use, but scaled to
 * absurdity:
 * <ul>
 *   <li><b>ボスターン (60s / 1200 ticks)</b>: a completely unbroken torrent of
 *       attacks - the 7-bite sin combo, goat machine-gun and homing squid
 *       missiles, the Longinus Spear cross-drop, the 35-bolt Shocker Breaker,
 *       timed gimmick creepers, martyr summons, charge/tail/claw melee - chosen
 *       at random and chained back-to-back with no gaps.</li>
 *   <li><b>プレイヤーターン (5s / 100 ticks)</b>: a golden <b>YOUR TURN</b>
 *       title drops, the dragon freezes completely (navigation stopped, no new
 *       attacks) and becomes the only window in which it can be damaged at
 *       all.</li>
 * </ul>
 * Its 3000 armor / 300 armor toughness means chip damage is meaningless; the
 * 5-second windows are the entire fight. Knockback resistance is 1.0, so it
 * cannot be shoved out of position, and while it lives the whole world is
 * dyed red (see {@code RedDragonSpawnHandler} + the client fog handler).
 */
public class ApocalypseSevenHeadedRedDragonEntity extends Monster implements GeoEntity {

    // ------------------------------------------------------------------
    // Core parameters (section 1 of the master spec)
    // ------------------------------------------------------------------

    public static final double MAX_HEALTH = 1000.0D;
    public static final double ARMOR = 3000.0D;
    public static final double ARMOR_TOUGHNESS = 300.0D;
    public static final double MOVEMENT_SPEED = 0.30D;
    public static final double KNOCKBACK_RESISTANCE = 1.0D;

    /** ボスターン: 60 seconds of uninterrupted offence. */
    public static final int BOSS_TURN_TICKS = 20 * 60;
    /** プレイヤーターン: the 5-second counter-attack window. */
    public static final int PLAYER_TURN_TICKS = 20 * 5;
    /** Never start a fresh pattern with less than this left, so attacks never bleed into YOUR TURN. */
    private static final int MIN_TICKS_FOR_NEW_PATTERN = 40;

    /** Experience payout, per spec ("経験値: 100,000 EXP"). */
    public static final int EXPERIENCE_REWARD = 100_000;

    // ------------------------------------------------------------------
    // Synched state
    // ------------------------------------------------------------------

    private static final EntityDataAccessor<Boolean> DATA_PLAYER_TURN =
            SynchedEntityData.defineId(ApocalypseSevenHeadedRedDragonEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Byte> DATA_ACTION_STATE =
            SynchedEntityData.defineId(ApocalypseSevenHeadedRedDragonEntity.class, EntityDataSerializers.BYTE);

    /** Which of the seven bite animations (1-7) is currently playing; 0 = none. */
    private static final EntityDataAccessor<Byte> DATA_BITE_INDEX =
            SynchedEntityData.defineId(ApocalypseSevenHeadedRedDragonEntity.class, EntityDataSerializers.BYTE);

    /** Bumped every time a one-shot animation is (re)triggered, so GeckoLib replays it. */
    private static final EntityDataAccessor<Integer> DATA_ANIM_TICKET =
            SynchedEntityData.defineId(ApocalypseSevenHeadedRedDragonEntity.class, EntityDataSerializers.INT);

    // Action states, mapped 1:1 onto the supplied dragon.animation.json clips.
    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_BITE = 1;
    public static final byte ACTION_CLAW = 2;
    public static final byte ACTION_CHARGE = 3;
    public static final byte ACTION_TAIL = 4;
    public static final byte ACTION_FLY_START = 5;
    public static final byte ACTION_FLY = 6;
    public static final byte ACTION_FLY_END = 7;

    // ------------------------------------------------------------------
    // Animations (names taken verbatim from the provided dragon.animation.json)
    // ------------------------------------------------------------------

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.dragon.idle");
    private static final RawAnimation ANIM_CLAW = RawAnimation.begin().thenLoop("animation.dragon.claw");
    private static final RawAnimation ANIM_CHARGE = RawAnimation.begin().thenLoop("animation.dragon.attack_charge");
    private static final RawAnimation ANIM_TAIL =
            RawAnimation.begin().then("animation.dragon.attack_tail", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_FLY_START =
            RawAnimation.begin().then("animation.dragon.fly.start", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("animation.dragon.fly");
    private static final RawAnimation ANIM_FLY_END =
            RawAnimation.begin().then("animation.dragon.fly.end", Animation.LoopType.PLAY_ONCE);

    /** attack_bite_1 .. attack_bite_7, indexed 0-6. */
    private static final RawAnimation[] ANIM_BITES = new RawAnimation[7];
    static {
        for (int i = 0; i < 7; i++) {
            ANIM_BITES[i] = RawAnimation.begin()
                    .then("animation.dragon.attack_bite_" + (i + 1), Animation.LoopType.PLAY_ONCE);
        }
    }

    // ------------------------------------------------------------------
    // Instance state
    // ------------------------------------------------------------------

    /** 漆黒/紫色の専用ボスバー. */
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)
            .setDarkenScreen(true)
            .setCreateWorldFog(true);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int turnTimer;
    private boolean patternActive;

    /** Set while the flying/magic-casting mode is airborne, so gravity is suspended. */
    private boolean hovering;
    private double hoverTargetY;

    /** Set while a charge attack is driving movement, so the kite goal keeps out of the way. */
    private boolean charging;

    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();

    public ApocalypseSevenHeadedRedDragonEntity(EntityType<? extends ApocalypseSevenHeadedRedDragonEntity> type,
                                                Level level) {
        super(type, level);
        this.turnTimer = BOSS_TURN_TICKS;
        this.setMaxUpStep(0.0F);
        this.xpReward = EXPERIENCE_REWARD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PLAYER_TURN, false);
        this.entityData.define(DATA_ACTION_STATE, ACTION_IDLE);
        this.entityData.define(DATA_BITE_INDEX, (byte) 0);
        this.entityData.define(DATA_ANIM_TICKET, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new DragonApproachGoal(this, 1.0D, 12.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 32.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------
    // Turn state
    // ------------------------------------------------------------------

    public boolean isPlayerTurn() {
        return this.entityData.get(DATA_PLAYER_TURN);
    }

    protected void setPlayerTurn(boolean value) {
        this.entityData.set(DATA_PLAYER_TURN, value);
    }

    @Nullable
    public LivingEntity getFocusedTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            return target;
        }
        Player nearestPlayer = this.level().getNearestPlayer(this, 64.0D);
        if (nearestPlayer != null && nearestPlayer.isAlive()) {
            this.setTarget(nearestPlayer);
            return nearestPlayer;
        }
        return null;
    }

    /** Called by the pattern manager once a pattern is completely finished. */
    public void onPatternFinished() {
        this.patternActive = false;
        setActionState(ACTION_IDLE);
    }

    // ------------------------------------------------------------------
    // Animation state
    // ------------------------------------------------------------------

    public void setActionState(byte state) {
        this.entityData.set(DATA_ACTION_STATE, state);
        this.entityData.set(DATA_ANIM_TICKET, this.entityData.get(DATA_ANIM_TICKET) + 1);
    }

    public byte getActionState() {
        return this.entityData.get(DATA_ACTION_STATE);
    }

    /** Plays bite animation {@code index} (0-6 → attack_bite_1..7). */
    public void playBite(int index) {
        this.entityData.set(DATA_BITE_INDEX, (byte) Math.floorMod(index, 7));
        setActionState(ACTION_BITE);
    }

    public int getBiteIndex() {
        return this.entityData.get(DATA_BITE_INDEX);
    }

    // ------------------------------------------------------------------
    // Flight / hover control (⑦ 飛行・魔法詠唱モード)
    // ------------------------------------------------------------------

    /** Begins hovering toward {@code targetY}; gravity is suspended until {@link #stopHovering()}. */
    public void startHovering(double targetY) {
        this.hovering = true;
        this.hoverTargetY = targetY;
        this.setNoGravity(true);
    }

    public void stopHovering() {
        this.hovering = false;
        this.setNoGravity(false);
    }

    public boolean isHovering() {
        return this.hovering;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public boolean isCharging() {
        return this.charging;
    }

    // ------------------------------------------------------------------
    // Scheduling (mirrors the pattern used by the mod's other bosses)
    // ------------------------------------------------------------------

    public void scheduleIn(int delayTicks, Runnable task) {
        this.scheduledTasks.add(new ScheduledTask(Math.max(0, delayTicks), task));
    }

    private void tickScheduledTasks() {
        // Iterate backwards by index so a task can safely schedule further
        // tasks from inside its own Runnable (every one of this boss's combos
        // chains itself this way) without a ConcurrentModificationException.
        for (int i = this.scheduledTasks.size() - 1; i >= 0; i--) {
            ScheduledTask task = this.scheduledTasks.get(i);
            if (task.ticksLeft-- <= 0) {
                this.scheduledTasks.remove(i);
                task.task.run();
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

    // ------------------------------------------------------------------
    // Boss bar
    // ------------------------------------------------------------------

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

    /** Everyone currently engaged with the boss (used for drops, titles and the advancement). */
    public List<ServerPlayer> getEngagedPlayers() {
        return new ArrayList<>(this.bossEvent.getPlayers());
    }

    // ------------------------------------------------------------------
    // Main tick
    // ------------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        tickScheduledTasks();
        tickHover();

        if (isPlayerTurn()) {
            // プレイヤーターン: 地上に停止して無防備
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.isHovering() ? 0.0D : this.getDeltaMovement().y, 0.0D);
        } else {
            // ボスターン（計60秒 / 1200 ticks）:
            // 前半30秒(1200〜601): 地上近接物理攻撃
            // 後半30秒(600〜1): 上空10ブロックに滞空し、空中魔法攻撃
            if (this.turnTimer <= 600 && !this.isHovering()) {
                this.startHovering(this.getY() + 10.0D);
                this.setActionState(ACTION_FLY_START);
                this.scheduleIn(20, () -> this.setActionState(ACTION_FLY));
            }

            if (!this.patternActive && this.turnTimer > MIN_TICKS_FOR_NEW_PATTERN) {
                this.patternActive = true;
                RedDragonAttackPatternManager.startRandomAttack(this);
            }
        }

        if (--this.turnTimer <= 0) {
            switchTurn();
        }
    }

    /** Smoothly lifts/holds the dragon at its hover altitude during the flying magic mode. */
    private void tickHover() {
        if (!this.hovering) return;

        double dy = this.hoverTargetY - this.getY();
        if (Math.abs(dy) < 0.25D) {
            this.setDeltaMovement(this.getDeltaMovement().x * 0.6D, 0.0D, this.getDeltaMovement().z * 0.6D);
        } else {
            double lift = Math.signum(dy) * Math.min(0.45D, Math.abs(dy) * 0.25D);
            this.setDeltaMovement(this.getDeltaMovement().x * 0.8D, lift, this.getDeltaMovement().z * 0.8D);
        }
        this.fallDistance = 0.0F;
    }

    /** Flips between the 60-second boss turn and the 5-second player turn. */
    private void switchTurn() {
        boolean nextIsPlayerTurn = !isPlayerTurn();
        setPlayerTurn(nextIsPlayerTurn);
        this.turnTimer = nextIsPlayerTurn ? PLAYER_TURN_TICKS : BOSS_TURN_TICKS;
        this.patternActive = false;
        this.charging = false;
        this.scheduledTasks.clear();

        if (nextIsPlayerTurn) {
            // ボスターン終了: 着地してプレイヤーの反撃ターン開始
            if (this.isHovering()) {
                setActionState(ACTION_FLY_END);
                scheduleIn(20, () -> {
                    stopHovering();
                    setActionState(ACTION_IDLE);
                });
            } else {
                setActionState(ACTION_IDLE);
            }
            broadcastYourTurnTitle();
        } else {
            setActionState(ACTION_IDLE);
        }
    }

    /** Displays the golden bold <b>YOUR TURN</b> title to everyone fighting the boss. */
    private void broadcastYourTurnTitle() {
        Component title = Component.literal("YOUR TURN")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (ServerPlayer player : getEngagedPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(2, PLAYER_TURN_TICKS - 10, 8));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1.0F, 1.4F);
        }
    }

    // ------------------------------------------------------------------
    // Damage handling
    // ------------------------------------------------------------------

    /**
     * Fully invulnerable outside the player's turn - attacks are deflected
     * with a shield clang, not merely reduced. Combined with 3000 armor this
     * makes the 5-second windows the only meaningful damage opportunity.
     */
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return 0;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) return false;
        if (!isPlayerTurn()) {
            if (!this.level().isClientSide) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK,
                        SoundSource.HOSTILE, 1.0F, 0.7F);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            this.getX(), this.getY() + 4.0D, this.getZ(), 12, 1.2D, 1.2D, 1.2D, 0.0D);
                }
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    /** All melee is driven deliberately by the attack patterns, never by vanilla AI. */
    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Immovable: a 1000 HP apocalypse dragon is not shoved around by mobs.
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        // Immune to every debuff - including the player mirroring the mod's own
        // custom poisons back at it.
        return false;
    }

    @Override
    public boolean fireImmune() {
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

    @Override
    public int getExperienceReward() {
        return EXPERIENCE_REWARD;
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return true;
    }

    // ------------------------------------------------------------------
    // Death: light-beam spectacle, sky restoration, advancement, drops
    // ------------------------------------------------------------------

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide) {
            for (ServerPlayer player : getEngagedPlayers()) {
                // 🏆 実績「世界を救った」
                com.sevenheadeddragon.event.RedDragonSpawnHandler.grantWorldSavedAdvancement(player);
            }
            // 赤く染まっていた空が元の青空へ復元
            com.sevenheadeddragon.event.RedDragonSpawnHandler.onDragonDefeated(this);
        }
        super.die(damageSource);
    }

    /**
     * Ender-dragon style death: a rising beam of light and escalating
     * explosions over the ~200-tick death animation, then the sky returns to
     * normal blue.
     */
    @Override
    protected void tickDeath() {
        ++this.deathTime;

        if (this.level() instanceof ServerLevel serverLevel) {
            // A column of light climbing out of the corpse.
            for (int i = 0; i < 6; i++) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 6.0D,
                        this.getY() + this.random.nextDouble() * 20.0D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 6.0D,
                        1, 0.0D, 0.35D, 0.0D, 0.06D);
            }
            if (this.deathTime % 8 == 0) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 8.0D,
                        this.getY() + this.random.nextDouble() * 6.0D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 8.0D,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                        SoundSource.HOSTILE, 3.0F, 0.5F);
            }
            if (this.deathTime == 1) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_DEATH,
                        SoundSource.HOSTILE, 4.0F, 0.6F);
            }
        }

        if (this.deathTime >= 200 && !this.level().isClientSide) {
            // Vanilla's XP payout is capped per orb, so 100,000 EXP is emitted
            // in bulk here rather than through the normal small-orb drip.
            dropExperienceBulk();
            this.remove(Entity.RemovalReason.KILLED);
            this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DIE);
        }
    }

    private void dropExperienceBulk() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        net.minecraft.world.entity.ExperienceOrb.award(serverLevel, this.position(), EXPERIENCE_REWARD);
    }

    /**
     * 討伐ドロップ報酬: the spec's exact loot table, built in code (no datapack
     * loot table) so the custom Elytra's enchantments and unbreakable flag are
     * guaranteed rather than relying on a loot-function chain.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        if (this.level().isClientSide) return;

        this.spawnAtLocation(createApocalypseElytra());

        spawnStackedLoot(Items.DIAMOND_BLOCK, 128);
        spawnStackedLoot(Items.ENCHANTED_GOLDEN_APPLE, 128);
        spawnStackedLoot(Items.NETHER_STAR, 64);
        spawnStackedLoot(Items.NETHERITE_BLOCK, 128);
    }

    /**
     * 🪽 専用エリトラ: Protection 100 + Unbreakable.
     * <p>
     * Protection 100 is far above the vanilla cap, so it is applied via the
     * raw NBT enchantment list ({@code EnchantedBookItem}-style) rather than
     * {@code ItemStack#enchant}, which would clamp it.
     */
    public static ItemStack createApocalypseElytra() {
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        elytra.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 100);
        elytra.getOrCreateTag().putBoolean("Unbreakable", true);
        elytra.setHoverName(Component.translatable("item.sevenheadeddragon.apocalypse_elytra")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        return elytra;
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
    // Sounds
    // ------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TurnTimer", this.turnTimer);
        tag.putBoolean("PlayerTurn", isPlayerTurn());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TurnTimer")) {
            this.turnTimer = tag.getInt("TurnTimer");
        }
        if (tag.contains("PlayerTurn")) {
            setPlayerTurn(tag.getBoolean("PlayerTurn"));
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<ApocalypseSevenHeadedRedDragonEntity> state) {
        switch (getActionState()) {
            case ACTION_BITE -> state.getController().setAnimation(ANIM_BITES[getBiteIndex()]);
            case ACTION_CLAW -> state.getController().setAnimation(ANIM_CLAW);
            case ACTION_CHARGE -> state.getController().setAnimation(ANIM_CHARGE);
            case ACTION_TAIL -> state.getController().setAnimation(ANIM_TAIL);
            case ACTION_FLY_START -> state.getController().setAnimation(ANIM_FLY_START);
            case ACTION_FLY -> state.getController().setAnimation(ANIM_FLY);
            case ACTION_FLY_END -> state.getController().setAnimation(ANIM_FLY_END);
            default -> {
                if (this.isHovering()) {
                    state.getController().setAnimation(ANIM_FLY);
                } else {
                    state.getController().setAnimation(ANIM_IDLE);
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
    // Movement AI
    // ------------------------------------------------------------------

    /**
     * Walks the dragon into melee range during its own turn, and gets out of
     * the way entirely while a charge attack or the flying magic mode is
     * driving movement directly.
     */
    private static class DragonApproachGoal extends Goal {
        private final ApocalypseSevenHeadedRedDragonEntity dragon;
        private final double speedModifier;
        private final float preferredDistance;

        DragonApproachGoal(ApocalypseSevenHeadedRedDragonEntity dragon, double speedModifier, float preferredDistance) {
            this.dragon = dragon;
            this.speedModifier = speedModifier;
            this.preferredDistance = preferredDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.dragon.getTarget();
            return target != null && target.isAlive()
                    && !this.dragon.isPlayerTurn()
                    && !this.dragon.isCharging()
                    && !this.dragon.isHovering()
                    && this.dragon.getActionState() != ACTION_BITE
                    && this.dragon.getActionState() != ACTION_CLAW;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            this.dragon.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.dragon.getTarget();
            if (target == null) return;

            this.dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

            double distanceSq = this.dragon.distanceToSqr(target);
            if (distanceSq > this.preferredDistance * this.preferredDistance) {
                if (this.dragon.getNavigation().isDone()) {
                    this.dragon.getNavigation().moveTo(target, this.speedModifier);
                }
            } else if (!this.dragon.getNavigation().isDone()) {
                this.dragon.getNavigation().stop();
            }
        }
    }
}
