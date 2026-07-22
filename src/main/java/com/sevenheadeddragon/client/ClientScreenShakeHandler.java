package com.sevenheadeddragon.client;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies the Centipede boss's screen-shake effect via Forge's official
 * {@code ViewportEvent.ComputeCameraAngles} hook (confirmed real, current
 * Forge API for exactly this purpose - no client Mixin needed).
 */
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientScreenShakeHandler {

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        event.setYaw(event.getYaw() + ScreenShakeManager.getYawOffset());
        event.setPitch(event.getPitch() + ScreenShakeManager.getPitchOffset());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ScreenShakeManager.tickDown();
        }
    }
}
