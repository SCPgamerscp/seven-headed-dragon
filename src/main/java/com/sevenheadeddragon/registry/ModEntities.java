package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.MagicCircleEntity;
import com.sevenheadeddragon.entity.PotionMasterEntity;
import com.sevenheadeddragon.entity.projectile.PotionBulletEntity;
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
                    .noSummon()
                    .build("magic_circle"));

    public static final RegistryObject<EntityType<PotionBulletEntity>> POTION_BULLET =
            ENTITY_TYPES.register("potion_bullet", () -> EntityType.Builder.<PotionBulletEntity>of(PotionBulletEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(32)
                    .updateInterval(2)
                    .noSummon()
                    .build("potion_bullet"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(POTION_MASTER.get(), PotionMasterEntity.createAttributes().build());
    }

    private ModEntities() {}
}
