package com.sevenheadeddragon.event;

import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.util.EffectUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Central combat-damage adjustments for the mod's custom effects.
 * <p>
 * Handles the "marker-only" effects that need cross-cutting damage logic:
 * <ul>
 *   <li><b>Scorch (炎上)</b>: multiplies fire damage x5, unless the target
 *       has Fire Resistance or a Fire Protection enchant (see
 *       {@link EffectUtil#hasFireProtection}).</li>
 *   <li><b>Poison Amplify (毒ダメージ倍化)</b>: multiplies damage, but only
 *       when the incoming damage is vanilla Poison damage AND the target
 *       currently has vanilla Poison active.</li>
 *   <li><b>Fall Damage Amplify (落下ダメージ倍化)</b>: multiplies fall
 *       damage. Does not touch the separate periodic void-damage tick,
 *       which is handled inside {@code FallDamageAmplifyEffect} itself.</li>
 * </ul>
 * Also provides a small, generic hook point for enchantment/potion-based
 * damage reduction so that Protection enchantments and Resistance potions
 * continue to meaningfully reduce damage from these custom sources (vanilla
 * already applies EnchantmentProtection + Resistance automatically via
 * LivingEntity#getDamageAfterMagicAbsorb, so no extra code is required here
 * beyond firing hurt() with an appropriate DamageSource - this class only
 * adds the *multipliers* on top of that vanilla pipeline).
 */
public class ModCombatEvents {

    private static final float SCORCH_MULTIPLIER = 5.0f;
    private static final float POISON_AMPLIFY_MULTIPLIER = 3.0f;

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float amount = event.getAmount();
        float newAmount = amount;

        if (target.hasEffect(ModEffects.SCORCH.get()) && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            if (!EffectUtil.hasFireProtection(target)) {
                newAmount *= SCORCH_MULTIPLIER;
            } else {
                // Fire Resistance / Fire Protection reduces the multiplier's
                // effect - here we simply fall back to the un-amplified
                // amount, letting vanilla's own resistance/enchant handling
                // (applied earlier in the damage pipeline) do the rest.
                newAmount = amount;
            }
        }

        if (target.hasEffect(ModEffects.POISON_AMPLIFY.get())
                && target.hasEffect(MobEffects.POISON)
                && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) == false
                && isMagicDamage(source)) {
            newAmount *= POISON_AMPLIFY_MULTIPLIER;
        }

        if (target.hasEffect(ModEffects.FALL_AMPLIFY.get()) && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            newAmount *= com.sevenheadeddragon.effect.FallDamageAmplifyEffect.FALL_DAMAGE_MULTIPLIER;
        }

        if (newAmount != amount) {
            event.setAmount(Math.max(0.0f, newAmount));
        }
    }

    private boolean isMagicDamage(DamageSource source) {
        return source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
    }
}
