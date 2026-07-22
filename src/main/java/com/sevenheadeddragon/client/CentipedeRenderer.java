package com.sevenheadeddragon.client;

import com.sevenheadeddragon.entity.CentipedeBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the Centipede Boss via GeckoLib using the provided model/texture/animations.
 */
public class CentipedeRenderer extends GeoEntityRenderer<CentipedeBossEntity> {

    public CentipedeRenderer(EntityRendererProvider.Context context) {
        super(context, new CentipedeModel());
        this.shadowRadius = 1.5F;
    }
}
