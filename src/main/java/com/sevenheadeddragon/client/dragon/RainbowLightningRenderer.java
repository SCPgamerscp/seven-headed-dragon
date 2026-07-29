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
import org.joml.Matrix4f;

/**
 * Renders one bolt of 七色の雷 (Rainbow Lightning / ショッカー・ブレイカー).
 * <p>
 * Vanilla's {@code LightningBoltRenderer} is hard-coded to white, so the bolt
 * is drawn here as a tinted vertical column made of two crossed, tapering
 * quads plus a brighter inner core. The tint comes straight from the entity's
 * synched colour so each of the seven colours reads distinctly, matching the
 * 魔法陣 that warned the player 0.5 seconds earlier.
 */
public class RainbowLightningRenderer extends EntityRenderer<RainbowLightningEntity> {

    /** Half-width of the outer glow at the base, in blocks. */
    private static final float OUTER_HALF_WIDTH = 0.55F;

    /** Half-width of the bright inner core, in blocks. */
    private static final float CORE_HALF_WIDTH = 0.18F;

    /** Number of vertical segments - more segments give the bolt its jagged flicker. */
    private static final int SEGMENTS = 8;

    public RainbowLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(RainbowLightningEntity entity) {
        // Untextured - drawn with RenderType.lightning().
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public boolean shouldRender(RainbowLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        // The beam is 48 blocks tall but the entity's own hitbox is tiny, so
        // never let frustum culling clip it away mid-strike.
        return true;
    }

    @Override
    public void render(RainbowLightningEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        int rgb = entity.getColor();
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        // Fade out over the bolt's short lifetime.
        float life = Mth.clamp(entity.getLife() / (float) RainbowLightningEntity.STRIKE_LIFETIME_TICKS,
                0.0F, 1.0F);
        if (life <= 0.0F) return;
        float alpha = life * life;

        // Flicker so the pillar never looks like a static box.
        long seed = entity.getId() * 31L + entity.tickCount;
        float flicker = 0.75F + (Math.floorMod(seed * 1103515245L + 12345L, 100) / 100.0F) * 0.35F;

        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        float height = RainbowLightningEntity.BEAM_HEIGHT;

        drawColumn(consumer, matrix, OUTER_HALF_WIDTH * flicker, height, red, green, blue, alpha * 0.45F);
        drawColumn(consumer, matrix, CORE_HALF_WIDTH * flicker, height,
                Math.min(1.0F, red + 0.45F), Math.min(1.0F, green + 0.45F), Math.min(1.0F, blue + 0.45F),
                alpha);

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * Draws a tapering vertical column as two crossed quad strips, so the bolt
     * looks the same from every viewing angle without needing to billboard.
     */
    private void drawColumn(VertexConsumer consumer, Matrix4f matrix, float halfWidth, float height,
                            float red, float green, float blue, float alpha) {
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float y0 = height * segment / SEGMENTS;
            float y1 = height * (segment + 1) / SEGMENTS;
            // Taper towards the top so it reads as coming down from the sky.
            float w0 = halfWidth * (1.0F - 0.6F * segment / SEGMENTS);
            float w1 = halfWidth * (1.0F - 0.6F * (segment + 1) / SEGMENTS);
            float a0 = alpha * (1.0F - 0.5F * segment / SEGMENTS);
            float a1 = alpha * (1.0F - 0.5F * (segment + 1) / SEGMENTS);

            // North-south facing quad.
            quad(consumer, matrix, -w0, y0, 0, w0, y0, 0, w1, y1, 0, -w1, y1, 0,
                    red, green, blue, a0, a1);
            // East-west facing quad.
            quad(consumer, matrix, 0, y0, -w0, 0, y0, w0, 0, y1, w1, 0, y1, -w1,
                    red, green, blue, a0, a1);
        }
    }

    private void quad(VertexConsumer consumer, Matrix4f matrix,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float x4, float y4, float z4,
                      float red, float green, float blue, float bottomAlpha, float topAlpha) {
        consumer.vertex(matrix, x1, y1, z1).color(red, green, blue, bottomAlpha).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(red, green, blue, bottomAlpha).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(red, green, blue, topAlpha).endVertex();
        consumer.vertex(matrix, x4, y4, z4).color(red, green, blue, topAlpha).endVertex();
    }
}
