package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.CentipedeBossEntity;
import com.sevenheadeddragon.entity.FangConductorEntity;
import com.sevenheadeddragon.entity.FangKingEntity;
import com.sevenheadeddragon.entity.MagicCircleEntity;
import com.sevenheadeddragon.entity.PotionMasterEntity;
import com.sevenheadeddragon.entity.dragon.ApocalypseSevenHeadedRedDragonEntity;
import com.sevenheadeddragon.entity.dragon.DebilitationMartyrEntity;
import com.sevenheadeddragon.entity.dragon.DragonMagicCircleEntity;
import com.sevenheadeddragon.entity.dragon.GoatMissileEntity;
import com.sevenheadeddragon.entity.dragon.LonginusSpearEntity;
import com.sevenheadeddragon.entity.dragon.RainbowLightningEntity;
import com.sevenheadeddragon.entity.dragon.SquidMissileEntity;
import com.sevenheadeddragon.entity.dragon.TimedGimmickCreeperEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers all custom entities: the Potion Master boss, its magic-circle
 * telegraph entity, and its potion projectile.
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SevenHeadedDragon.MODID);

    public static final RegistryObject<EntityType<PotionMasterEntity>> POTION_MASTER =
            ENTITY_TYPES.register("potion_master", () -> EntityType.Builder.of(PotionMasterEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f) // identical hitbox to vanilla Witch
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .fireImmune()
                    .build("potion_master"));

    public static final RegistryObject<EntityType<MagicCircleEntity>> MAGIC_CIRCLE =
            ENTITY_TYPES.register("magic_circle", () -> EntityType.Builder.<MagicCircleEntity>of(MagicCircleEntity::new, MobCategory.MISC)
                    .sized(1.5f, 0.1f)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("magic_circle"));

    public static final RegistryObject<EntityType<FangKingEntity>> FANG_KING =
            ENTITY_TYPES.register("fang_king", () -> EntityType.Builder.of(FangKingEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f) // identical hitbox to vanilla Evoker
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("fang_king"));

    public static final RegistryObject<EntityType<FangConductorEntity>> FANG_CONDUCTOR =
            ENTITY_TYPES.register("fang_conductor", () -> EntityType.Builder.<FangConductorEntity>of(FangConductorEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .noSave()
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("fang_conductor"));

    public static final RegistryObject<EntityType<CentipedeBossEntity>> CENTIPEDE_BOSS =
            ENTITY_TYPES.register("centipede_black_dragon_eater", () -> EntityType.Builder.of(CentipedeBossEntity::new, MobCategory.MONSTER)
                    .sized(2.0f, 2.0f)
                    .clientTrackingRange(80)
                    .updateInterval(1)
                    .build("centipede_black_dragon_eater"));

    // ------------------------------------------------------------------
    // 終末の七つ頭の赤い竜 (Apocalypse Seven Headed Red Dragon) and its arsenal
    // ------------------------------------------------------------------

    /**
     * The final boss itself. The registry name matches the spec's entity ID
     * {@code sevenheadeddragon:apocalypse_seven_headed_red_dragon}.
     * <p>
     * The hitbox is deliberately huge (24x14) because the supplied Blockbench
     * model is roughly 248x84x261 pixels (~15x5x16 blocks) at 1.0x scale, and a
     * boss the player is meant to hit during the 5-second window must actually
     * be clickable across its rendered body.
     */
    public static final RegistryObject<EntityType<ApocalypseSevenHeadedRedDragonEntity>> APOCALYPSE_RED_DRAGON =
            ENTITY_TYPES.register("apocalypse_seven_headed_red_dragon",
                    () -> EntityType.Builder.of(ApocalypseSevenHeadedRedDragonEntity::new, MobCategory.MONSTER)
                            .sized(16.0F, 12.0F)
                            .clientTrackingRange(160)
                            .updateInterval(1)
                            .fireImmune()
                            .build("apocalypse_seven_headed_red_dragon"));

    /** 🐐 山羊ミサイル - the machine-gun projectile (30 rounds, power 4). */
    public static final RegistryObject<EntityType<GoatMissileEntity>> GOAT_MISSILE =
            ENTITY_TYPES.register("goat_missile",
                    () -> EntityType.Builder.<GoatMissileEntity>of(GoatMissileEntity::new, MobCategory.MISC)
                            .sized(0.9F, 0.9F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .noSave()
                            .build("goat_missile"));

    /** 🦑 イカミサイル - the homing projectile (5-missile volley, power 3). */
    public static final RegistryObject<EntityType<SquidMissileEntity>> SQUID_MISSILE =
            ENTITY_TYPES.register("squid_missile",
                    () -> EntityType.Builder.<SquidMissileEntity>of(SquidMissileEntity::new, MobCategory.MISC)
                            .sized(0.9F, 1.4F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .noSave()
                            .build("squid_missile"));

    /** ロンギヌスの槍 - the 50+ spear cross-drop (power 5 + 神殺し). */
    public static final RegistryObject<EntityType<LonginusSpearEntity>> LONGINUS_SPEAR =
            ENTITY_TYPES.register("longinus_spear",
                    () -> EntityType.Builder.<LonginusSpearEntity>of(LonginusSpearEntity::new, MobCategory.MISC)
                            .sized(0.6F, 3.0F)
                            .clientTrackingRange(160)
                            .updateInterval(1)
                            .noSave()
                            .build("longinus_spear"));

    /** 七色の雷 - one Shocker Breaker bolt. */
    public static final RegistryObject<EntityType<RainbowLightningEntity>> RAINBOW_LIGHTNING =
            ENTITY_TYPES.register("rainbow_lightning",
                    () -> EntityType.Builder.<RainbowLightningEntity>of(RainbowLightningEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(160)
                            .updateInterval(1)
                            .noSave()
                            .fireImmune()
                            .build("rainbow_lightning"));

    /** 魔法陣 - the Red Dragon's coloured, resizable telegraph circle. */
    public static final RegistryObject<EntityType<DragonMagicCircleEntity>> DRAGON_MAGIC_CIRCLE =
            ENTITY_TYPES.register("dragon_magic_circle",
                    () -> EntityType.Builder.<DragonMagicCircleEntity>of(DragonMagicCircleEntity::new, MobCategory.MISC)
                            .sized(1.0F, 0.1F)
                            .clientTrackingRange(160)
                            .updateInterval(1)
                            .noSave()
                            .build("dragon_magic_circle"));

    /** 10秒時限爆発クリーパー - the timed-bomb gimmick add (power 10). */
    public static final RegistryObject<EntityType<TimedGimmickCreeperEntity>> TIMED_GIMMICK_CREEPER =
            ENTITY_TYPES.register("timed_gimmick_creeper",
                    () -> EntityType.Builder.of(TimedGimmickCreeperEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("timed_gimmick_creeper"));

    /** 衰弱の殉教者 - the Wither-cloud summon (HP 20, 7 at a time). */
    public static final RegistryObject<EntityType<DebilitationMartyrEntity>> DEBILITATION_MARTYR =
            ENTITY_TYPES.register("debilitation_martyr",
                    () -> EntityType.Builder.of(DebilitationMartyrEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 2.4F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .fireImmune()
                            .build("debilitation_martyr"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(POTION_MASTER.get(), PotionMasterEntity.createAttributes().build());
        event.put(FANG_KING.get(), FangKingEntity.createAttributes().build());
        event.put(CENTIPEDE_BOSS.get(), CentipedeBossEntity.createAttributes().build());
        event.put(APOCALYPSE_RED_DRAGON.get(), ApocalypseSevenHeadedRedDragonEntity.createAttributes().build());
        event.put(TIMED_GIMMICK_CREEPER.get(), TimedGimmickCreeperEntity.createAttributes().build());
        event.put(DEBILITATION_MARTYR.get(), DebilitationMartyrEntity.createAttributes().build());
    }

    private ModEntities() {}
}
