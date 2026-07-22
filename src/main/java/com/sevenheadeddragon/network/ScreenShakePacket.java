package com.sevenheadeddragon.network;

import com.sevenheadeddragon.client.ScreenShakeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C packet telling nearby clients to shake their camera - used by the
 * Centipede boss's walking AoE attack ("画面が揺れる").
 */
public class ScreenShakePacket {

    private final float intensityDegrees;
    private final int durationTicks;

    public ScreenShakePacket(float intensityDegrees, int durationTicks) {
        this.intensityDegrees = intensityDegrees;
        this.durationTicks = durationTicks;
    }

    public static void encode(ScreenShakePacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.intensityDegrees);
        buf.writeVarInt(packet.durationTicks);
    }

    public static ScreenShakePacket decode(FriendlyByteBuf buf) {
        return new ScreenShakePacket(buf.readFloat(), buf.readVarInt());
    }

    public static void handle(ScreenShakePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ScreenShakeManager.trigger(packet.intensityDegrees, packet.durationTicks)));
        context.setPacketHandled(true);
    }
}
