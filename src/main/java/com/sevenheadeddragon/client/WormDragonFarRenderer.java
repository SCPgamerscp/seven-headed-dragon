package com.sevenheadeddragon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.WormDragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ensures the Worm Dragon continues to render across ultra-long distances
 * even when its chunk is outside the client's video settings render distance.
 */
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, value = Dist.CLIENT)
public class WormDragonFarRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = System.currentTimeMillis();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof WormDragonEntity dragon && dragon.isAlive()) {
                // If it was already rendered in this frame by standard chunk pipeline, skip
                if (now - dragon.clientRenderFrame < 15L) {
                    continue;
                }

                double dx = Mth.lerp(partialTick, dragon.xOld, dragon.getX()) - camPos.x;
                double dy = Mth.lerp(partialTick, dragon.yOld, dragon.getY()) - camPos.y;
                double dz = Mth.lerp(partialTick, dragon.zOld, dragon.getZ()) - camPos.z;
                float yaw = Mth.rotLerp(partialTick, dragon.yRotO, dragon.getYRot());

                int packedLight = LightTexture.pack(15, 15);

                poseStack.pushPose();
                dispatcher.render(dragon, dx, dy, dz, yaw, partialTick, poseStack, bufferSource, packedLight);
                poseStack.popPose();
                dragon.clientRenderFrame = now;
            }
        }
    }
}
