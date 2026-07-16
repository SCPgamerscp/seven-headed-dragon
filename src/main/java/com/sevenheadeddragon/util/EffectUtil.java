package com.sevenheadeddragon.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared helper logic for the mod's custom debuff MobEffects.
 */
public final class EffectUtil {

    private EffectUtil() {}

    /** Returns true if the entity has any means of breathing (used to negate Asphyxiation). */
    public static boolean canBreathe(LivingEntity entity) {
        if (entity.hasEffect(MobEffects.WATER_BREATHING)) return true;
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Items.TURTLE_HELMET);
    }

    /**
     * Returns true if the entity has any protection against extra fire damage
     * (vanilla Fire Resistance potion effect, or Fire Protection enchantment on
     * any armor piece) - per spec, Scorch's x5 fire damage multiplier can be
     * blocked/reduced this way.
     */
    public static boolean hasFireProtection(LivingEntity entity) {
        if (entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return true;
        return totalFireProtectionLevel(entity) > 0;
    }

    public static int totalFireProtectionLevel(LivingEntity entity) {
        int level = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                level += entity.getItemBySlot(slot).getEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.FIRE_PROTECTION);
            }
        }
        return level;
    }

    /**
     * Finds a safe teleport position near the target. If the destination has a
     * solid (non-transparent, including opaque) block, the position is lowered
     * by one block repeatedly until open air/passable space is found or a
     * safety limit is hit.
     */
    public static BlockPos resolveTeleportDestination(Level level, BlockPos desired) {
        BlockPos pos = desired;
        int guard = 0;
        while (guard < 32 && isBlocked(level, pos)) {
            pos = pos.below();
            guard++;
        }
        return pos;
    }

    /**
     * A position is considered "blocked" (i.e. occupied by a block, including
     * non-transparent/opaque blocks) if its collision shape is not empty.
     * Per spec: "テレポート先にブロックがあった場合、ブロックより1ブロック下にテレポートする
     * (ブロックより1ブロック下(非透過ブロック含む))".
     */
    private static boolean isBlocked(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isExposedToSky(Level level, BlockPos pos) {
        return level.canSeeSky(pos);
    }

    public static boolean isDaytimeSunny(Level level) {
        return level.isDay() && !level.isRaining();
    }
}
