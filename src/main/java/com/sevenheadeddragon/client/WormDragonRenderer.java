package com.sevenheadeddragon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sevenheadeddragon.entity.WormDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WormDragonRenderer extends GeoEntityRenderer<WormDragonEntity> {
    public WormDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new WormDragonModel());
        shadowRadius = 16.0F;
    }

    @Override
    public void preRender(PoseStack poseStack, WormDragonEntity entity, BakedGeoModel model,
                          MultiBufferSource source, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean reRender, float partialTick, int light, int overlay,
                          float red, float green, float blue, float alpha) {
        poseStack.scale(1.0F, 1.0F, 1.0F);
        super.preRender(poseStack, entity, model, source, buffer, reRender, partialTick, light, overlay,
                red, green, blue, alpha);
    }

    @Override
    public boolean shouldRender(WormDragonEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }
}
