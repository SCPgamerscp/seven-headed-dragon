package com.sevenheadeddragon.client.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sevenheadeddragon.entity.dragon.SquidMissileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the イカミサイル (Squid Missile) - the homing projectile launched
 * five at a time. Its bundled {@code squid.animation.json} loops continuously
 * (the tentacle swim cycle) while it chases the player.
 */
public class SquidMissileRenderer extends GeoEntityRenderer<SquidMissileEntity> {

    public SquidMissileRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonAssetModel<>("squid"));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack, SquidMissileEntity animatable,
                          software.bernie.geckolib.cache.object.BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot())));
        // The squid model swims head-first along -Y, so tip it forward onto the flight vector.
        poseStack.mulPose(Axis.XP.rotationDegrees(
                90.0F + Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
