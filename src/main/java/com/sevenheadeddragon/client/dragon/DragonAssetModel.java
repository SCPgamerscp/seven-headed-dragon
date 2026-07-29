package com.sevenheadeddragon.client.dragon;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * Shared {@link GeoModel} for every asset that ships under the Red Dragon's
 * {@code .../dragon/} asset folder. All five models (dragon, goat, longinus,
 * squid, wither_skeleton) follow the exact same naming convention:
 *
 * <pre>
 *   geo/dragon/&lt;name&gt;.geo.json
 *   textures/entity/dragon/&lt;name&gt;.png
 *   animations/dragon/&lt;name&gt;.animation.json
 * </pre>
 *
 * so one parameterised base class removes five copies of identical boilerplate.
 */
public class DragonAssetModel<T extends GeoAnimatable> extends GeoModel<T> {

    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public DragonAssetModel(String name) {
        this.model = new ResourceLocation(SevenHeadedDragon.MODID, "geo/dragon/" + name + ".geo.json");
        this.texture = new ResourceLocation(SevenHeadedDragon.MODID, "textures/entity/dragon/" + name + ".png");
        this.animation = new ResourceLocation(SevenHeadedDragon.MODID, "animations/dragon/" + name + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return this.animation;
    }
}
