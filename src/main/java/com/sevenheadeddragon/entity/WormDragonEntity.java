package com.sevenheadeddragon.entity;

import com.sevenheadeddragon.network.ModNetworking;
import com.sevenheadeddragon.network.ScreenShakePacket;
import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.registry.ModPotions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

/** 地中の竜王（ワームドラゴン）—赤い竜が造った対神生物兵器。 */
public class WormDragonEntity extends Monster implements GeoEntity {
    public static final int BOSS_TURN_TICKS = 20 * 60;
    public static final int PLAYER_TURN_TICKS = 20 * 5;
    public static final int MAX_MINIONS = 30;
    private static final int QUAKE_RADIUS = 100;
    private static final EntityDataAccessor<Boolean> PLAYER_TURN = SynchedEntityData.defineId(WormDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BITING = SynchedEntityData.defineId(WormDragonEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final WormDragonPart[] parts;
    private int turnTimer = BOSS_TURN_TICKS;
    private int attackTimer = 40;
    private int quakeTicks;
    private int biteTicks;

    public WormDragonEntity(EntityType<? extends WormDragonEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(5.0F);
        xpReward = 5000;
        this.noCulling = true;

        // 13 parts matching the 13 upright bones of the model (neck11..neck5, head, jaw)
        // Body segments: 40m x 20m. Head: 64m x 30m. Jaw: 48m x 20m.
        this.parts = new WormDragonPart[13];
        this.parts[0] = new WormDragonPart(this, "neck11", 40.0F, 20.0F);
        String[] neckNames = {"neck10", "neck9", "neck8", "neck7", "neck6", "neck1", "neck2", "neck3", "neck4", "neck5"};
        for (int i = 0; i < 10; i++) {
            this.parts[1 + i] = new WormDragonPart(this, neckNames[i], 40.0F, 20.0F);
        }
        this.parts[11] = new WormDragonPart(this, "head", 64.0F, 30.0F);
        this.parts[12] = new WormDragonPart(this, "jaw", 48.0F, 20.0F);
        positionParts();
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

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.ATTACK_DAMAGE, 40.0D);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PLAYER_TURN, false);
        entityData.define(BITING, false);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 128.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isPlayerTurn() { return entityData.get(PLAYER_TURN); }

    @Override public void aiStep() {
        super.aiStep();
        positionParts();
        if (level().isClientSide) return;
        bossEvent.setProgress(getHealth() / getMaxHealth());
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);

        LivingEntity target = getTarget();
        boolean hasValidTarget = target != null && target.isAlive()
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));

        if (biteTicks > 0 && --biteTicks == 0) entityData.set(BITING, false);
        if (quakeTicks > 0) tickQuake();

        if (!hasValidTarget) {
            if (isPlayerTurn()) {
                entityData.set(PLAYER_TURN, false);
            }
            turnTimer = BOSS_TURN_TICKS;
            attackTimer = 40;
            return;
        }

        if (!isPlayerTurn() && quakeTicks == 0 && biteTicks == 0 && --attackTimer <= 0) {
            attackTimer = 60 + random.nextInt(61);
            switch (random.nextInt(3)) {
                case 0 -> startQuake();
                case 1 -> bite();
                default -> summonMinions();
            }
        }
        if (--turnTimer <= 0) switchTurn();
    }

    private void switchTurn() {
        boolean playerTurn = !isPlayerTurn();
        entityData.set(PLAYER_TURN, playerTurn);
        turnTimer = playerTurn ? PLAYER_TURN_TICKS : BOSS_TURN_TICKS;
        quakeTicks = 0;
        biteTicks = 0;
        entityData.set(BITING, false);
        if (playerTurn) {
            for (ServerPlayer player : bossEvent.getPlayers()) {
                if (!player.isCreative() && !player.isSpectator()) {
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("YOUR TURN").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                    player.playNotifySound(com.sevenheadeddragon.registry.ModSounds.YOUR_TURN.get(), SoundSource.MASTER, 1.0F, 1.0F);
                }
            }
        }
    }

    private void startQuake() {
        quakeTicks = 200;
        level().playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 6.0F, 0.5F);
    }

    private void tickQuake() {
        quakeTicks--;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        // A dense, 100-block-wide warning carpet without scanning/changing terrain.
        for (int i = 0; i < 800; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * QUAKE_RADIUS;
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            double y = serverLevel.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new net.minecraft.core.BlockPos((int) x, (int) getY(), (int) z)).getY() + 0.1D;
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 1, 0, 0.05D, 0, 0);
        }
        if (quakeTicks % 10 == 0) {
            ScreenShakePacket packet = new ScreenShakePacket(3.0F, 12);
            for (ServerPlayer player : serverLevel.players()) {
                player.playNotifySound(com.sevenheadeddragon.registry.ModSounds.CENTIPEDE_WALK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                if (player.distanceTo(this) <= QUAKE_RADIUS + 32.0D) {
                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                }
            }
            AABB area = getBoundingBox().inflate(QUAKE_RADIUS, 32.0D, QUAKE_RADIUS);
            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && e != this && !(e instanceof Mob mob && mob.getPersistentData().getBoolean("WormDragonMinion")))) {
                if (victim.distanceToSqr(this) <= QUAKE_RADIUS * QUAKE_RADIUS) victim.hurt(damageSources().mobAttack(this), 10.0F);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(10000.0D);
    }

    private void positionParts() {
        if (this.parts == null) return;
        double rootX = getX();
        double rootY = getY();
        double rootZ = getZ();
        float yawRad = (float) Math.toRadians(getYRot());
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);
        float time = (float) this.tickCount * 0.05F;

        boolean isBiting = entityData.get(BITING) || biteTicks > 0;
        double biteProgress = isBiting ? Math.sin((25 - Math.max(0, biteTicks)) / 25.0D * Math.PI) : 0.0D;

        // 1. Vertical Spine Tower (parts 0..10 for neck11..neck5) rising from rootY up to rootY + 120
        for (int i = 0; i < 11; i++) {
            double swayFactor = i * 0.25D;
            double swayX = Math.sin(time + i * 0.3D) * swayFactor;
            double swayZ = Math.cos(time + i * 0.3D) * swayFactor;
            double px = rootX + swayX;
            double py = rootY + i * 12.0D;
            double pz = rootZ + swayZ;
            this.parts[i].updatePosWithOld(px, py, pz);
        }

        // 2. Head (part 11) & Jaw (part 12)
        // During idle: sit vertically atop the pillar at Y+132, Y+122.
        // During biting lunge: arcs forward and down along look vector to ground (Y+4.0D), then arcs back!
        double headLunge = biteProgress * 30.0D;
        double headDrop = biteProgress * 125.0D;

        double hx = rootX - sinYaw * headLunge;
        double hy = rootY + 132.0D - headDrop;
        double hz = rootZ + cosYaw * headLunge;
        this.parts[11].updatePosWithOld(hx, hy, hz);

        double jx = rootX - sinYaw * (headLunge + 2.0D);
        double jy = rootY + 122.0D - headDrop - 4.0D;
        double jz = rootZ + cosYaw * (headLunge + 2.0D);
        this.parts[12].updatePosWithOld(jx, jy, jz);
    }

    private void bite() {
        entityData.set(BITING, true);
        biteTicks = 25;
        Vec3 look = getLookAngle();
        Vec3 center = position().add(look.x * 25.0D, 4.0D, look.z * 25.0D);
        AABB area = new AABB(center, center).inflate(25.0D, 16.0D, 25.0D);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
            victim.hurt(damageSources().mobAttack(this), 40.0F);
            victim.knockback(3.0D, getX() - victim.getX(), getZ() - victim.getZ());
        }
        level().playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5.0F, 0.65F);
    }

    private void summonMinions() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        List<Mob> currentMinions = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(160),
                m -> m.isAlive() && m.getPersistentData().hasUUID("WormDragonOwner")
                        && m.getPersistentData().getUUID("WormDragonOwner").equals(getUUID()));
        int canSpawnCount = MAX_MINIONS - currentMinions.size();
        if (canSpawnCount <= 0) return;

        List<EntityType<? extends Mob>> types = List.of(EntityType.SKELETON, EntityType.DROWNED, EntityType.SHULKER,
                EntityType.LLAMA, EntityType.GHAST, EntityType.BLAZE, EntityType.PILLAGER);
        int spawnAmount = Math.min(canSpawnCount, types.size());
        for (int i = 0; i < spawnAmount; i++) {
            Mob minion = types.get(i).create(serverLevel);
            if (minion == null) continue;
            double angle = i * Math.PI * 2.0D / spawnAmount;
            minion.moveTo(getX() + Math.cos(angle) * 20.0D, getY() + 3.0D, getZ() + Math.sin(angle) * 20.0D, 0, 0);
            minion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(minion.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            if (minion instanceof net.minecraft.world.entity.monster.PatrollingMonster patrolling) {
                patrolling.setPatrolLeader(false);
            }
            minion.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
            minion.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD, 0.0F);
            minion.getPersistentData().putBoolean("WormDragonMinion", true);
            minion.getPersistentData().putUUID("WormDragonOwner", getUUID());
            minion.setPersistenceRequired();
            if (getTarget() != null) minion.setTarget(getTarget());
            serverLevel.addFreshEntity(minion);
        }
        serverLevel.sendParticles(ParticleTypes.WITCH, getX(), getY() + 5, getZ(), 100, 15, 6, 15, 0.1D);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (!isPlayerTurn()) {
            if (!level().isClientSide) level().playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1, 0.6F);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override public boolean doHurtTarget(Entity target) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity entity) {}
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossEvent.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossEvent.removePlayer(player); }
    @Override public void setCustomName(@Nullable Component name) { super.setCustomName(name); bossEvent.setName(getDisplayName()); }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.ENCHANTMENT_BLESSING.get());
        for (int i = 0; i < 5; i++) spawnAtLocation(potion.copy());
        spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 20));
    }

    @Override public void die(DamageSource source) {
        if (!level().isClientSide) {
            level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(160), m -> m.getPersistentData().hasUUID("WormDragonOwner")
                    && m.getPersistentData().getUUID("WormDragonOwner").equals(getUUID())).forEach(Entity::discard);
        }
        super.die(source);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("TurnTimer", turnTimer); tag.putBoolean("PlayerTurn", isPlayerTurn());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); if (tag.contains("TurnTimer")) turnTimer = tag.getInt("TurnTimer"); entityData.set(PLAYER_TURN, tag.getBoolean("PlayerTurn"));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> {
            state.getController().setAnimation(entityData.get(BITING)
                    ? RawAnimation.begin().thenPlay("animation.wormdragon.attack").thenLoop("animation.wormdragon.idle")
                    : RawAnimation.begin().thenLoop("animation.wormdragon.idle"));
            return PlayState.CONTINUE;
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
