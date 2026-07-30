package com.sevenheadeddragon.client.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sevenheadeddragon.entity.dragon.ApocalypseSevenHeadedRedDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders 終末の七つ頭の赤い竜 with Ender-Dragon style death light beam rays.
 */
public class ApocalypseSevenHeadedRedDragonRenderer
        extends GeoEntityRenderer<ApocalypseSevenHeadedRedDragonEntity> {

    /** Spec: モデルサイズ 1.0倍. */
    public static final float MODEL_SCALE = 1.0F;

    public ApocalypseSevenHeadedRedDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new ApocalypseSevenHeadedRedDragonModel());
        this.shadowRadius = 4.0F;
    }

    @Override
    public void render(ApocalypseSevenHeadedRedDragonEntity entity, float entityYaw,
                       float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (entity.deathTime > 0) {
            float deathProgress = ((float) entity.deathTime + partialTick) / 200.0F;
            renderDeathLightBeams(entity, deathProgress, poseStack, bufferSource);
        }
    }

    /**
     * Recreates vanilla Ender Dragon's radiant 3D light beams shooting out of
     * the dragon's torso during its 10-second death sequence.
     */
    private void renderDeathLightBeams(ApocalypseSevenHeadedRedDragonEntity entity, float progress,
                                       PoseStack poseStack, MultiBufferSource bufferSource) {
        float f1 = (progress > 0.8F) ? (progress - 0.8F) / 0.2F : 0.0F;
        RandomSource random = RandomSource.create(432L);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        poseStack.pushPose();
        poseStack.translate(0.0D, 3.5D, 0.0D);

        for (int i = 0; (float) i < (progress + progress * progress) / 2.0F * 60.0F; ++i) {
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + progress * 90.0F));

            float rayLength = random.nextFloat() * 20.0F + 5.0F + progress * 15.0F;
            float rayWidth = random.nextFloat() * 2.5F + 1.0F + progress * 2.0F;
            Matrix4f matrix = poseStack.last().pose();

            int coreAlpha = (int) (200.0F * (1.0F - f1));

            // Radiant light beam pyramid quad strip
            consumer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(255, 255, 255, coreAlpha).endVertex();
            consumer.vertex(matrix, -0.866F * rayWidth, rayLength, -0.5F * rayWidth).color(255, 215, 0, 0).endVertex();
            consumer.vertex(matrix, 0.866F * rayWidth, rayLength, -0.5F * rayWidth).color(255, 100, 100, 0).endVertex();

            consumer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(255, 255, 255, coreAlpha).endVertex();
            consumer.vertex(matrix, 0.866F * rayWidth, rayLength, -0.5F * rayWidth).color(255, 100, 100, 0).endVertex();
            consumer.vertex(matrix, 0.0F, rayLength, 1.0F * rayWidth).color(255, 215, 0, 0).endVertex();

            consumer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(255, 255, 255, coreAlpha).endVertex();
            consumer.vertex(matrix, 0.0F, rayLength, 1.0F * rayWidth).color(255, 215, 0, 0).endVertex();
            consumer.vertex(matrix, -0.866F * rayWidth, rayLength, -0.5F * rayWidth).color(255, 100, 100, 0).endVertex();
        }

        poseStack.popPose();
    }

    @Override
    public void preRender(PoseStack poseStack, ApocalypseSevenHeadedRedDragonEntity animatable,
                          software.bernie.geckolib.cache.object.BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {
        if (MODEL_SCALE != 1.0F) {
            poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
