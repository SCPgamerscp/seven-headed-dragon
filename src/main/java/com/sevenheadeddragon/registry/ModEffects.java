package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.effect.AsphyxiationEffect;
import com.sevenheadeddragon.effect.ControlReversalEffect;
import com.sevenheadeddragon.effect.FireworkMarkEffect;
import com.sevenheadeddragon.effect.GodSlayingEffect;
import com.sevenheadeddragon.effect.SevenSinsEffect;
import com.sevenheadeddragon.effect.InsecticideEffect;
import com.sevenheadeddragon.effect.InsectifyEffect;
import com.sevenheadeddragon.effect.LightningMarkEffect;
import com.sevenheadeddragon.effect.MagmaEffect;
import com.sevenheadeddragon.effect.PercentPoisonEffect;
import com.sevenheadeddragon.effect.PoisonAmplifyEffect;
import com.sevenheadeddragon.effect.ScorchEffect;
import com.sevenheadeddragon.effect.FallDamageAmplifyEffect;
import com.sevenheadeddragon.effect.TeleportMarkEffect;
import com.sevenheadeddragon.effect.UndeadCurseEffect;
import com.sevenheadeddragon.effect.WaterVulnerabilityEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers all 14 custom MobEffect instances used by the Potion Master boss.
 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SevenHeadedDragon.MODID);

    /** All special effects share the same duration (in ticks). Defined centrally here. */
    public static final int SPECIAL_EFFECT_DURATION_TICKS = 20 * 20; // 20 seconds

    // 1. 割合毒 (Percent Poison) - damage as a % of current HP, can kill.
    public static final RegistryObject<MobEffect> PERCENT_POISON =
            EFFECTS.register("percent_poison", PercentPoisonEffect::new);

    // 2. アンデッド化 (Undead Curse) - burns in daylight like a zombie.
    public static final RegistryObject<MobEffect> UNDEAD_CURSE =
            EFFECTS.register("undead_curse", UndeadCurseEffect::new);

    // 3. 昆虫化 (Insectify) - internal status flag only, enables Insecticide damage.
    public static final RegistryObject<MobEffect> INSECTIFY =
            EFFECTS.register("insectify", InsectifyEffect::new);

    // 4. 殺虫 (Insecticide) - damage only if target is Insectify'd, else 0 dmg.
    public static final RegistryObject<MobEffect> INSECTICIDE =
            EFFECTS.register("insecticide", InsecticideEffect::new);

    // 5. 炎上 (Scorch) - fire damage x5, can be reduced/blocked by fire resistance.
    public static final RegistryObject<MobEffect> SCORCH =
            EFFECTS.register("scorch", ScorchEffect::new);

    // 6. 酸欠 (Asphyxiation) - drowning damage even on land.
    public static final RegistryObject<MobEffect> ASPHYXIATION =
            EFFECTS.register("asphyxiation", AsphyxiationEffect::new);

    // 7. 毒ダメージ倍化 (Poison Amplify) - only functions while poisoned.
    public static final RegistryObject<MobEffect> POISON_AMPLIFY =
            EFFECTS.register("poison_amplify", PoisonAmplifyEffect::new);

    // 8. 奈落ダメージ (Void Damage) - bypasses armor completely.
    public static final RegistryObject<MobEffect> VOID_DAMAGE =
            EFFECTS.register("void_damage", com.sevenheadeddragon.effect.VoidDamageEffect::new);

    // 8. マグマダメージ (Magma) - deals magma block contact damage each tick.
    public static final RegistryObject<MobEffect> MAGMA =
            EFFECTS.register("magma", MagmaEffect::new);

    // 9. 落下ダメージ倍化 (Fall Damage Amplify) - multiplies fall damage (no physics
    // change) and periodically deals armor-piercing void damage every 5 seconds.
    public static final RegistryObject<MobEffect> FALL_AMPLIFY =
            EFFECTS.register("fall_amplify", FallDamageAmplifyEffect::new);

    // 10. 水耐性低下 (Water Vulnerability) - takes damage while raining.
    public static final RegistryObject<MobEffect> WATER_VULNERABILITY =
            EFFECTS.register("water_vulnerability", WaterVulnerabilityEffect::new);

    // 11. 雷 (Lightning Mark) - periodically strikes lightning on the target, piercing roofs.
    public static final RegistryObject<MobEffect> LIGHTNING_MARK =
            EFFECTS.register("lightning_mark", LightningMarkEffect::new);

    // 12. 操作反転 (Control Reversal) - inverts WASD movement keys (client-side handling).
    public static final RegistryObject<MobEffect> CONTROL_REVERSAL =
            EFFECTS.register("control_reversal", ControlReversalEffect::new);

    // 13. テレポート (Teleport Mark) - teleports the target after a short delay.
    public static final RegistryObject<MobEffect> TELEPORT_MARK =
            EFFECTS.register("teleport_mark", TeleportMarkEffect::new);

    // 14. 花火 (Firework Mark) - detonates random fireworks around the target, no block breaking except "weak explosion".
    public static final RegistryObject<MobEffect> FIREWORK_MARK =
            EFFECTS.register("firework_mark", FireworkMarkEffect::new);

    // 15. 爆発 (Explosion) - periodically causes a power-1 explosion.
    public static final RegistryObject<MobEffect> EXPLOSION =
            EFFECTS.register("explosion", com.sevenheadeddragon.effect.ExplosionEffect::new);

    // 16. ドラゴン殺しの毒 (Dragon-Slaying Poison) - Centipede Boss bite debuff, 10 magic dmg/sec, stops at 1 HP.
    public static final RegistryObject<MobEffect> DRAGON_SLAYING_POISON =
            EFFECTS.register("dragon_slaying_poison", com.sevenheadeddragon.effect.DragonSlayingPoisonEffect::new);

    // ------------------------------------------------------------------
    // 終末の七つ頭の赤い竜 (Apocalypse Seven Headed Red Dragon)
    // ------------------------------------------------------------------

    // 17. 神殺し (God Slaying) - Longinus Spear direct hit; strips 10% of MAX HP per second.
    public static final RegistryObject<MobEffect> GOD_SLAYING =
            EFFECTS.register("god_slaying", GodSlayingEffect::new);

    // 18-24. 七つの大罪 (The Seven Deadly Sins) - one per dragon head, each
    // -50% attack damage and -50% movement speed. Registered as seven distinct
    // effects (not one stacking effect) so all seven can coexist on a victim
    // that eats the full 7-bite combo.
    public static final RegistryObject<MobEffect> SIN_PRIDE =
            EFFECTS.register("sin_pride", () -> new SevenSinsEffect(SevenSinsEffect.Sin.PRIDE));
    public static final RegistryObject<MobEffect> SIN_WRATH =
            EFFECTS.register("sin_wrath", () -> new SevenSinsEffect(SevenSinsEffect.Sin.WRATH));
    public static final RegistryObject<MobEffect> SIN_ENVY =
            EFFECTS.register("sin_envy", () -> new SevenSinsEffect(SevenSinsEffect.Sin.ENVY));
    public static final RegistryObject<MobEffect> SIN_SLOTH =
            EFFECTS.register("sin_sloth", () -> new SevenSinsEffect(SevenSinsEffect.Sin.SLOTH));
    public static final RegistryObject<MobEffect> SIN_GREED =
            EFFECTS.register("sin_greed", () -> new SevenSinsEffect(SevenSinsEffect.Sin.GREED));
    public static final RegistryObject<MobEffect> SIN_GLUTTONY =
            EFFECTS.register("sin_gluttony", () -> new SevenSinsEffect(SevenSinsEffect.Sin.GLUTTONY));
    public static final RegistryObject<MobEffect> SIN_LUST =
            EFFECTS.register("sin_lust", () -> new SevenSinsEffect(SevenSinsEffect.Sin.LUST));

    /**
     * The seven sins in bite order (head 1 → head 7), so the attack pattern can
     * simply index into this list as it walks through attack_bite_1..7.
     */
    public static final java.util.List<RegistryObject<MobEffect>> SEVEN_SINS = java.util.List.of(
            SIN_PRIDE, SIN_WRATH, SIN_ENVY, SIN_SLOTH, SIN_GREED, SIN_GLUTTONY, SIN_LUST);

    private ModEffects() {}
}
