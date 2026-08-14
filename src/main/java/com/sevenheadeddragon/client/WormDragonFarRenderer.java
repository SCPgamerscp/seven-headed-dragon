package com.sevenheadeddragon.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.WormDragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Renders the 1.0-scale massive Worm Dragon across extreme distances,
 * using the Wither Storm Mod architecture (expanded far-plane projection matrix & client tracking).
 */
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, value = Dist.CLIENT)
public class WormDragonFarRenderer {

    public static final Set<WormDragonEntity> TRACKED_DRAGONS = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || TRACKED_DRAGONS.isEmpty()) return;

        long now = System.currentTimeMillis();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Matrix4f originalProj = RenderSystem.getProjectionMatrix();
        double fov = mc.options.fov().get();
        float aspect = (float) mc.getWindow().getWidth() / (float) Math.max(1, mc.getWindow().getHeight());
        // Expand OpenGL depth far clipping plane from vanilla 192m to 10,000m
        Matrix4f farProj = new Matrix4f().perspective((float) Math.toRadians(fov), aspect, 0.05F, 10000.0F);
        RenderSystem.setProjectionMatrix(farProj, VertexSorting.DISTANCE_TO_ORIGIN);

        for (WormDragonEntity dragon : TRACKED_DRAGONS) {
            if (dragon != null && dragon.isAlive()) {
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

        bufferSource.endBatch();
        RenderSystem.setProjectionMatrix(originalProj, VertexSorting.DISTANCE_TO_ORIGIN);
    }
}
