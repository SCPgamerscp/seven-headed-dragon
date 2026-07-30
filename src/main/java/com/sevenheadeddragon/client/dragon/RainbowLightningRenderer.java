package com.sevenheadeddragon.client.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sevenheadeddragon.entity.dragon.RainbowLightningEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * Renders 七色の雷 (Rainbow Lightning) using vanilla-style jagged, branching lightning geometry
 * tinted with the entity's synched rainbow color.
 */
public class RainbowLightningRenderer extends EntityRenderer<RainbowLightningEntity> {

    public RainbowLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(RainbowLightningEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public boolean shouldRender(RainbowLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(RainbowLightningEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        int rgb = entity.getColor();
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        float life = Mth.clamp(entity.getLife() / (float) RainbowLightningEntity.STRIKE_LIFETIME_TICKS, 0.0F, 1.0F);
        if (life <= 0.0F) return;
        float alpha = life * life;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f pose = poseStack.last().pose();

        RandomSource random = RandomSource.create(entity.getId() * 31L + entity.tickCount);

        // Generate jagged segments from ground to sky
        float[] offX = new float[16];
        float[] offZ = new float[16];
        float curX = 0.0F;
        float curZ = 0.0F;
        for (int i = 0; i < 16; i++) {
            offX[i] = curX;
            offZ[i] = curZ;
            curX += (random.nextFloat() - 0.5F) * 0.9F;
            curZ += (random.nextFloat() - 0.5F) * 0.9F;
        }

        // Draw outer glow and inner core quad strips
        for (int layer = 0; layer < 2; layer++) {
            float width = (layer == 0) ? 0.40F : 0.15F;
            float r = (layer == 0) ? red : Math.min(1.0F, red + 0.5F);
            float g = (layer == 0) ? green : Math.min(1.0F, green + 0.5F);
            float b = (layer == 0) ? blue : Math.min(1.0F, blue + 0.5F);
            float a = (layer == 0) ? alpha * 0.45F : alpha;

            for (int i = 0; i < 15; i++) {
                float y0 = (float) RainbowLightningEntity.BEAM_HEIGHT * i / 15.0F;
                float y1 = (float) RainbowLightningEntity.BEAM_HEIGHT * (i + 1) / 15.0F;

                float x0 = offX[i];
                float z0 = offZ[i];
                float x1 = offX[i + 1];
                float z1 = offZ[i + 1];

                drawSegmentQuad(consumer, pose, x0, y0, z0, x1, y1, z1, width, r, g, b, a);
            }
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawSegmentQuad(VertexConsumer consumer, Matrix4f pose,
                                       float x0, float y0, float z0,
                                       float x1, float y1, float z1,
                                       float width, float r, float g, float b, float a) {
        consumer.vertex(pose, x0 - width, y0, z0 - width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x0 + width, y0, z0 + width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 + width, y1, z1 + width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 - width, y1, z1 - width).color(r, g, b, a).endVertex();

        consumer.vertex(pose, x0 - width, y0, z0 + width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x0 + width, y0, z0 - width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 + width, y1, z1 - width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 - width, y1, z1 + width).color(r, g, b, a).endVertex();
    }
}
