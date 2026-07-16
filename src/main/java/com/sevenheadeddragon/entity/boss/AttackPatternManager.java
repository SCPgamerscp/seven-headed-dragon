package com.sevenheadeddragon.entity.boss;

import com.sevenheadeddragon.entity.MagicCircleEntity;
import com.sevenheadeddragon.entity.PotionMasterEntity;
import com.sevenheadeddragon.registry.ModEntities;
import com.sevenheadeddragon.util.PotionEffectPool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;

/**
 * Selects and executes one of several geometric "bullet-hell" attack
 * patterns for the Potion Master boss's 60-second attack turn.
 * <p>
 * Every pattern first spawns one or more {@link MagicCircleEntity} telegraph
 * markers and only fires the actual vanilla Splash Potion projectiles
 * after a short warning delay, giving the player time to react/dodge.
 * <p>
 * All bullets are affected by gravity and apply a randomly rolled debuff
 * from {@link com.sevenheadeddragon.util.PotionEffectPool} on impact.
 */
public final class AttackPatternManager {

    /** Ticks between a telegraph appearing and its attack actually firing. */
    private static final int TELEGRAPH_TICKS = 30; // 1.5 seconds warning

    private AttackPatternManager() {}

    /**
     * Picks one attack pattern at random and begins executing it against the
     * boss's currently focused target.
     */
    public static void startRandomAttack(PotionMasterEntity boss) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;
        LivingEntity target = boss.getFocusedTarget();
        if (target == null) return;

