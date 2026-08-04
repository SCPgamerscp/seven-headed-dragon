package com.sevenheadeddragon.client.wing;

import com.sevenheadeddragon.item.ApocalypseElytraItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class WingArmorRenderer extends GeoArmorRenderer<ApocalypseElytraItem> {
    public WingArmorRenderer() {
        super(new WingArmorModel());
    }
}
