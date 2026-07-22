package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.CentipedeBossEntity;
import com.sevenheadeddragon.entity.FangConductorEntity;
import com.sevenheadeddragon.entity.FangKingEntity;
import com.sevenheadeddragon.entity.MagicCircleEntity;
import com.sevenheadeddragon.entity.PotionMasterEntity;
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

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(POTION_MASTER.get(), PotionMasterEntity.createAttributes().build());
        event.put(FANG_KING.get(), FangKingEntity.createAttributes().build());
        event.put(CENTIPEDE_BOSS.get(), CentipedeBossEntity.createAttributes().build());
    }

    private ModEntities() {}
}
