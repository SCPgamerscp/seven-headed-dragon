package com.sevenheadeddragon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sevenheadeddragon.entity.WormDragonEntity;
import com.sevenheadeddragon.entity.WormDragonPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.entity.PartEntity;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WormDragonRenderer extends GeoEntityRenderer<WormDragonEntity> {

    private static final String[] ALL_BONES = {
            "neck11", "neck10", "neck9", "neck8", "neck7", "neck6", "neck1", "neck2", "neck3", "neck4", "neck5",
            "head", "jaw"
    };

    public WormDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new WormDragonModel());
        shadowRadius = 32.0F;
    }

    @Override
    public void preRender(PoseStack poseStack, WormDragonEntity entity, BakedGeoModel model,
                          MultiBufferSource source, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean reRender, float partialTick, int light, int overlay,
                          float red, float green, float blue, float alpha) {
        poseStack.scale(1.0F, 1.0F, 1.0F);

        // Enable matrix tracking on ALL 25 animated bones (tail, base, neck, head, jaw)
        for (String boneName : ALL_BONES) {
            model.getBone(boneName).ifPresent(bone -> bone.setTrackingMatrices(true));
        }

        super.preRender(poseStack, entity, model, source, buffer, reRender, partialTick, light, overlay,
                red, green, blue, alpha);
    }

    @Override
    public void render(WormDragonEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // Sync client-side multipart hitboxes to real-time bone locations from GeckoLib animation
        GeoModel<WormDragonEntity> model = getGeoModel();
        if (model != null && entity.getParts() != null) {
            for (PartEntity<?> p : entity.getParts()) {
                if (p instanceof WormDragonPart part) {
                    model.getBone(part.partName).ifPresent(bone -> {
                        Vector3d pos = bone.getWorldPosition();
                        if (pos != null && (pos.x != 0.0 || pos.y != 0.0 || pos.z != 0.0)) {
                            part.updatePosWithOld(pos.x, pos.y, pos.z);
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean shouldRender(WormDragonEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }
}
