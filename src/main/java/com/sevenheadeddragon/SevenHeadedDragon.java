package com.sevenheadeddragon;

import com.mojang.logging.LogUtils;
import com.sevenheadeddragon.event.CentipedeSpawnHandler;
import com.sevenheadeddragon.event.ModCombatEvents;
import com.sevenheadeddragon.event.RaidVictoryHandler;
import com.sevenheadeddragon.network.ModNetworking;
import com.sevenheadeddragon.registry.ModCreativeTabs;
import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.registry.ModEntities;
import com.sevenheadeddragon.registry.ModItems;
import com.sevenheadeddragon.registry.ModParticles;
import com.sevenheadeddragon.registry.ModPotionRecipes;
import com.sevenheadeddragon.registry.ModPotions;
import com.sevenheadeddragon.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

/**
 * Seven Headed Dragon
 * <p>
 * A high-difficulty boss mod for Minecraft Forge 1.20.1.
 * The first boss added by this mod is the "Potion Master" (ポーションマスター),
 * a turn-based, bullet-hell style Witch-boss inspired by Undertale-like
 * dodging gameplay. The mod is named after a future planned final boss
 * (a seven headed dragon) which is not yet implemented.
 */
@Mod(SevenHeadedDragon.MODID)
public class SevenHeadedDragon {

    public static final String MODID = "sevenheadeddragon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SevenHeadedDragon(FMLJavaModLoadingContext context) {
        GeckoLib.initialize();

        var modEventBus = context.getModEventBus();

        // Register everything to the mod event bus
        ModEffects.EFFECTS.register(modEventBus);
        ModPotions.POTIONS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ModCombatEvents());
        MinecraftForge.EVENT_BUS.register(new RaidVictoryHandler());
        MinecraftForge.EVENT_BUS.register(new CentipedeSpawnHandler());

        LOGGER.info("Seven Headed Dragon mod initializing - Potion Master, Fang King, and Centipede bosses loaded.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Register brewing-stand recipes for all 14 custom potions so
        // players can brew them, not just obtain them from boss loot.
        event.enqueueWork(ModPotionRecipes::register);
        event.enqueueWork(ModNetworking::register);
    }
}
