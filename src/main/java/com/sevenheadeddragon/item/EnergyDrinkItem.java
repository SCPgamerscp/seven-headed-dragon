package com.sevenheadeddragon.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A drinkable item that grants six level IV combat and movement effects for
 * ten minutes. The empty bottle is returned after use, like a vanilla potion.
 */
public class EnergyDrinkItem extends Item {

    public static final int EFFECT_DURATION_TICKS = 20 * 60 * 10;
    private static final int LEVEL_FOUR_AMPLIFIER = 3;

    public EnergyDrinkItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION_TICKS, LEVEL_FOUR_AMPLIFIER));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, EFFECT_DURATION_TICKS, LEVEL_FOUR_AMPLIFIER));
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP, EFFECT_DURATION_TICKS, LEVEL_FOUR_AMPLIFIER));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION_TICKS, LEVEL_FOUR_AMPLIFIER));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, LEVEL_FOUR_AMPLIFIER));
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION_TICKS, LEVEL_FOUR_AMPLIFIER));
        }

        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }

        stack.shrink(1);
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }

        if (entity instanceof Player player) {
            player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
