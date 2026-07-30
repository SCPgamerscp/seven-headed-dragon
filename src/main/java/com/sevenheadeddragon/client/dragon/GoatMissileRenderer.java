package com.sevenheadeddragon.client.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sevenheadeddragon.entity.dragon.GoatMissileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the 山羊ミサイル (Goat Missile) - the machine-gun projectile fired
 * 30 rounds at a time. It is oriented along its own velocity and tumbles as it
 * flies so a stream of them reads clearly as a barrage.
 */
public class GoatMissileRenderer extends GeoEntityRenderer<GoatMissileEntity> {

    public GoatMissileRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonAssetModel<>("goat"));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack, GoatMissileEntity animatable,
                          software.bernie.geckolib.cache.object.BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {
        // Point the goat along its flight path, then tumble it about that axis.
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(animatable.getTumble() + partialTick * 24.0F));
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
