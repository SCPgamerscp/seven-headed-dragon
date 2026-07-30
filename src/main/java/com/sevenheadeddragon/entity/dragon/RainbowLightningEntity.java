package com.sevenheadeddragon.entity.dragon;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * 七色の雷 (Rainbow Lightning) - one bolt of the Shocker Breaker barrage.
 * <p>
 * A faithful recreation of the ショッカー・ブレイカー from the Asriel fight in
 * <i>UNDERTALE</i>: a vertical pillar of coloured lightning that strikes a
 * fixed spot, always preceded by a same-coloured 魔法陣 warning on the ground
 * {@code 0.5s} earlier (spawned by the attack pattern, not by this entity), so
 * every single bolt is dodgeable if the player reads the telegraph.
 * <p>
 * This is deliberately <em>not</em> a vanilla {@code LightningBolt}: vanilla
 * bolts set fires, convert mobs (pig → zoglin, creeper → charged), can be
 * redirected by lightning rods, and cannot be tinted. This entity instead
 * renders a coloured beam and deals exactly {@link #DAMAGE} once to everything
 * inside a thin vertical column, which is what the "各雷 20ダメージ" spec needs.
 * <p>
 * The seven colours cycle through {@link #RAINBOW}, matching the seven heads
 * of the dragon and the seven bolts of the final homing sequence.
 */
public class RainbowLightningEntity extends Entity {

    /** Damage per bolt, per spec ("各雷 20ダメージ"). */
    public static final float DAMAGE = 20.0F;

    /** How long the visual beam persists after striking. */
    public static final int STRIKE_LIFETIME_TICKS = 12;

    /** Beam height in blocks - tall enough to read as "from the sky". */
    public static final float BEAM_HEIGHT = 48.0F;

    /** Horizontal hit radius of the column. */
    private static final double HIT_RADIUS = 1.1D;

    /** The seven colours of 七色の雷, in strike order. */
    public static final int[] RAINBOW = {
            0xFF0000, // 赤
            0xFF7F00, // 橙
            0xFFFF00, // 黄
            0x00CC22, // 緑
            0x0099FF, // 青
            0x3300CC, // 藍
            0x8B00FF, // 紫
    };

    /** Returns the colour for bolt index {@code i}, wrapping every 7 bolts. */
    public static int colorFor(int i) {
        return RAINBOW[Math.floorMod(i, RAINBOW.length)];
    }

    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(RainbowLightningEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(RainbowLightningEntity.class, EntityDataSerializers.INT);

    /** Everything already zapped by this bolt, so a lingering beam cannot multi-hit. */
    private final Set<Entity> alreadyHit = new HashSet<>();

    @Nullable
    private LivingEntity caster;

    private boolean hasStruck;

    public RainbowLightningEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_COLOR, RAINBOW[0]);
        this.entityData.define(DATA_LIFE, STRIKE_LIFETIME_TICKS);
    }

    public void setColor(int rgb) {
        this.entityData.set(DATA_COLOR, rgb);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public void setCaster(@Nullable LivingEntity caster) {
        this.caster = caster;
    }

    /**
     * Spawns and immediately strikes a coloured bolt at the given position.
     *
     * @param level  server level
     * @param x      strike column centre X
     * @param y      ground level Y of the strike
     * @param z      strike column centre Z
     * @param rgb    packed 0xRRGGBB bolt colour
     * @param caster the dragon, credited for the damage
     */
    public static RainbowLightningEntity strike(ServerLevel level, double x, double y, double z,
                                                int rgb, @Nullable LivingEntity caster) {
        RainbowLightningEntity bolt =
                com.sevenheadeddragon.registry.ModEntities.RAINBOW_LIGHTNING.get().create(level);
        if (bolt == null) return null;
        bolt.moveTo(x, y, z, 0.0F, 0.0F);
        bolt.setColor(rgb);
        bolt.setCaster(caster);
        level.addFreshEntity(bolt);
        return bolt;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            spawnClientBeamParticles();
            return;
        }

        // Damage lands on the very first tick, so the bolt is instantaneous
        // (as in Undertale) rather than a lingering damage-over-time column.
        if (!this.hasStruck) {
            this.hasStruck = true;
            playStrikeSound();
            damageColumn();
        }

        int remaining = this.entityData.get(DATA_LIFE) - 1;
        if (remaining <= 0) {
            this.discard();
        } else {
            this.entityData.set(DATA_LIFE, remaining);
        }
    }

    private void playStrikeSound() {
        this.level().playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.HOSTILE, 5.0F, 0.8F + this.random.nextFloat() * 0.4F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.HOSTILE, 2.5F, 0.9F + this.random.nextFloat() * 0.4F);
    }

    /** Applies {@link #DAMAGE} once to every living entity in the vertical column. */
    private void damageColumn() {
        AABB column = new AABB(
                this.getX() - HIT_RADIUS, this.getY() - 2.0D, this.getZ() - HIT_RADIUS,
                this.getX() + HIT_RADIUS, this.getY() + BEAM_HEIGHT, this.getZ() + HIT_RADIUS);

        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, column,
                e -> e.isAlive() && !(e instanceof ApocalypseSevenHeadedRedDragonEntity))) {
            if (!this.alreadyHit.add(victim)) continue;
            victim.hurt(ModDamageTypes.source(victim, ModDamageTypes.RAINBOW_LIGHTNING, this, this.caster), DAMAGE);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            spawnServerStrikeParticles(serverLevel);
        }
    }

    private void spawnServerStrikeParticles(ServerLevel serverLevel) {
        int rgb = getColor();
        Vector3f color = new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F);
        DustParticleOptions dust = new DustParticleOptions(color, 2.4F);

        // A dense coloured pillar plus a ground burst, so the bolt's colour is
        // unmistakable even for players without the custom renderer in view.
        for (double dy = 0.0D; dy < BEAM_HEIGHT; dy += 0.5D) {
            serverLevel.sendParticles(dust,
                    this.getX(), this.getY() + dy, this.getZ(),
                    2, 0.18D, 0.2D, 0.18D, 0.0D);
        }
        serverLevel.sendParticles(dust, this.getX(), this.getY() + 0.2D, this.getZ(),
                40, 1.0D, 0.3D, 1.0D, 0.05D);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.2D, this.getZ(),
                30, 0.8D, 0.4D, 0.8D, 0.4D);
    }

    private void spawnClientBeamParticles() {
        int rgb = getColor();
        Vector3f color = new Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F);
        DustParticleOptions dust = new DustParticleOptions(color, 1.6F);
        for (int i = 0; i < 4; i++) {
            double dy = this.random.nextDouble() * BEAM_HEIGHT;
            this.level().addParticle(dust,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.6D,
                    this.getY() + dy,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.6D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Transient combat effect - never persisted.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Transient combat effect - never persisted.
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
        return distanceSqr < 160.0D * 160.0D;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(
                this.getX() - 2.0D, this.getY() - 1.0D, this.getZ() - 2.0D,
                this.getX() + 2.0D, this.getY() + BEAM_HEIGHT, this.getZ() + 2.0D);
    }
}
