package com.sevenheadeddragon.client;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.CentipedeBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, value = Dist.CLIENT)
public class ClientHitboxRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (!mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) return;
        if (mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof CentipedeBossEntity parent) {
                for (PartEntity<?> part : parent.getParts()) {
                    if (part != null) {
                        AABB box = part.getBoundingBox();
                        LevelRenderer.renderLineBox(poseStack, buffer, box, 1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
        }

        poseStack.popPose();
    }
}
