package com.sevenheadeddragon.client.wing;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.item.ApocalypseElytraItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WingArmorModel extends GeoModel<ApocalypseElytraItem> {
    @Override
    public ResourceLocation getModelResource(ApocalypseElytraItem animatable) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "geo/wing.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ApocalypseElytraItem animatable) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "textures/models/armor/wing.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ApocalypseElytraItem animatable) {
        return new ResourceLocation(SevenHeadedDragon.MODID, "animations/wing.animation.json");
    }
}
