package com.sevenheadeddragon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sevenheadeddragon.entity.projectile.PotionBulletEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Renders the potion bullet projectile using the vanilla splash potion
 * item model, tinted to match the randomly rolled effect's display color.
 */
public class PotionBulletRenderer extends EntityRenderer<PotionBulletEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/item/splash_potion.png");

    private final ItemRenderer itemRenderer;

    public PotionBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(PotionBulletEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PotionBulletEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(1.5f, 1.5f, 1.5f);
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, 0,
                poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
