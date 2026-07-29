package com.sevenheadeddragon.entity.dragon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared explosion helper for every one of the Red Dragon's exploding
 * attacks.
 * <p>
 * The master spec is emphatic that <em>none</em> of the dragon's attacks may
 * break terrain ("地形破壊なし") - the goat missiles (power 4), squid missiles
 * (power 3), Longinus spears (power 5) and the timed gimmick creepers
 * (power 10) all deal full explosion <em>damage</em> and knockback while
 * leaving the world geometry completely intact. Centralising this in one
 * place makes it impossible for one attack to accidentally regress to a
 * terrain-destroying explosion.
 * <p>
 * {@link Level.ExplosionInteraction#NONE} is the vanilla 1.20.1 mode that skips the
 * block-destruction pass entirely (it is what a Creeper explosion in a
 * mobGriefing=false world resolves to), so entity damage/knockback and the
 * explosion particles + sound all still play normally.
 */
public final class DragonExplosions {

    private DragonExplosions() {}

    /**
     * Detonates a non-terrain-damaging explosion at {@code pos}.
     *
     * @param level  the (server) level to explode in
     * @param source the entity credited with the explosion, may be {@code null}
     * @param pos    explosion centre
     * @param power  explosion radius/power (4 = goat, 3 = squid, 5 = spear, 10 = creeper)
     */
    public static void explodeNoGrief(Level level, Entity source, Vec3 pos, float power) {
        explodeNoGrief(level, source, pos.x, pos.y, pos.z, power);
    }

    /** @see #explodeNoGrief(Level, Entity, Vec3, float) */
    public static void explodeNoGrief(Level level, Entity source, double x, double y, double z, float power) {
        if (level.isClientSide) return;
        level.explode(source, x, y, z, power, false, Level.ExplosionInteraction.NONE);
    }
}
