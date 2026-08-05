package com.sevenheadeddragon.client.dragon;

import com.sevenheadeddragon.entity.dragon.DragonCloneDiveEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DragonCloneDiveRenderer extends GeoEntityRenderer<DragonCloneDiveEntity> {
    public DragonCloneDiveRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DragonAssetModel<>("dragon"));
    }
}
