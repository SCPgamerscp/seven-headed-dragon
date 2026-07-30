package com.sevenheadeddragon.network;

import com.sevenheadeddragon.client.RedWorldManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C packet toggling the "Red World" ({@code 世界演出}) effect - while the
 * Apocalypse Seven Headed Red Dragon is alive the whole world is dyed red via
 * environment fog / sky tinting, and it is restored to normal on its defeat.
 */
public class RedWorldPacket {

    private final boolean active;

    public RedWorldPacket(boolean active) {
        this.active = active;
    }

    public static void encode(RedWorldPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.active);
    }

    public static RedWorldPacket decode(FriendlyByteBuf buf) {
        return new RedWorldPacket(buf.readBoolean());
    }

    public static void handle(RedWorldPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        RedWorldManager.setActive(packet.active)));
        context.setPacketHandled(true);
    }
}