        RandomSource random = boss.getRandom();
        int pattern = random.nextInt(4);
        switch (pattern) {
            case 0 -> coneBlast(serverLevel, boss, target, random);
            case 1 -> radialWave(serverLevel, boss, target, random);
            case 2 -> spiralStream(serverLevel, boss, target, random);
            default -> skyRain(serverLevel, boss, target, random);
        }
    }

    // ------------------------------------------------------------------
    // Pattern 1: Cone Blast (円錐状ショットガン)
    // A large vertical magic circle appears in front of the boss, facing
    // the target. After the telegraph, a dense 3D cone of potion bullets
    // is fired through it toward the player.
    // ------------------------------------------------------------------
    private static void coneBlast(ServerLevel level, PotionMasterEntity boss, LivingEntity target, RandomSource random) {
        double bossX = boss.getX();
        double bossY = boss.getY() + 1.5;
        double bossZ = boss.getZ();

        // Direction from boss to target (horizontal)
        double dx = target.getX() - bossX;
        double dz = target.getZ() - bossZ;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        if (hDist < 0.001) hDist = 1.0;
        double dirX = dx / hDist;
        double dirZ = dz / hDist;

        // Spawn vertical magic circle 2.5 blocks in front of boss, facing the target
        double circleX = bossX + dirX * 2.5;
        double circleZ = bossZ + dirZ * 2.5;
        float yawDeg = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));

        spawnTelegraph(level, circleX, bossY, circleZ, TELEGRAPH_TICKS + 5, 90.0f, yawDeg);

        int bulletCount = 25 + random.nextInt(6); // 25-30 bullets

        boss.scheduleIn(TELEGRAPH_TICKS, () -> {
            if (!boss.isAlive()) return;
            // Re-acquire target direction at fire time for accuracy
            LivingEntity current = boss.getFocusedTarget();
            double tx = current != null ? current.getX() : target.getX();
            double ty = (current != null ? current.getY() : target.getY()) + 1.0;
            double tz = current != null ? current.getZ() : target.getZ();

            double fdx = tx - circleX;
            double fdy = ty - bossY;
            double fdz = tz - circleZ;
            double fLen = Math.sqrt(fdx * fdx + fdy * fdy + fdz * fdz);
            if (fLen < 0.001) fLen = 1.0;
            double fwdX = fdx / fLen;
            double fwdY = fdy / fLen;
            double fwdZ = fdz / fLen;

            // Perpendicular axes for cone spread
            // perpH is horizontal perpendicular to forward
            double perpHX = -fwdZ;
            double perpHZ = fwdX;
            double perpHLen = Math.sqrt(perpHX * perpHX + perpHZ * perpHZ);
            if (perpHLen < 0.001) { perpHX = 1.0; perpHZ = 0.0; perpHLen = 1.0; }
            perpHX /= perpHLen;
            perpHZ /= perpHLen;
            // perpV is vertical perpendicular
            // cross product of forward and perpH
            double perpVX = fwdY * perpHZ;
            double perpVY = -(fwdX * perpHZ - fwdZ * perpHX);
            double perpVZ = -fwdY * perpHX;
            double perpVLen = Math.sqrt(perpVX * perpVX + perpVY * perpVY + perpVZ * perpVZ);
            if (perpVLen < 0.001) { perpVX = 0; perpVY = 1; perpVZ = 0; perpVLen = 1.0; }
            perpVX /= perpVLen;
            perpVY /= perpVLen;
            perpVZ /= perpVLen;

            double speed = 1.6; // Increased speed for longer throw distance
            double coneHalfAngle = Math.toRadians(22);

            for (int i = 0; i < bulletCount; i++) {
                double phi = random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(random.nextDouble()) * Math.tan(coneHalfAngle);

                double vx = fwdX + perpHX * r * Math.cos(phi) + perpVX * r * Math.sin(phi);
                double vy = fwdY + perpHZ * 0 + perpVY * r * Math.sin(phi);
                double vz = fwdZ + perpHZ * r * Math.cos(phi) + perpVZ * r * Math.sin(phi);
                double vLen = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (vLen < 0.001) vLen = 1.0;

                spawnBullet(level, boss, circleX, bossY, circleZ,
                        vx / vLen * speed, vy / vLen * speed, vz / vLen * speed);
            }
        });
    }

    // ------------------------------------------------------------------
    // Pattern 2: Radial Wave (波紋型放射弾幕)
    // Waves of bullets radiate outward from the boss center in expanding
    // concentric rings, like ripples in water. Each successive wave
    // travels further than the last.
    // ------------------------------------------------------------------
    private static void radialWave(ServerLevel level, PotionMasterEntity boss, LivingEntity target, RandomSource random) {
        double bossX = boss.getX();
        double bossY = boss.getY() + 1.0;
        double bossZ = boss.getZ();

        // Telegraph at boss feet
        spawnTelegraph(level, bossX, boss.getY(), bossZ, TELEGRAPH_TICKS + 50, 0.0f, 0.0f);

        int waves = 4 + random.nextInt(2); // 4-5 waves
        int bulletsPerWave = 20 + random.nextInt(5); // 20-24 per wave

        for (int w = 0; w < waves; w++) {
            int waveDelay = TELEGRAPH_TICKS + w * 12; // each wave 0.6s apart
            double waveSpeed = 0.5 + w * 0.15; // Increased speed for wider/farther spread

            final double fSpeed = waveSpeed;
            final double angleOffset = w * 0.15; // slight angular offset between waves
            final int fBullets = bulletsPerWave;

            boss.scheduleIn(waveDelay, () -> {
                if (!boss.isAlive()) return;
                for (int j = 0; j < fBullets; j++) {
                    double angle = (Math.PI * 2 * j) / fBullets + angleOffset;
                    double vx = Math.cos(angle) * fSpeed;
                    double vz = Math.sin(angle) * fSpeed;
                    spawnBullet(level, boss, bossX, bossY, bossZ, vx, 0.4, vz); // slightly higher arc
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // Pattern 3: Spiral Stream (渦巻きスパイラル)
    // A large horizontal magic circle appears above the target. It spins
    // while dropping potion bullets from points along its circumference.
    // Gravity pulls them into a natural spiral rain below.
    // ------------------------------------------------------------------
    private static void spiralStream(ServerLevel level, PotionMasterEntity boss, LivingEntity target, RandomSource random) {
        double centerX = target.getX();
        double centerZ = target.getZ();
        double circleY = target.getY() + 4.0; // Magic circle 4 blocks above target

        // Horizontal telegraph above target (Pitch 90 is horizontal flat on ground, we need a flat circle in the air)
        spawnTelegraph(level, centerX, circleY, centerZ, TELEGRAPH_TICKS + 100, 90.0f, 0.0f);

        int totalBullets = 100; // 1 bullet per tick
        int fireDuration = 100; // 5 seconds of continuous firing

        for (int i = 0; i < totalBullets; i++) {
            int fireDelay = TELEGRAPH_TICKS + i; // 1 tick apart
            // 7 full rotations over the 100 ticks
            double angle = (Math.PI * 2 * i * 7.0) / totalBullets;
            final int bulletIndex = i;

            boss.scheduleIn(fireDelay, () -> {
                if (!boss.isAlive()) return;
                // Speed increases from 0.1 to 1.6 over the duration
                double currentSpeed = 0.1 + ((double) bulletIndex / totalBullets) * 1.5;
                // Shoot outward horizontally from the center of the magic circle
                double vx = Math.cos(angle) * currentSpeed;
                double vz = Math.sin(angle) * currentSpeed;
                spawnBullet(level, boss, centerX, circleY, centerZ, vx, -0.2, vz);
            });
        }
    }

    // ------------------------------------------------------------------
    // Pattern 4: Sky Rain (豪雨)
    // A massive barrage of potion bullets rains from high above, covering
    // a wide area around the target with staggered timing.
    // ------------------------------------------------------------------
    private static void skyRain(ServerLevel level, PotionMasterEntity boss, LivingEntity target, RandomSource random) {
        int count = 35 + random.nextInt(16); // 35-50 bullets
        double centerX = target.getX();
        double centerZ = target.getZ();
        double groundY = target.getY();

        // Scatter telegraphs on the ground as warning
        for (int t = 0; t < 8; t++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * 6.0;
            spawnTelegraph(level,
                    centerX + Math.cos(angle) * dist, groundY,
                    centerZ + Math.sin(angle) * dist,
                    TELEGRAPH_TICKS + 10, 0.0f, 0.0f);
        }

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * 7.0;
            final double x = centerX + Math.cos(angle) * dist;
            final double z = centerZ + Math.sin(angle) * dist;
            final int delay = TELEGRAPH_TICKS + random.nextInt(40); // staggered over 2 seconds

            boss.scheduleIn(delay, () -> {
                if (!boss.isAlive()) return;
                double spawnY = groundY + 14.0 + random.nextDouble() * 4.0;
                spawnBullet(level, boss, x, spawnY, z, 0.0, -0.1, 0.0);
            });
        }
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    /**
     * Spawns a telegraph magic circle with optional pitch/yaw orientation.
     * pitch=0, yaw=0 is flat/horizontal. pitch=90 makes it vertical.
     */
    private static void spawnTelegraph(ServerLevel level, double x, double y, double z,
                                        int lifetime, float pitch, float yaw) {
        MagicCircleEntity circle = ModEntities.MAGIC_CIRCLE.get().create(level);
        if (circle != null) {
            circle.moveTo(x, y, z, 0.0f, 0.0f);
            circle.setLifetime(lifetime);
            circle.setOrientationPitch(pitch);
            circle.setOrientationYaw(yaw);
            level.addFreshEntity(circle);
        }
    }

    private static void spawnBullet(ServerLevel level, LivingEntity owner, double x, double y, double z,
                                     double vx, double vy, double vz) {
        ThrownPotion bullet = new ThrownPotion(EntityType.POTION, level);
        bullet.setPos(x, y, z);
        bullet.setOwner(owner);
        bullet.setItem(PotionEffectPool.createRandomPotionItem(owner.getRandom()));
        bullet.setDeltaMovement(vx, vy, vz);
        level.addFreshEntity(bullet);
    }
}
