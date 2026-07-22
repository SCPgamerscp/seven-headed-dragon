package com.sevenheadeddragon.client;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.CentipedeBossEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Points GeckoLib at the Centipede Boss's already-prepared geo/texture/animation assets.
 */
public class CentipedeModel extends GeoModel<CentipedeBossEntity> {

    @Override
    public ResourceLocation getModelResource(CentipedeBossEntity animatable) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "geo/centipede.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CentipedeBossEntity animatable) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "textures/entity/centipede.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CentipedeBossEntity animatable) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "animations/centipede.animation.json");
    }
}
