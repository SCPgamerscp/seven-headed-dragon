package com.sevenheadeddragon.item;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.function.Supplier;

/**
 * Custom ForgeSpawnEggItem subclass that overrides getColor() to return 0xFFFFFF (no tinting),
 * allowing custom PNG textures to render in their 100% crisp original natural colors.
 */
public class CustomBossSpawnEggItem extends ForgeSpawnEggItem {

    public CustomBossSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int primaryColor, int secondaryColor, Item.Properties props) {
        super(type, primaryColor, secondaryColor, props);
    }

    @Override
    public int getColor(int tintIndex) {
        // Return pure white (0xFFFFFF / no tinting multiplier) for all model layers
        return 0xFFFFFF;
    }
}
