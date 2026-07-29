package com.sevenheadeddragon.client.dragon;

import com.sevenheadeddragon.entity.dragon.DebilitationMartyrEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders 衰弱の殉教者 (the Debilitation Martyr) - the seven HP-20 summons that
 * continuously emit a Wither-inflicting cloud until they are killed. Uses the
 * supplied {@code wither_skeleton.geo.json} / {@code wither_skeleton.png}.
 */
public class DebilitationMartyrRenderer extends GeoEntityRenderer<DebilitationMartyrEntity> {

    public DebilitationMartyrRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonAssetModel<>("wither_skeleton"));
        this.shadowRadius = 0.5F;
    }
}
