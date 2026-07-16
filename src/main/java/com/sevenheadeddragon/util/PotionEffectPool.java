package com.sevenheadeddragon.util;

import com.sevenheadeddragon.registry.ModEffects;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Central pool of debuff effects the Potion Master's potion bullets may
 * apply on impact. Per user request, this mixes the mod's 14 custom
 * effects together with a selection of vanilla debuff potion effects
 * (poison, weakness, slowness, blindness, hunger, nausea, levitation, bad
 * omen, darkness, etc.), so that the boss's bullet-hell attacks are not
 * limited to only custom effects.
 * <p>
 * All entries share the same uniform duration
 * ({@link ModEffects#SPECIAL_EFFECT_DURATION_TICKS}) when applied during
 * combat, per spec ("これらの特殊ポーションの「効果持続時間」は一律" - and by
 * extension, the vanilla debuffs mixed in here use the same duration for
 * consistency with the rest of the bullet-hell attacks).
 * <p>
 * Multiple debuffs CAN be applied simultaneously to the same target (e.g. a
 * player could be hit by both a custom "percent poison" bullet and a
 * vanilla Slowness bullet in the same attack turn) - see spec: "ポーションの
 * デバフは複数同時にかかります".
 */
public final class PotionEffectPool {

    private static final List<Supplier<MobEffect>> POOL = buildPool();

    private static List<Supplier<MobEffect>> buildPool() {
        List<Supplier<MobEffect>> list = new ArrayList<>();

        // --- 14 custom effects ---
        for (RegistryObject<MobEffect> effect : List.of(
                ModEffects.PERCENT_POISON, ModEffects.UNDEAD_CURSE, ModEffects.INSECTIFY,
                ModEffects.INSECTICIDE, ModEffects.SCORCH, ModEffects.ASPHYXIATION,
                ModEffects.POISON_AMPLIFY, ModEffects.MAGMA, ModEffects.FALL_AMPLIFY,
                ModEffects.WATER_VULNERABILITY, ModEffects.LIGHTNING_MARK,
                ModEffects.CONTROL_REVERSAL, ModEffects.TELEPORT_MARK, ModEffects.FIREWORK_MARK)) {
            list.add(effect::get);
        }

        // --- vanilla debuff effects, mixed into the same pool ---
        list.add(() -> MobEffects.POISON);
        list.add(() -> MobEffects.WEAKNESS);
        list.add(() -> MobEffects.MOVEMENT_SLOWDOWN); // Slowness
        list.add(() -> MobEffects.DIG_SLOWDOWN);      // Mining Fatigue
        list.add(() -> MobEffects.CONFUSION);         // Nausea
        list.add(() -> MobEffects.BLINDNESS);
        list.add(() -> MobEffects.HUNGER);
        list.add(() -> MobEffects.LEVITATION);
        list.add(() -> MobEffects.UNLUCK);
        list.add(() -> MobEffects.BAD_OMEN);
        list.add(() -> MobEffects.DARKNESS);

        return list;
    }

    /**
     * Applies one randomly selected effect (from the combined custom +
     * vanilla pool) to the target, using the mod's uniform special-effect
     * duration.
     */
    public static void applyRandomEffect(LivingEntity target, RandomSource random) {
        MobEffect effect = POOL.get(random.nextInt(POOL.size())).get();
        target.addEffect(new MobEffectInstance(effect, ModEffects.SPECIAL_EFFECT_DURATION_TICKS, 0));
    }

    private PotionEffectPool() {}
}
