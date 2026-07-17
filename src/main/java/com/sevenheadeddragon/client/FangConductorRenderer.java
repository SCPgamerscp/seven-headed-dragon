package com.sevenheadeddragon.client;

import com.sevenheadeddragon.entity.FangConductorEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * No-op renderer for {@link FangConductorEntity}. This entity is a purely
 * server-side logic controller for the Fang King's attack patterns and is
 * never meant to be visible, so rendering is unconditionally skipped.
 */
public class FangConductorRenderer extends EntityRenderer<FangConductorEntity> {

    private static final ResourceLocation UNUSED_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/witch.png");

    public FangConductorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FangConductorEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(FangConductorEntity entity) {
        return UNUSED_TEXTURE;
    }
}
