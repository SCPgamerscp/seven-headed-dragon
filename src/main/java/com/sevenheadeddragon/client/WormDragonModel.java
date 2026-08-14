package com.sevenheadeddragon.client;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.WormDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WormDragonModel extends GeoModel<WormDragonEntity> {
    @Override public ResourceLocation getModelResource(WormDragonEntity entity) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "geo/wormdragon.geo.json");
    }
    @Override public ResourceLocation getTextureResource(WormDragonEntity entity) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "textures/entity/wormdragon.png");
    }
    @Override public ResourceLocation getAnimationResource(WormDragonEntity entity) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "animations/wormdragon.animation.json");
    }
}
