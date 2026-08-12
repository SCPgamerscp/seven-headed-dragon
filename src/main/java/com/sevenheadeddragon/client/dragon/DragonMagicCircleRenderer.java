package com.sevenheadeddragon.client.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.dragon.DragonMagicCircleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Renders the Red Dragon's coloured 魔法陣 telegraph as a flat horizontal quad.
 * <p>
 * Unlike the Potion Master's fixed white circle, this one reads its diameter
 * and its 0xRRGGBB tint from the entity, which is what lets a single class
 * serve both the one big golden ロンギヌスの槍 marker and the 35 individually
 * rainbow-tinted warning circles of the ショッカー・ブレイカー barrage.
 */
public class DragonMagicCircleRenderer extends EntityRenderer<DragonMagicCircleEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SevenHeadedDragon.MODID, "textures/entity/magic_circle.png");

    /** Lifted 0.18m off the ground so it never buries into grass, snow layers, or terrain. */
    private static final double GROUND_OFFSET = 0.18D;

    public DragonMagicCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DragonMagicCircleEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(DragonMagicCircleEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        int rgb = entity.getColor();
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        // Pulse the circle as its warning window elapses so the player can feel
        // the 0.5s fuse before the bolt / spear lands on it.
        int alpha = 190;

        poseStack.pushPose();
        poseStack.translate(0.0D, GROUND_OFFSET, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                (entity.tickCount + partialTick) * entity.getSpinDegreesPerTick() % 360.0F));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        float half = entity.getSize() / 2.0F;

        vertex(consumer, matrix, -half, 0, -half, 0, 0, packedLight, red, green, blue, alpha);
        vertex(consumer, matrix, -half, 0, half, 0, 1, packedLight, red, green, blue, alpha);
        vertex(consumer, matrix, half, 0, half, 1, 1, packedLight, red, green, blue, alpha);
        vertex(consumer, matrix, half, 0, -half, 1, 0, packedLight, red, green, blue, alpha);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                        float u, float v, int light, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(0, 10)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }
}
