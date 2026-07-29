package com.sevenheadeddragon.client.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sevenheadeddragon.entity.dragon.ApocalypseSevenHeadedRedDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders 終末の七つ頭の赤い竜.
 * <p>
 * Per spec (「モデルサイズ: 1.0倍」) the model is rendered at its authored scale -
 * no {@code poseStack.scale(...)} is applied.
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
