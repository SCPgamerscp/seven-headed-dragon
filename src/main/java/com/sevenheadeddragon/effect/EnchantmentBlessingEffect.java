package com.sevenheadeddragon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Rewards the bearer with enchanting supplies once per second. */
public class EnchantmentBlessingEffect extends MobEffect {
    public EnchantmentBlessingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8A2BE2);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player) || player.level().isClientSide) return;
        give(player, new ItemStack(Items.EXPERIENCE_BOTTLE, 10));
        give(player, new ItemStack(Items.LAPIS_BLOCK, 2));
        give(player, new ItemStack(Items.BOOKSHELF, 2));
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
