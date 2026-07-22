package com.sevenheadeddragon.util;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Custom, data-driven {@link DamageType}s (registered via JSON under
 * {@code data/sevenheadeddragon/damage_type/}) used to give several of the
 * mod's 14 custom debuffs their own dedicated death messages, per spec
 * ("特殊デバフによる死亡時には専用のデスメッセージを表示する").
 * <p>
 * Their localized "death.attack.&lt;message_id&gt;" translation strings live
 * in the lang files (see en_us.json / ja_jp.json).
 */
public final class ModDamageTypes {

    public static final ResourceKey<DamageType> PERCENT_POISON = key("percent_poison");
    public static final ResourceKey<DamageType> INSECTICIDE = key("insecticide");
    public static final ResourceKey<DamageType> ASPHYXIATION = key("asphyxiation");
    public static final ResourceKey<DamageType> MAGMA = key("magma");
    public static final ResourceKey<DamageType> VOID_DAMAGE = key("void_damage");
    public static final ResourceKey<DamageType> WATER_VULNERABILITY = key("water_vulnerability");
    public static final ResourceKey<DamageType> DRAGON_SLAYING_POISON = key("dragon_slaying_poison");

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(SevenHeadedDragon.MODID, path));
    }

    /**
     * Builds a {@link DamageSource} for one of this mod's custom damage
     * types, resolved dynamically against the current world's registry
     * access (custom DamageTypes are data-driven/reloadable, so they must be
     * looked up via {@link Level#registryAccess()} rather than referenced
     * statically like a regular Forge registry object).
     */
    public static DamageSource source(LivingEntity victim, ResourceKey<DamageType> key) {
        Holder<DamageType> holder = victim.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(holder);
    }

    private ModDamageTypes() {}
}
