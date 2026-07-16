package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Registers the 14 custom Potion definitions (used to create the matching
 * Splash Potion items).
 * <p>
 * There are two distinct durations at play in this mod:
 * <ul>
 *   <li>{@link ModEffects#SPECIAL_EFFECT_DURATION_TICKS} - the uniform,
 *       fixed duration applied whenever the Potion Master boss's bullet-hell
 *       attacks land an effect on a player during combat (applied directly
 *       in code by the projectile/AoE logic, NOT through this Potion's own
 *       registered duration). This keeps "全体の効果持続時間は一律" true for
 *       actual battle balance.</li>
 *   <li>{@link #LOOT_POTION_DURATION_TICKS} - the duration baked into these
 *       registered Potion items themselves (10 minutes). This is what
 *       matters when a player drinks/splashes/lingers one of the boss's
 *       loot-drop potions on themselves, another player (PvP), or another
 *       mob - matching the spec's "特殊スプラッシュポーション10分版" reward.</li>
 * </ul>
 */
public final class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, SevenHeadedDragon.MODID);

    /** 10 minutes, matching the "特殊スプラッシュポーション10分版" loot reward spec. */
    public static final int LOOT_POTION_DURATION_TICKS = 20 * 60 * 10;

    private static final int DURATION = LOOT_POTION_DURATION_TICKS;

    public static final RegistryObject<Potion> PERCENT_POISON = register("percent_poison", ModEffects.PERCENT_POISON);
    public static final RegistryObject<Potion> UNDEAD_CURSE = register("undead_curse", ModEffects.UNDEAD_CURSE);
    public static final RegistryObject<Potion> INSECTIFY = register("insectify", ModEffects.INSECTIFY);
    public static final RegistryObject<Potion> INSECTICIDE = register("insecticide", ModEffects.INSECTICIDE);
    public static final RegistryObject<Potion> SCORCH = register("scorch", ModEffects.SCORCH);
    public static final RegistryObject<Potion> ASPHYXIATION = register("asphyxiation", ModEffects.ASPHYXIATION);
    public static final RegistryObject<Potion> POISON_AMPLIFY = register("poison_amplify", ModEffects.POISON_AMPLIFY);
    public static final RegistryObject<Potion> VOID_DAMAGE = register("void_damage", ModEffects.VOID_DAMAGE);
    public static final RegistryObject<Potion> MAGMA = register("magma", ModEffects.MAGMA);
    public static final RegistryObject<Potion> FALL_AMPLIFY = register("fall_amplify", ModEffects.FALL_AMPLIFY);
    public static final RegistryObject<Potion> WATER_VULNERABILITY = register("water_vulnerability", ModEffects.WATER_VULNERABILITY);
    public static final RegistryObject<Potion> LIGHTNING_MARK = register("lightning_mark", ModEffects.LIGHTNING_MARK);
    public static final RegistryObject<Potion> CONTROL_REVERSAL = register("control_reversal", ModEffects.CONTROL_REVERSAL);
    public static final RegistryObject<Potion> TELEPORT_MARK = register("teleport_mark", ModEffects.TELEPORT_MARK);
    public static final RegistryObject<Potion> FIREWORK_MARK = register("firework_mark", ModEffects.FIREWORK_MARK);

    private static RegistryObject<Potion> register(String name, Supplier<MobEffect> effect) {
        return POTIONS.register(name, () -> new Potion(name, new MobEffectInstance(effect.get(), DURATION, 0)));
    }

    /**
     * All 14 custom potion RegistryObjects, in a fixed order. Used for
     * guaranteed one-of-each drops from the boss.
     */
    public static java.util.List<RegistryObject<Potion>> all() {
        return java.util.List.of(
                PERCENT_POISON, UNDEAD_CURSE, INSECTIFY, INSECTICIDE, SCORCH,
                ASPHYXIATION, POISON_AMPLIFY, VOID_DAMAGE, MAGMA, FALL_AMPLIFY, WATER_VULNERABILITY,
                LIGHTNING_MARK, CONTROL_REVERSAL, TELEPORT_MARK, FIREWORK_MARK
        );
    }

    private ModPotions() {}
}
