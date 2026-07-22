package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SevenHeadedDragon.MODID);

    public static final RegistryObject<CreativeModeTab> SEVEN_HEADED_DRAGON_TAB = CREATIVE_MODE_TABS.register(
            "seven_headed_dragon_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sevenheadeddragon"))
                    .icon(() -> ModItems.POTION_MASTER_SUMMON.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.POTION_MASTER_SUMMON.get());
                        output.accept(ModItems.ENERGY_DRINK.get());
                        output.accept(ModItems.FANG_KING_SPAWN_EGG.get());
                        output.accept(ModItems.CENTIPEDE_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
