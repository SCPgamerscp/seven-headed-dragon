package com.sevenheadeddragon.client;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.client.dragon.ApocalypseSevenHeadedRedDragonRenderer;
import com.sevenheadeddragon.client.dragon.DebilitationMartyrRenderer;
import com.sevenheadeddragon.client.dragon.DragonMagicCircleRenderer;
import com.sevenheadeddragon.client.dragon.GoatMissileRenderer;
import com.sevenheadeddragon.client.dragon.LonginusSpearRenderer;
import com.sevenheadeddragon.client.dragon.RainbowLightningRenderer;
import com.sevenheadeddragon.client.dragon.SquidMissileRenderer;
import com.sevenheadeddragon.client.dragon.TimedGimmickCreeperRenderer;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the client-side entity renderers for all custom entities:
 * the Potion Master boss (reusing vanilla Witch model/texture), the
 * Magic Circle telegraph entity (custom flat-quad renderer), and the
 * Potion Bullet projectile (reusing vanilla splash potion item render).
 */
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.POTION_MASTER.get(), PotionMasterRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGIC_CIRCLE.get(), MagicCircleRenderer::new);
        event.registerEntityRenderer(ModEntities.FANG_KING.get(), FangKingRenderer::new);
        event.registerEntityRenderer(ModEntities.FANG_CONDUCTOR.get(), FangConductorRenderer::new);
        event.registerEntityRenderer(ModEntities.CENTIPEDE_BOSS.get(), CentipedeRenderer::new);
        event.registerEntityRenderer(ModEntities.WORM_DRAGON.get(), WormDragonRenderer::new);

        // 終末の七つ頭の赤い竜 and its whole attack cast.
        event.registerEntityRenderer(ModEntities.APOCALYPSE_RED_DRAGON.get(),
                ApocalypseSevenHeadedRedDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.GOAT_MISSILE.get(), GoatMissileRenderer::new);
        event.registerEntityRenderer(ModEntities.SQUID_MISSILE.get(), SquidMissileRenderer::new);
        event.registerEntityRenderer(ModEntities.LONGINUS_SPEAR.get(), LonginusSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.RAINBOW_LIGHTNING.get(), RainbowLightningRenderer::new);
        event.registerEntityRenderer(ModEntities.DRAGON_MAGIC_CIRCLE.get(), DragonMagicCircleRenderer::new);
        event.registerEntityRenderer(ModEntities.TIMED_GIMMICK_CREEPER.get(), TimedGimmickCreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.DEBILITATION_MARTYR.get(), DebilitationMartyrRenderer::new);
        event.registerEntityRenderer(ModEntities.DRAGON_CLONE_DIVE.get(), com.sevenheadeddragon.client.dragon.DragonCloneDiveRenderer::new);
    }

    private ClientSetup() {}
}
