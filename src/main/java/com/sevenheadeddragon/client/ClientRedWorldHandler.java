package com.sevenheadeddragon.client;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renders the "Red World" ({@code 世界演出}) - while the Apocalypse Seven Headed
 * Red Dragon is alive the entire world is dyed red through the environment fog
 * colour, and the fog is pulled in close so the sky itself reads as blood red.
 * Everything is restored to the normal blue sky once the dragon is defeated.
 *
 * <p>Uses Forge's official {@code ViewportEvent.ComputeFogColor} /
 * {@code ViewportEvent.RenderFog} hooks - no Mixin required.</p>
 */
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientRedWorldHandler {

    /** How close the fog is pulled in at full strength (blocks). */
    private static final float FOG_FAR_AT_FULL = 96.0F;
    private static final float FOG_NEAR_AT_FULL = 8.0F;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RedWorldManager.tick();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RedWorldManager.reset();
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        float strength = RedWorldManager.getStrength((float) event.getPartialTick());
        if (strength <= 0.001F) return;

        event.setRed(Mth.lerp(strength, event.getRed(), RedWorldManager.RED_R));
        event.setGreen(Mth.lerp(strength, event.getGreen(), RedWorldManager.RED_G));
        event.setBlue(Mth.lerp(strength, event.getBlue(), RedWorldManager.RED_B));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float strength = RedWorldManager.getStrength((float) event.getPartialTick());
        if (strength <= 0.001F) return;

        // Never fight with underwater / lava / powder-snow fog - those are gameplay critical.
        FogType fogType = event.getCamera().getFluidInCamera();
        if (fogType != FogType.NONE) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.isSpectator()) return;

        float far = Mth.lerp(strength, event.getFarPlaneDistance(), FOG_FAR_AT_FULL);
        float near = Mth.lerp(strength, event.getNearPlaneDistance(), FOG_NEAR_AT_FULL);
        // Only ever tighten the fog, never push it further out than vanilla would.
        if (far < event.getFarPlaneDistance()) {
            event.setFarPlaneDistance(far);
            event.setNearPlaneDistance(Math.min(near, far - 1.0F));
            event.setCanceled(true);
        }
    }
}
