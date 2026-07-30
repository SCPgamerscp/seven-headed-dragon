package com.sevenheadeddragon.entity.boss;

import com.sevenheadeddragon.entity.dragon.ApocalypseSevenHeadedRedDragonEntity;
import com.sevenheadeddragon.entity.dragon.DebilitationMartyrEntity;
import com.sevenheadeddragon.entity.dragon.DragonMagicCircleEntity;
import com.sevenheadeddragon.entity.dragon.GoatMissileEntity;
import com.sevenheadeddragon.entity.dragon.LonginusSpearEntity;
import com.sevenheadeddragon.entity.dragon.RainbowLightningEntity;
import com.sevenheadeddragon.entity.dragon.SquidMissileEntity;
import com.sevenheadeddragon.entity.dragon.TimedGimmickCreeperEntity;
import com.sevenheadeddragon.network.ModNetworking;
import com.sevenheadeddragon.network.ScreenShakePacket;
import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives every attack the 終末の七つ頭の赤い竜 performs.
 * <p>
 * During the boss's 60-second turn this manager is called repeatedly with no
 * cooldown: each pattern signals completion via
 * {@link ApocalypseSevenHeadedRedDragonEntity#onPatternFinished()}, at which
 * point the next random pattern begins on the very next tick. The result is the
 * spec's "ランダムかつ切れ目なく怒涛の勢い" - a continuous, gapless torrent.
 * <p>
 * The eight patterns:
 * <ol>
 *   <li>{@link #startSevenBiteCombo 7連噛みつき} - seven bites, seven sins</li>
 *   <li>{@link #startGoatMachineGun 山羊ミサイル} - 30-round machine gun</li>
 *   <li>{@link #startSquidVolley イカミサイル} - 5 homing missiles</li>
 *   <li>{@link #startLonginusSpears ロンギヌスの槍} - 50+ spear cross-drop</li>
 *   <li>{@link #startRainbowLightning 七色の雷} - the Shocker Breaker</li>
 *   <li>{@link #startCreeperGimmick クリーパーギミック} - 7 timed bombs</li>
 *   <li>{@link #startMartyrSummon 衰弱の殉教者} - 7 wither-cloud summons</li>
 *   <li>{@link #startFlyingMagicMode 飛行・魔法詠唱} - airborne 5x magic combo</li>
 *   <li>{@link #startClawCharge 爪→突進} and {@link #startTailSwipe 尻尾} - melee</li>
 * </ol>
 */
public final class RedDragonAttackPatternManager {

    private RedDragonAttackPatternManager() {}

    /** Standard damage figure used by nearly every attack, per spec ("各20ダメージ"). */
    public static final float STANDARD_DAMAGE = 20.0F;

    /** Duration of each of the seven sin debuffs (20 seconds). */
    public static final int SIN_DURATION_TICKS = 20 * 20;

    public static void startRandomAttack(ApocalypseSevenHeadedRedDragonEntity dragon) {
        LivingEntity target = dragon.getFocusedTarget();
        if (target == null || !target.isAlive()) {
            dragon.scheduleIn(10, dragon::onPatternFinished);
            return;
        }

        if (dragon.isHovering()) {
            // 空中魔法フェーズ（後半30秒）: 上空10ブロックに浮遊したまま魔法6種を連打
            startRandomMagicAttack(dragon, target, dragon::onPatternFinished);
        } else {
            // 地上物理フェーズ（前半30秒）: 地上で近接・物理攻撃3種を実行
            startRandomGroundAttack(dragon, target);
        }
    }

    private static void startRandomGroundAttack(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        switch (dragon.getRandom().nextInt(3)) {
            case 0 -> startSevenBiteCombo(dragon, target);
            case 1 -> startClawCharge(dragon, target);
            default -> startTailSwipe(dragon, target);
        }
    }

    /**
     * 空中魔法・召喚攻撃6種（前半30秒の地上戦が終わり、上空へ浮上した後に連打）
     */
    private static void startRandomMagicAttack(ApocalypseSevenHeadedRedDragonEntity dragon,
                                                LivingEntity target, Runnable onComplete) {
        switch (dragon.getRandom().nextInt(6)) {
            case 0 -> runMartyrSummon(dragon, target, onComplete);
            case 1 -> runGoatMachineGun(dragon, target, onComplete);
            case 2 -> runSquidVolley(dragon, target, onComplete);
            case 3 -> runLonginusSpears(dragon, target, onComplete);
            case 4 -> runRainbowLightning(dragon, target, onComplete);
            default -> runCreeperGimmick(dragon, target, onComplete);
        }
    }

    // ==================================================================
    // ① 7連噛みつき攻撃（七つの大罪デバフ）
    // ==================================================================

    /** Ticks between consecutive bites - fast enough to feel like one combo. */
    private static final int BITE_INTERVAL_TICKS = 16;

    /** Reach of each bite. */
    private static final double BITE_REACH = 9.0D;

    /**
     * Seven consecutive bites using attack_bite_1 … attack_bite_7 in order.
     * Each head deals {@value #STANDARD_DAMAGE} damage and brands the victim
     * with its own deadly sin ({@code 攻撃力 -50% ＆ 移動速度 -50%}). Because all
     * seven sins are distinct effects they stack, so eating the full combo
     * leaves the player at a quarter of their offensive output and crawling.
     */
    private static void startSevenBiteCombo(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        dragon.getNavigation().stop();
        dragon.setDeltaMovement(0.0D, dragon.getDeltaMovement().y, 0.0D);
        biteStep(dragon, target, 0);
    }

    private static void biteStep(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target, int index) {
        if (!dragon.isAlive() || dragon.isPlayerTurn() || index >= 7) {
            dragon.onPatternFinished();
            return;
        }

        dragon.getNavigation().stop();
        dragon.setDeltaMovement(0.0D, dragon.getDeltaMovement().y, 0.0D);
        dragon.playBite(index);
        faceTarget(dragon, target);
        dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 1.6F, 1.1F + index * 0.06F);

        // The damage lands a few ticks into the animation, on the "snap".
        dragon.scheduleIn(4, () -> {
            if (!dragon.isAlive() || target == null || !target.isAlive()) return;
            if (dragon.distanceTo(target) <= BITE_REACH) {
                target.hurt(dragon.damageSources().mobAttack(dragon), STANDARD_DAMAGE);
                applySin(target, index, dragon);
            }
        });

        dragon.scheduleIn(BITE_INTERVAL_TICKS, () -> biteStep(dragon, target, index + 1));
    }

    /** Applies the {@code index}-th deadly sin (0 = Pride … 6 = Lust). */
    private static void applySin(LivingEntity target, int index, LivingEntity source) {
        var sinEffect = ModEffects.SEVEN_SINS.get(Math.floorMod(index, 7)).get();
        target.addEffect(new MobEffectInstance(sinEffect, SIN_DURATION_TICKS, 0, false, true, true), source);
    }

    // ==================================================================
    // ② 山羊ミサイル - マシンガン 30連射
    // ==================================================================

    private static final int GOAT_MISSILE_COUNT = 30;
    private static final int GOAT_FIRE_INTERVAL_TICKS = 2;
    private static final double GOAT_SPREAD = 0.10D;

    private static void startGoatMachineGun(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        runGoatMachineGun(dragon, target, dragon::onPatternFinished);
    }

    /**
     * Thirty goats fired at ~10 rounds/second with a slight spread, so the
     * stream sweeps rather than laser-striking one point. Each detonates for
     * power 4 without harming terrain.
     */
    private static void runGoatMachineGun(ApocalypseSevenHeadedRedDragonEntity dragon,
                                          LivingEntity target, Runnable onComplete) {
        goatStep(dragon, target, 0, onComplete);
    }

    private static void goatStep(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target,
                                 int fired, Runnable onComplete) {
        if (!dragon.isAlive() || dragon.isPlayerTurn() || fired >= GOAT_MISSILE_COUNT
                || target == null || !target.isAlive()) {
            onComplete.run();
            return;
        }

        if (dragon.level() instanceof ServerLevel serverLevel) {
            faceTarget(dragon, target);
            Vec3 muzzle = muzzlePosition(dragon);
            Vec3 aim = new Vec3(target.getX(), target.getY(0.6D), target.getZ());

            GoatMissileEntity goat = new GoatMissileEntity(serverLevel, dragon);
            goat.aimAt(muzzle, aim, GOAT_SPREAD);
            serverLevel.addFreshEntity(goat);

            serverLevel.playSound(null, dragon.blockPosition(), SoundEvents.GOAT_SCREAMING_AMBIENT,
                    SoundSource.HOSTILE, 1.0F, 1.5F);
        }

        dragon.scheduleIn(GOAT_FIRE_INTERVAL_TICKS,
                () -> goatStep(dragon, target, fired + 1, onComplete));
    }

    // ==================================================================
    // ② Squid Missile - 追尾型ホーミング 5一斉発射
    // ==================================================================

    private static final int SQUID_MISSILE_COUNT = 5;

    private static void startSquidVolley(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        runSquidVolley(dragon, target, dragon::onPatternFinished);
    }

    /**
     * A simultaneous 5-missile volley, fanned across a 90-degree arc so the
     * squids converge on the player from visibly different angles instead of
     * arriving as one clump.
     */
    private static void runSquidVolley(ApocalypseSevenHeadedRedDragonEntity dragon,
                                       LivingEntity target, Runnable onComplete) {
        if (!dragon.isAlive() || target == null || !target.isAlive()
                || !(dragon.level() instanceof ServerLevel serverLevel)) {
            onComplete.run();
            return;
        }

        faceTarget(dragon, target);
        Vec3 muzzle = muzzlePosition(dragon);
        Vec3 baseDir = new Vec3(target.getX() - muzzle.x, target.getY(0.6D) - muzzle.y, target.getZ() - muzzle.z)
                .normalize();

        for (int i = 0; i < SQUID_MISSILE_COUNT; i++) {
            // Fan evenly from -45 deg to +45 deg around the aim direction.
            double spreadAngle = Math.toRadians(-45.0D + (90.0D / (SQUID_MISSILE_COUNT - 1)) * i);
            double cos = Math.cos(spreadAngle);
            double sin = Math.sin(spreadAngle);
            Vec3 fanned = new Vec3(
                    baseDir.x * cos - baseDir.z * sin,
                    baseDir.y + 0.25D,
                    baseDir.x * sin + baseDir.z * cos).normalize();

            SquidMissileEntity squid = new SquidMissileEntity(serverLevel, dragon);
            squid.setHomingTarget(target);
            squid.launch(muzzle, fanned);
            serverLevel.addFreshEntity(squid);
        }

        serverLevel.playSound(null, dragon.blockPosition(), SoundEvents.SQUID_SQUIRT,
                SoundSource.HOSTILE, 2.0F, 0.7F);

        // Let the volley fly for a while before starting another pattern, so
        // missiles are still in the air and genuinely threatening.
        dragon.scheduleIn(40, onComplete);
    }

    // ==================================================================
    // ③ ロンギヌスの槍（Longinus Spear）＆「神殺し」デバフ
    // ==================================================================

    /** Arm length of the cross in each of the four directions. */
    private static final int LONGINUS_CROSS_ARM = 10;

    /** Height above the target the spears spawn at. */
    private static final double LONGINUS_DROP_HEIGHT = 30.0D;

    /** Telegraph time before the spears fall. */
    private static final int LONGINUS_TELEGRAPH_TICKS = 25;

    private static void startLonginusSpears(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        runLonginusSpears(dragon, target, dragon::onPatternFinished);
    }

    /**
     * Builds the cross (＋) footprint - a full 20-block span on both axes -
     * shows a big golden 魔法陣 on the ground, then drops a spear on every
     * cell of the cross simultaneously. With arms of 10 in four directions
     * plus the centre this is 41 cells; the two axes are additionally
     * double-rowed so the total comfortably exceeds the spec's "50本以上".
     */
    private static void runLonginusSpears(ApocalypseSevenHeadedRedDragonEntity dragon,
                                          LivingEntity target, Runnable onComplete) {
        if (!dragon.isAlive() || target == null || !target.isAlive()
                || !(dragon.level() instanceof ServerLevel serverLevel)) {
            onComplete.run();
            return;
        }

        final Vec3 center = target.position();
        List<Vec3> spearSpots = buildCrossPattern(center);

        // 魔法陣: one large golden circle marking the whole strike zone.
        DragonMagicCircleEntity.spawn(serverLevel, center.x, groundY(serverLevel, center) + 0.05D, center.z,
                0xFFD700, LONGINUS_CROSS_ARM * 2.0F + 2.0F, LONGINUS_TELEGRAPH_TICKS + 20);

        // Smaller circles marking each individual impact point, so the safe
        // diagonal gaps in the cross are unmistakable.
        for (Vec3 spot : spearSpots) {
            DragonMagicCircleEntity.spawn(serverLevel, spot.x, groundY(serverLevel, spot) + 0.06D, spot.z,
                    0xFFE873, 1.6F, LONGINUS_TELEGRAPH_TICKS + 6);
        }

        serverLevel.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 3.0F, 0.6F);
        dragon.setActionState(ApocalypseSevenHeadedRedDragonEntity.ACTION_IDLE);

        dragon.scheduleIn(LONGINUS_TELEGRAPH_TICKS, () -> {
            if (!dragon.isAlive()) {
                onComplete.run();
                return;
            }
            for (Vec3 spot : spearSpots) {
                LonginusSpearEntity spear = new LonginusSpearEntity(serverLevel, dragon);
                spear.dropFrom(spot.x, groundY(serverLevel, spot) + LONGINUS_DROP_HEIGHT, spot.z);
                serverLevel.addFreshEntity(spear);
            }
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THROW,
                    SoundSource.HOSTILE, 3.0F, 0.5F);
            shakeNearbyScreens(serverLevel, dragon, 3.0F, 10);

            // Let them land before yielding to the next pattern.
            dragon.scheduleIn(30, onComplete);
        });
    }

    /**
     * The cross footprint: two perpendicular 20-block bars through the centre,
     * each two cells wide, giving 50+ impact points with clear diagonal
     * escape routes.
     */
    private static List<Vec3> buildCrossPattern(Vec3 center) {
        List<Vec3> spots = new ArrayList<>();
        spots.add(center);
        for (int d = 1; d <= LONGINUS_CROSS_ARM; d++) {
            // North-south bar (two rows wide).
            spots.add(new Vec3(center.x, center.y, center.z - d));
            spots.add(new Vec3(center.x, center.y, center.z + d));
            spots.add(new Vec3(center.x + 1.0D, center.y, center.z - d));
            spots.add(new Vec3(center.x + 1.0D, center.y, center.z + d));
            // East-west bar (two rows wide).
            spots.add(new Vec3(center.x - d, center.y, center.z));
            spots.add(new Vec3(center.x + d, center.y, center.z));
            spots.add(new Vec3(center.x - d, center.y, center.z + 1.0D));
            spots.add(new Vec3(center.x + d, center.y, center.z + 1.0D));
        }
        return spots;
    }

    // ==================================================================
    // ④ 七色の雷 (Rainbow Lightning - Shocker Breaker)
    // ==================================================================

    /** Rows of lightning in front of and behind the player: 5 rows. */
    private static final int LIGHTNING_ROWS = 5;
    /** Bolts per row: one per rainbow colour. */
    private static final int LIGHTNING_PER_ROW = 7;
    /** Spacing between rows, in blocks. */
    private static final double LIGHTNING_ROW_SPACING = 3.0D;
    /** Spacing between bolts within a row, in blocks. */
    private static final double LIGHTNING_COLUMN_SPACING = 3.0D;

    /** 魔法陣 warning appears this many ticks before the bolt (0.5s). */
    private static final int LIGHTNING_TELEGRAPH_TICKS = 10;
    /** Sweep interval within a sequence: 0.15s. */
    private static final int LIGHTNING_SWEEP_INTERVAL = 3;
    /** Homing finale interval: 0.25s. */
    private static final int LIGHTNING_HOMING_INTERVAL = 5;

    private static void startRainbowLightning(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        runRainbowLightning(dragon, target, dragon::onPatternFinished);
    }

    /**
     * The full three-part Shocker Breaker, recreating the Asriel fight beat for
     * beat:
     * <ol>
     *   <li><b>Sequence ①</b>: a 5-row × 7-colour wall spanning the player's
     *       front and back; all five rows fire together, sweeping
     *       <b>right → left</b> at 0.15s per column.</li>
     *   <li><b>Sequence ②</b>: the same wall sweeps back <b>left → right</b>.</li>
     *   <li><b>Sequence ③</b>: seven bolts chase the player's <em>current</em>
     *       feet position at 0.25s intervals - no longer avoidable by standing
     *       still, only by constant movement.</li>
     * </ol>
     * Every bolt is preceded by a same-coloured 魔法陣 exactly 0.5s earlier.
     */
    private static void runRainbowLightning(ApocalypseSevenHeadedRedDragonEntity dragon,
                                            LivingEntity target, Runnable onComplete) {
        if (!dragon.isAlive() || target == null || !target.isAlive()
                || !(dragon.level() instanceof ServerLevel serverLevel)) {
            onComplete.run();
            return;
        }

        // Lock the grid to the player's facing at cast time, so the wall really
        // does form "in front of and behind" them.
        final float yaw = target.getYRot();
        final Vec3 forward = Vec3.directionFromRotation(0.0F, yaw).normalize();
        final Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F).normalize();
        final Vec3 origin = target.position();

        serverLevel.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE, 3.0F, 1.4F);

        // Sequence ①: right -> left.
        int sequenceOneTicks = scheduleSweep(dragon, serverLevel, origin, forward, right, false, 0);

        // Sequence ②: left -> right, immediately after.
        int gap = 10;
        int sequenceTwoStart = sequenceOneTicks + gap;
        int sequenceTwoTicks = scheduleSweep(dragon, serverLevel, origin, forward, right, true, sequenceTwoStart);

        // Sequence ③: seven homing bolts under the player's feet.
        int homingStart = sequenceTwoStart + sequenceTwoTicks + gap;
        for (int i = 0; i < LIGHTNING_PER_ROW; i++) {
            final int boltIndex = i;
            int fireAt = homingStart + i * LIGHTNING_HOMING_INTERVAL;

            // Telegraph 0.5s before, at wherever the player is standing then.
            dragon.scheduleIn(Math.max(0, fireAt - LIGHTNING_TELEGRAPH_TICKS), () -> {
                if (!target.isAlive()) return;
                Vec3 spot = target.position();
                DragonMagicCircleEntity.spawn(serverLevel, spot.x, groundY(serverLevel, spot) + 0.06D, spot.z,
                        RainbowLightningEntity.colorFor(boltIndex), 3.2F, LIGHTNING_TELEGRAPH_TICKS + 4);
            });

            dragon.scheduleIn(fireAt, () -> {
                if (!dragon.isAlive() || !target.isAlive()) return;
                Vec3 spot = target.position();
                RainbowLightningEntity.strike(serverLevel, spot.x, groundY(serverLevel, spot), spot.z,
                        RainbowLightningEntity.colorFor(boltIndex), dragon);
            });
        }

        int total = homingStart + LIGHTNING_PER_ROW * LIGHTNING_HOMING_INTERVAL + 10;
        dragon.scheduleIn(total, onComplete);
    }

    /**
     * Schedules one directional sweep of the 5×7 wall.
     *
     * @param reverse   {@code false} = right → left, {@code true} = left → right
     * @param startTick offset from now at which the sweep begins
     * @return how many ticks the sweep occupies
     */
    private static int scheduleSweep(ApocalypseSevenHeadedRedDragonEntity dragon, ServerLevel serverLevel,
                                     Vec3 origin, Vec3 forward, Vec3 right, boolean reverse, int startTick) {
        // Rows are laid out symmetrically about the player: -2,-1,0,+1,+2
        // times the row spacing along their facing axis, i.e. both in front of
        // and behind them.
        for (int column = 0; column < LIGHTNING_PER_ROW; column++) {
            int columnOrder = reverse ? column : (LIGHTNING_PER_ROW - 1 - column);
            int fireAt = startTick + column * LIGHTNING_SWEEP_INTERVAL;
            int colorIndex = column;

            for (int row = 0; row < LIGHTNING_ROWS; row++) {
                double forwardOffset = (row - (LIGHTNING_ROWS - 1) / 2.0D) * LIGHTNING_ROW_SPACING;
                double rightOffset = (columnOrder - (LIGHTNING_PER_ROW - 1) / 2.0D) * LIGHTNING_COLUMN_SPACING;

                double x = origin.x + forward.x * forwardOffset + right.x * rightOffset;
                double z = origin.z + forward.z * forwardOffset + right.z * rightOffset;
                final Vec3 spot = new Vec3(x, origin.y, z);
                final int rgb = RainbowLightningEntity.colorFor(colorIndex);

                // 魔法陣事前警告: exactly 0.5 seconds before this bolt.
                dragon.scheduleIn(Math.max(0, fireAt - LIGHTNING_TELEGRAPH_TICKS), () -> {
                    if (!dragon.isAlive()) return;
                    DragonMagicCircleEntity.spawn(serverLevel, spot.x, groundY(serverLevel, spot) + 0.06D, spot.z,
                            rgb, 2.8F, LIGHTNING_TELEGRAPH_TICKS + 4);
                });

                dragon.scheduleIn(fireAt, () -> {
                    if (!dragon.isAlive()) return;
                    RainbowLightningEntity.strike(serverLevel, spot.x, groundY(serverLevel, spot), spot.z, rgb, dragon);
                });
            }
        }
        return LIGHTNING_PER_ROW * LIGHTNING_SWEEP_INTERVAL;
    }

    // ==================================================================
    // ⑤ クリーパー 10秒時限爆発ギミック
    // ==================================================================

    private static final int CREEPER_COUNT = 7;
    private static final double CREEPER_RING_RADIUS = 5.0D;

    private static void startCreeperGimmick(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        runCreeperGimmick(dragon, target, dragon::onPatternFinished);
    }

    /**
     * Rings the player with seven 10-second bombs. Unlike vanilla creepers
     * these cannot be defused by walking away - the only escape is killing all
     * seven inside the countdown.
     */
    private static void runCreeperGimmick(ApocalypseSevenHeadedRedDragonEntity dragon,
                                          LivingEntity target, Runnable onComplete) {
        if (!dragon.isAlive() || target == null || !target.isAlive()
                || !(dragon.level() instanceof ServerLevel serverLevel)) {
            onComplete.run();
            return;
        }

        for (int i = 0; i < CREEPER_COUNT; i++) {
            double angle = (Math.PI * 2.0D / CREEPER_COUNT) * i;
            double x = target.getX() + Math.cos(angle) * CREEPER_RING_RADIUS;
            double z = target.getZ() + Math.sin(angle) * CREEPER_RING_RADIUS;

            TimedGimmickCreeperEntity creeper = ModEntities.TIMED_GIMMICK_CREEPER.get().create(serverLevel);
            if (creeper == null) continue;
            creeper.moveTo(x, groundY(serverLevel, new Vec3(x, target.getY(), z)), z,
                    (float) Math.toDegrees(-angle), 0.0F);
            creeper.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(creeper.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null, null);
            creeper.setTarget(target);
            serverLevel.addFreshEntity(creeper);

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, creeper.getY() + 1.0D, z,
                    12, 0.4D, 0.5D, 0.4D, 0.02D);
        }

        serverLevel.playSound(null, target.blockPosition(), SoundEvents.CREEPER_PRIMED,
                SoundSource.HOSTILE, 3.0F, 0.6F);

        // Hand control back well before the fuse expires so the dragon keeps
        // pressuring the player while they scramble to clear the bombs.
        dragon.scheduleIn(30, onComplete);
    }

    // ==================================================================
    // ⑥ 召喚攻撃：「衰弱の殉教者」
    // ==================================================================

    private static final int MARTYR_COUNT = 7;
    private static final double MARTYR_SPAWN_RADIUS = 8.0D;

    /**
     * Summons seven martyrs at random positions around the player. Each emits
     * a persistent Wither cloud until killed, so ignoring them compounds
     * quickly with everything else the dragon is doing.
     */
    private static void runMartyrSummon(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target, Runnable onComplete) {
        if (!dragon.isAlive() || target == null || !target.isAlive()
                || !(dragon.level() instanceof ServerLevel serverLevel)) {
            onComplete.run();
            return;
        }

        for (int i = 0; i < MARTYR_COUNT; i++) {
            double angle = dragon.getRandom().nextDouble() * Math.PI * 2.0D;
            double dist = 3.0D + dragon.getRandom().nextDouble() * (MARTYR_SPAWN_RADIUS - 3.0D);
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;

            DebilitationMartyrEntity martyr = ModEntities.DEBILITATION_MARTYR.get().create(serverLevel);
            if (martyr == null) continue;
            martyr.moveTo(x, groundY(serverLevel, new Vec3(x, target.getY(), z)), z,
                    dragon.getRandom().nextFloat() * 360.0F, 0.0F);
            martyr.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(martyr.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null, null);
            martyr.setTarget(target);
            serverLevel.addFreshEntity(martyr);

            serverLevel.sendParticles(ParticleTypes.SOUL, x, martyr.getY() + 1.0D, z,
                    20, 0.4D, 0.8D, 0.4D, 0.05D);
        }

        serverLevel.playSound(null, dragon.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 2.0F, 1.4F);

        dragon.scheduleIn(30, onComplete);
    }

    // ==================================================================
    // ⑦ 飛行・魔法詠唱モード
    // ==================================================================

    /** Hover altitude above the ground, per spec ("上空10ブロック"). */
    private static final double FLY_ALTITUDE = 10.0D;
    /** fly.start animation length before the magic begins. */
    private static final int FLY_START_TICKS = 20;
    /** Magic repetitions while airborne, per spec ("5回繰り返し"). */
    private static final int FLY_MAGIC_REPEATS = 5;
    /** fly.end animation length after landing begins. */
    private static final int FLY_END_TICKS = 20;

    /**
     * The signature spellcasting sequence:
     * {@code fly.start} → rise 10 blocks → loop {@code fly} while casting five
     * consecutive magic attacks (missiles / spears / lightning / creepers) →
     * {@code fly.end} and land.
     */
    private static void startFlyingMagicMode(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        if (!dragon.isAlive() || target == null || !target.isAlive()) {
            dragon.onPatternFinished();
            return;
        }

        dragon.setActionState(ApocalypseSevenHeadedRedDragonEntity.ACTION_FLY_START);
        dragon.getNavigation().stop();
        dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.HOSTILE, 3.0F, 0.8F);

        dragon.scheduleIn(FLY_START_TICKS, () -> {
            if (!dragon.isAlive() || dragon.isPlayerTurn()) {
                dragon.stopHovering();
                dragon.onPatternFinished();
                return;
            }
            dragon.startHovering(dragon.getY() + FLY_ALTITUDE);
            dragon.setActionState(ApocalypseSevenHeadedRedDragonEntity.ACTION_FLY);
            flyingMagicStep(dragon, target, 0);
        });
    }

    private static void flyingMagicStep(ApocalypseSevenHeadedRedDragonEntity dragon,
                                        LivingEntity target, int castsDone) {
        if (!dragon.isAlive() || dragon.isPlayerTurn() || castsDone >= FLY_MAGIC_REPEATS
                || target == null || !target.isAlive()) {
            endFlight(dragon);
            return;
        }

        // Keep the fly loop playing and stay locked onto the target.
        dragon.setActionState(ApocalypseSevenHeadedRedDragonEntity.ACTION_FLY);
        faceTarget(dragon, target);

        startRandomMagicAttack(dragon, target, () -> flyingMagicStep(dragon, target, castsDone + 1));
    }

    private static void endFlight(ApocalypseSevenHeadedRedDragonEntity dragon) {
        dragon.setActionState(ApocalypseSevenHeadedRedDragonEntity.ACTION_FLY_END);
        dragon.stopHovering();
        dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.HOSTILE, 3.0F, 0.6F);
        dragon.scheduleIn(FLY_END_TICKS, dragon::onPatternFinished);
    }

    // ==================================================================
    // ⑧ 近接物理攻撃: claw x3 -> charge, and tail
    // ==================================================================

    /** Claw wind-ups before the charge, per spec ("3回連続で行う"). */
    private static final int CLAW_REPEATS = 3;
    private static final int CLAW_ANIM_TICKS = 30;
    private static final float CLAW_DAMAGE = 20.0F;
    private static final double CLAW_REACH = 8.0D;

    /** Charge duration and speed. */
    private static final int CHARGE_DURATION_TICKS = 40;
    private static final double CHARGE_SPEED = 1.25D;
    private static final double CHARGE_HIT_RANGE = 4.5D;
    /** Upward launch imparted by a connecting charge ("プレイヤーを空高く打ち上げる"). */
    private static final double CHARGE_LAUNCH_POWER = 2.4D;

    /**
     * Three claw swipes as a visible wind-up, then an all-out charge that
     * launches the victim high into the air on contact.
     */
    private static void startClawCharge(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        clawStep(dragon, target, 0);
    }

    private static void clawStep(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target, int done) {
        if (!dragon.isAlive() || dragon.isPlayerTurn() || target == null || !target.isAlive()) {
            dragon.onPatternFinished();
            return;
        }

        if (done >= CLAW_REPEATS) {
            beginCharge(dragon, target);
            return;
        }

        dragon.getNavigation().stop();
        dragon.setDeltaMovement(0.0D, dragon.getDeltaMovement().y, 0.0D);
        dragon.setActionState(ApocalypseSevenHeadedRedDragonEntity.ACTION_CLAW);
        faceTarget(dragon, target);
        dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.RAVAGER_ATTACK,
                SoundSource.HOSTILE, 2.0F, 0.7F);

        dragon.scheduleIn(10, () -> {
            if (!dragon.isAlive() || !target.isAlive()) return;
            if (dragon.distanceTo(target) <= CLAW_REACH) {
                target.hurt(dragon.damageSources().mobAttack(dragon), CLAW_DAMAGE);
            }
        });

        dragon.scheduleIn(CLAW_ANIM_TICKS, () -> clawStep(dragon, target, done + 1));
    }

    private static void beginCharge(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        dragon.setActionState(ApocalypseSevenHeadedRedDragonEntity.ACTION_CHARGE);
        dragon.setCharging(true);
        dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE, 3.0F, 0.6F);
        chargeStep(dragon, target, 0, false);
    }

    private static void chargeStep(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target,
                                   int elapsed, boolean hasHit) {
        if (!dragon.isAlive() || dragon.isPlayerTurn() || target == null || !target.isAlive()
                || elapsed >= CHARGE_DURATION_TICKS) {
            dragon.setCharging(false);
            dragon.onPatternFinished();
            return;
        }

        faceTarget(dragon, target);

        // Drive the charge by direct velocity rather than pathfinding, so it is a
        // genuine unstoppable rush instead of a navigation walk.
        Vec3 toTarget = new Vec3(
                target.getX() - dragon.getX(), 0.0D, target.getZ() - dragon.getZ());
        if (toTarget.lengthSqr() > 1.0E-4D) {
            Vec3 push = toTarget.normalize().scale(CHARGE_SPEED * 0.25D);
            dragon.setDeltaMovement(push.x, dragon.getDeltaMovement().y, push.z);
        }

        boolean hit = hasHit;
        if (!hit && dragon.distanceTo(target) <= CHARGE_HIT_RANGE) {
            target.hurt(dragon.damageSources().mobAttack(dragon), STANDARD_DAMAGE);
            // 打ち上げ: launch the player high into the air. Applied directly to
            // the velocity (not via knockback) so the boss's own 100% knockback
            // resistance semantics and the player's Knockback Resistance gear
            // cannot cancel this signature move.
            target.setDeltaMovement(
                    target.getDeltaMovement().x * 0.4D,
                    CHARGE_LAUNCH_POWER,
                    target.getDeltaMovement().z * 0.4D);
            target.hurtMarked = true;
            if (dragon.level() instanceof ServerLevel serverLevel) {
                shakeNearbyScreens(serverLevel, dragon, 4.0F, 12);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + 1.0D, target.getZ(), 6, 0.6D, 0.4D, 0.6D, 0.0D);
            }
            hit = true;
        }

        final boolean hitState = hit;
        dragon.scheduleIn(1, () -> chargeStep(dragon, target, elapsed + 1, hitState));
    }

    // ---- Tail ----

    private static final int TAIL_ANIM_TICKS = 40;
    private static final double TAIL_REACH = 12.0D;
    /** Knockback strength of the tail, per spec ("強烈なノックバック"). */
    private static final double TAIL_KNOCKBACK = 3.2D;

    /** A single sweeping tail strike: 20 damage plus violent horizontal knockback. */
    private static void startTailSwipe(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        if (!dragon.isAlive() || target == null || !target.isAlive()) {
            dragon.onPatternFinished();
            return;
        }

        dragon.setActionState(ApocalypseSevenHeadedRedDragonEntity.ACTION_TAIL);
        faceTarget(dragon, target);
        dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.HOSTILE, 2.5F, 0.5F);

        dragon.scheduleIn(14, () -> {
            if (!dragon.isAlive()) return;

            // The tail sweeps a wide arc, so it hits everything in range rather
            // than only the focused target.
            List<LivingEntity> victims = dragon.level().getEntitiesOfClass(LivingEntity.class,
                    dragon.getBoundingBox().inflate(TAIL_REACH),
                    e -> e != dragon && e.isAlive()
                            && !(e instanceof DebilitationMartyrEntity)
                            && !(e instanceof TimedGimmickCreeperEntity)
                            && e.distanceTo(dragon) <= TAIL_REACH);

            for (LivingEntity victim : victims) {
                victim.hurt(dragon.damageSources().mobAttack(dragon), STANDARD_DAMAGE);
                Vec3 away = new Vec3(victim.getX() - dragon.getX(), 0.0D, victim.getZ() - dragon.getZ());
                if (away.lengthSqr() < 1.0E-4D) {
                    away = new Vec3(1.0D, 0.0D, 0.0D);
                }
                away = away.normalize().scale(TAIL_KNOCKBACK);
                victim.setDeltaMovement(away.x, 0.75D, away.z);
                victim.hurtMarked = true;
            }

            if (dragon.level() instanceof ServerLevel serverLevel) {
                shakeNearbyScreens(serverLevel, dragon, 3.5F, 10);
            }
        });

        dragon.scheduleIn(TAIL_ANIM_TICKS, dragon::onPatternFinished);
    }

    // ==================================================================
    // Shared helpers
    // ==================================================================

    /** Snaps the dragon's body and head yaw onto the target. */
    private static void faceTarget(ApocalypseSevenHeadedRedDragonEntity dragon, LivingEntity target) {
        if (target == null) return;
        double dx = target.getX() - dragon.getX();
        double dz = target.getZ() - dragon.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        dragon.setYRot(yaw);
        dragon.yBodyRot = yaw;
        dragon.yHeadRot = yaw;
        dragon.getLookControl().setLookAt(target, 60.0F, 60.0F);
    }

    /** The point projectiles are fired from - roughly the dragon's chest/mouth height. */
    private static Vec3 muzzlePosition(ApocalypseSevenHeadedRedDragonEntity dragon) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, dragon.getYRot()).normalize();
        return new Vec3(
                dragon.getX() + forward.x * 2.0D,
                dragon.getY() + dragon.getBbHeight() * 0.75D,
                dragon.getZ() + forward.z * 2.0D);
    }

    /**
     * Resolves the ground height at a horizontal position, so lightning bolts,
     * magic circles and spear impacts all sit on the actual terrain instead of
     * floating at the caster's Y.
     */
    private static double groundY(ServerLevel level, Vec3 pos) {
        BlockPos base = BlockPos.containing(pos.x, pos.y, pos.z);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
        // Prefer terrain near the reference Y so the attack does not jump to a
        // roof far above (or a cave floor far below) the actual fight.
        if (Math.abs(surface - pos.y) > 12.0D) {
            return pos.y;
        }
        return surface;
    }

    /** Sends a screen-shake packet to every player near the dragon. */
    private static void shakeNearbyScreens(ServerLevel level, ApocalypseSevenHeadedRedDragonEntity dragon,
                                           float intensity, int durationTicks) {
        ScreenShakePacket packet = new ScreenShakePacket(intensity, durationTicks);
        for (ServerPlayer player : level.players()) {
            if (player.distanceTo(dragon) <= 48.0F) {
                ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
