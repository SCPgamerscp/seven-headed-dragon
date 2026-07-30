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
 * Renders 七色の雷 (Rainbow Lightning) using 3D volumetric multi-angle cross quads
 * matching vanilla Minecraft's 3D lightning bolt geometry.
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
        float alpha = Math.min(1.0F, life * 1.5F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f pose = poseStack.last().pose();

        // Fixed seed per entity so the bolt doesn't flicker or vanish between ticks
        RandomSource random = RandomSource.create(entity.getId() * 31L);

        // Generate main jagged lightning column from ground to sky
        int segmentCount = 20;
        float[] offX = new float[segmentCount + 1];
        float[] offZ = new float[segmentCount + 1];
        float curX = 0.0F;
        float curZ = 0.0F;
        for (int i = 0; i <= segmentCount; i++) {
            offX[i] = curX;
            offZ[i] = curZ;
            curX += (random.nextFloat() - 0.5F) * 0.8F;
            curZ += (random.nextFloat() - 0.5F) * 0.8F;
        }

        // Render main 3D column (layer 0: outer glow, layer 1: core beam)
        for (int layer = 0; layer < 2; layer++) {
            float width = (layer == 0) ? 0.45F : 0.18F;
            float r = (layer == 0) ? red : Math.min(1.0F, red + 0.6F);
            float g = (layer == 0) ? green : Math.min(1.0F, green + 0.6F);
            float b = (layer == 0) ? blue : Math.min(1.0F, blue + 0.6F);
            float a = (layer == 0) ? alpha * 0.4F : alpha;

            for (int i = 0; i < segmentCount; i++) {
                float y0 = (float) RainbowLightningEntity.BEAM_HEIGHT * i / (float) segmentCount;
                float y1 = (float) RainbowLightningEntity.BEAM_HEIGHT * (i + 1) / (float) segmentCount;

                float x0 = offX[i];
                float z0 = offZ[i];
                float x1 = offX[i + 1];
                float z1 = offZ[i + 1];

                draw3DVolumetricCross(consumer, pose, x0, y0, z0, x1, y1, z1, width, r, g, b, a);
            }
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * Draws a 3D volumetric cross (4 quad planes at 0°, 45°, 90°, 135°)
     * ensuring 100% visibility from every angle with zero invisible edge-on angles.
     */
    private static void draw3DVolumetricCross(VertexConsumer consumer, Matrix4f pose,
                                               float x0, float y0, float z0,
                                               float x1, float y1, float z1,
                                               float width, float r, float g, float b, float a) {
        // Plane 1: X-axis offset
        consumer.vertex(pose, x0 - width, y0, z0).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x0 + width, y0, z0).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 + width, y1, z1).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 - width, y1, z1).color(r, g, b, a).endVertex();

        // Plane 2: Z-axis offset
        consumer.vertex(pose, x0, y0, z0 - width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x0, y0, z0 + width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1, y1, z1 + width).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1, y1, z1 - width).color(r, g, b, a).endVertex();

        // Plane 3: Diagonal 1 (45 deg)
        float d = width * 0.7071F;
        consumer.vertex(pose, x0 - d, y0, z0 - d).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x0 + d, y0, z0 + d).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 + d, y1, z1 + d).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 - d, y1, z1 - d).color(r, g, b, a).endVertex();

        // Plane 4: Diagonal 2 (135 deg)
        consumer.vertex(pose, x0 - d, y0, z0 + d).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x0 + d, y0, z0 - d).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 + d, y1, z1 - d).color(r, g, b, a).endVertex();
        consumer.vertex(pose, x1 - d, y1, z1 + d).color(r, g, b, a).endVertex();
    }
}
