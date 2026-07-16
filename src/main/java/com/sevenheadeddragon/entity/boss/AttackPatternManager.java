package com.sevenheadeddragon.entity.boss;

import com.sevenheadeddragon.entity.MagicCircleEntity;
import com.sevenheadeddragon.entity.PotionMasterEntity;
import com.sevenheadeddragon.entity.projectile.PotionBulletEntity;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Selects and executes one of several geometric "bullet-hell" attack
 * patterns for the Potion Master boss's 60-second attack turn, per spec
 * ("弾幕はUndertaleのような避けゲーです... 「空から降ってくる」「ボスの周りの
 * 魔法陣から放射状に広がる」の他、自由に見た目が綺麗な弾幕パターンを追加して
 * ください").
 * <p>
 * Every pattern first spawns one or more {@link MagicCircleEntity} telegraph
 * markers and only fires the actual {@link PotionBulletEntity} projectiles
 * after a short warning delay, giving the player time to react/dodge before
 * the attack actually launches ("弾幕が来る前に魔法陣で予告し、プレイヤーに
 * 反応する時間を与える").
 * <p>
 * Bullets apply a randomly rolled debuff from
 * {@link com.sevenheadeddragon.util.PotionEffectPool} (mixing the mod's 14
 * custom potions with vanilla debuffs) on impact - see
 * {@link PotionBulletEntity#tick()}.
 */
public final class AttackPatternManager {

    /** Ticks between a telegraph appearing and its attack actually firing. */
    private static final int TELEGRAPH_TICKS = 30; // 1.5 seconds warning

    private AttackPatternManager() {}

    /**
     * Picks one attack pattern at random and begins executing it against the
     * boss's currently focused target. Safe to call repeatedly throughout
     * the 60-second boss attack turn (e.g. once at the start, and again
     * periodically) to build up a full bullet-hell turn.
     */
    public static void startRandomAttack(PotionMasterEntity boss) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;
        LivingEntity target = boss.getFocusedTarget();
        if (target == null) return;

        RandomSource random = boss.getRandom();
        int pattern = random.nextInt(4);
        switch (pattern) {
            case 0 -> skyRain(serverLevel, boss, target, random);
            case 1 -> radialBurstFromBoss(serverLevel, boss, target, random);
            case 2 -> spiralAroundTarget(serverLevel, boss, target, random);
            default -> crossVolley(serverLevel, boss, target, random);
        }
    }

    // ------------------------------------------------------------------
    // Pattern 1: "rain from sky" - potion bullets fall from high above a
    // scatter of points near the target, each telegraphed on the ground.
    // ------------------------------------------------------------------
    private static void skyRain(ServerLevel level, PotionMasterEntity boss, LivingEntity target, RandomSource random) {
        int count = 8 + random.nextInt(5);
        double centerX = target.getX();
        double centerZ = target.getZ();
        double groundY = target.getY();

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * 4.0;
            final double x = centerX + Math.cos(angle) * dist;
            final double z = centerZ + Math.sin(angle) * dist;
            final int delay = TELEGRAPH_TICKS + random.nextInt(10);

            spawnTelegraph(level, x, groundY, z, delay + 5);

            boss.scheduleIn(delay, () -> {
                if (!boss.isAlive()) return;
                double spawnY = groundY + 12.0 + random.nextDouble() * 4.0;
                spawnBullet(level, boss, x, spawnY, z, 0.0, -0.55, 0.0);
            });
        }
    }

    // ------------------------------------------------------------------
    // Pattern 2: "radial burst from magic circles around the boss" -
    // circles ring the boss, then bullets fly outward like spokes.
    // ------------------------------------------------------------------
    private static void radialBurstFromBoss(ServerLevel level, PotionMasterEntity boss, LivingEntity target, RandomSource random) {
        int spokes = 8;
        double radius = 3.0;
        double bossX = boss.getX();
        double bossY = boss.getY() + 1.0;
        double bossZ = boss.getZ();

        for (int i = 0; i < spokes; i++) {
            double angle = (Math.PI * 2 * i) / spokes;
            final double cx = bossX + Math.cos(angle) * radius;
            final double cz = bossZ + Math.sin(angle) * radius;
            final double fAngle = angle;

            spawnTelegraph(level, cx, bossY, cz, TELEGRAPH_TICKS + 5);

            boss.scheduleIn(TELEGRAPH_TICKS, () -> {
                if (!boss.isAlive()) return;
                double vx = Math.cos(fAngle) * 0.45;
                double vz = Math.sin(fAngle) * 0.45;
                spawnBullet(level, boss, cx, bossY, cz, vx, 0.03, vz);
            });
        }
    }

    // ------------------------------------------------------------------
    // Pattern 3: rotating spiral of magic circles around the target,
    // firing inward in staggered waves for an expanding-spiral look.
    // ------------------------------------------------------------------
    private static void spiralAroundTarget(ServerLevel level, PotionMasterEntity boss, LivingEntity target, RandomSource random) {
        int waves = 3;
        int perWave = 6;
        double baseRadius = 5.0;

        for (int w = 0; w < waves; w++) {
            int waveDelay = w * 15;
            for (int j = 0; j < perWave; j++) {
                double angle = (Math.PI * 2 * j) / perWave + w * 0.35;
                final double x = target.getX() + Math.cos(angle) * baseRadius;
                final double z = target.getZ() + Math.sin(angle) * baseRadius;
                final double y = target.getY() + 1.0;
                final int fireDelay = waveDelay + TELEGRAPH_TICKS;

                spawnTelegraph(level, x, y, z, fireDelay + 5);

                boss.scheduleIn(fireDelay, () -> {
                    if (!boss.isAlive()) return;
                    LivingEntity current = boss.getFocusedTarget();
                    double tx = current != null ? current.getX() : target.getX();
                    double ty = current != null ? current.getY() + 1.0 : target.getY() + 1.0;
                    double tz = current != null ? current.getZ() : target.getZ();

                    double dx = tx - x;
                    double dy = ty - y;
                    double dz = tz - z;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (len < 0.001) len = 1.0;
                    double speed = 0.42;
                    spawnBullet(level, boss, x, y, z, dx / len * speed, dy / len * speed, dz / len * speed);
                });
            }
        }
    }

    // ------------------------------------------------------------------
    // Pattern 4: "cross volley" - four bullets converge on the target's
    // position from the north/south/east/west, Undertale-style.
    // ------------------------------------------------------------------
    private static void crossVolley(ServerLevel level, PotionMasterEntity boss, LivingEntity target, RandomSource random) {
        double cx = target.getX();
        double cz = target.getZ();
        double y = target.getY() + 1.0;
        double dist = 12.0;
        double speed = 0.5;

        spawnTelegraph(level, cx, y, cz, TELEGRAPH_TICKS + 5);

        int[][] dirs = { {0, -1}, {0, 1}, {1, 0}, {-1, 0} };

        boss.scheduleIn(TELEGRAPH_TICKS, () -> {
            if (!boss.isAlive()) return;
            for (int[] dir : dirs) {
                double sx = cx + dir[0] * dist;
                double sz = cz + dir[1] * dist;
                double vx = -dir[0] * speed;
                double vz = -dir[1] * speed;
                spawnBullet(level, boss, sx, y, sz, vx, 0.0, vz);
            }
        });
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------
    private static void spawnTelegraph(ServerLevel level, double x, double y, double z, int lifetime) {
        MagicCircleEntity circle = ModEntities.MAGIC_CIRCLE.get().create(level);
        if (circle != null) {
            circle.moveTo(x, y, z, 0.0f, 0.0f);
            circle.setLifetime(lifetime);
            level.addFreshEntity(circle);
        }
    }

    private static void spawnBullet(ServerLevel level, LivingEntity owner, double x, double y, double z,
                                     double vx, double vy, double vz) {
        PotionBulletEntity bullet = ModEntities.POTION_BULLET.get().create(level);
        if (bullet != null) {
            bullet.moveTo(x, y, z, 0.0f, 0.0f);
            bullet.setOwner(owner);
            bullet.setDeltaMovement(vx, vy, vz);
            level.addFreshEntity(bullet);
        }
    }
}
