package com.sevenheadeddragon.client;

import com.sevenheadeddragon.entity.PotionMasterEntity;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Potion Master boss using the exact same model structure and
 * texture as vanilla's Witch entity, per spec ("ポーションマスターのモデルは
 * バニラのウィッチと完全に同じ構造・テクスチャを使用します").
 */
public class PotionMasterRenderer extends MobRenderer<PotionMasterEntity, WitchModel<PotionMasterEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/witch.png");

    public PotionMasterRenderer(EntityRendererProvider.Context context) {
        super(context, new WitchModel<>(context.bakeLayer(ModelLayers.WITCH)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(PotionMasterEntity entity) {
        return TEXTURE;
    }
}
