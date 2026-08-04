package com.sevenheadeddragon.event;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.network.ModNetworking;
import com.sevenheadeddragon.network.WingBoostPacket;
import com.sevenheadeddragon.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side event handler detecting Space Key hold while gliding with "元熾天使の翼".
 * Provides instantaneous responsive client movement and sends packet to server.
 */
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, value = Dist.CLIENT)
public class ClientWingBoostHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isFallFlying()) {
            if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.APOCALYPSE_ELYTRA.get())) {
                if (mc.options.keyJump.isDown()) {
                    Vec3 look = mc.player.getLookAngle();
                    Vec3 current = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(current.add(look.x * 0.18D, look.y * 0.18D, look.z * 0.18D));
                    ModNetworking.CHANNEL.sendToServer(new WingBoostPacket());
                }
            }
        }
    }

    private ClientWingBoostHandler() {}
}
