package com.sevenheadeddragon.client;

import com.sevenheadeddragon.entity.FangKingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerRenderer;

/**
 * Renders the Fang King boss using vanilla's own {@link EvokerRenderer}
 * directly, per spec ("エヴォーカーのモデルを使う"). This is possible because
 * {@link FangKingEntity} extends {@code SpellcasterIllager} (the same base
 * class vanilla's Evoker extends), which {@code EvokerRenderer} requires -
 * so the model, texture, and spellcasting arm-pose animation are all
 * identical to vanilla with zero custom rendering code needed.
 */
public class FangKingRenderer extends EvokerRenderer<FangKingEntity> {

    public FangKingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
