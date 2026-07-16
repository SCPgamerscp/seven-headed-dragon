package com.sevenheadeddragon.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 花火 (Firework Mark)
 * <p>
 * After a short fuse delay, detonates a powerful firework rocket crafted
 * with 7 firework stars directly on the target. This deals massive damage
 * scaling with the number of stars, exactly matching vanilla mechanics.
 */
public class FireworkMarkEffect extends MobEffect {

    private static final int FUSE_DELAY_TICKS = 20 * 2;
    private static final int FIREWORK_STAR_COUNT = 7;

    public FireworkMarkEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF1493);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        detonateMassiveFirework(serverLevel, entity, entity.getRandom());
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int initialDuration = com.sevenheadeddragon.registry.ModEffects.SPECIAL_EFFECT_DURATION_TICKS;
        int elapsed = initialDuration - duration;
        return elapsed == FUSE_DELAY_TICKS;
    }

    private void detonateMassiveFirework(ServerLevel level, LivingEntity target, RandomSource random) {
        ItemStack fireworkItem = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fireworksTag = fireworkItem.getOrCreateTagElement("Fireworks");
        ListTag explosions = new ListTag();

        for (int i = 0; i < FIREWORK_STAR_COUNT; i++) {
            CompoundTag explosion = new CompoundTag();
            explosion.putByte("Type", (byte) random.nextInt(5)); // Random shape
            explosion.putIntArray("Colors", new int[]{ 0xFF000000 | random.nextInt(0x1000000) }); // Random color
            explosions.add(explosion);
        }

        fireworksTag.put("Explosions", explosions);
        fireworksTag.putByte("Flight", (byte) 0); // Explode very quickly

        FireworkRocketEntity rocket = new FireworkRocketEntity(level, fireworkItem, target, 
            target.getX(), target.getY(), target.getZ(), true);
        level.addFreshEntity(rocket);
    }
}
