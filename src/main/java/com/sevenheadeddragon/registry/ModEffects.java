package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.effect.AsphyxiationEffect;
import com.sevenheadeddragon.effect.ControlReversalEffect;
import com.sevenheadeddragon.effect.FireworkMarkEffect;
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

    private ModEffects() {}
}
