package com.sevenheadeddragon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.MagicCircleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the 魔法陣 (Magic Circle) telegraph entity as a flat, horizontal
 * quad using a transparent PNG texture, per spec ("ボスが召喚される際や弾幕を
 * 放つ際の「魔法陣」は透過テクスチャを貼った平面のモデルとして使う").
 */
public class MagicCircleRenderer extends EntityRenderer<MagicCircleEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SevenHeadedDragon.MODID, "textures/entity/magic_circle.png");

    private static final float SIZE = 3.0f;

    public MagicCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(MagicCircleEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(MagicCircleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.05, 0.0);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                (entity.tickCount + partialTicks) * 1.5f % 360.0f));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity)));
        float half = SIZE / 2.0f;
        var matrix = poseStack.last().pose();

        vertex(consumer, matrix, -half, 0, -half, 0, 0, packedLight);
        vertex(consumer, matrix, -half, 0, half, 0, 1, packedLight);
        vertex(consumer, matrix, half, 0, half, 1, 1, packedLight);
        vertex(consumer, matrix, half, 0, -half, 1, 0, packedLight);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void vertex(VertexConsumer consumer, org.joml.Matrix4f matrix, float x, float y, float z,
                         float u, float v, int light) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 200)
                .uv(u, v)
                .overlayCoords(0, 10)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }
}
