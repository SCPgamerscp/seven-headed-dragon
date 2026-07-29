package com.sevenheadeddragon.client.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sevenheadeddragon.entity.dragon.LonginusSpearEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders ロンギヌスの槍 (the Spear of Longinus). Fifty-plus of these rain down
 * simultaneously in a 20-block cross pattern, so the spear is drawn pointing
 * straight down along its fall vector.
 */
public class LonginusSpearRenderer extends GeoEntityRenderer<LonginusSpearEntity> {

    public LonginusSpearRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonAssetModel<>("longinus"));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack, LonginusSpearEntity animatable,
                          software.bernie.geckolib.cache.object.BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
