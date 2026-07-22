package com.sevenheadeddragon.client;

import com.sevenheadeddragon.SevenHeadedDragon;
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
    }

    private ClientSetup() {}
}
