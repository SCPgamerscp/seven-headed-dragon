package com.sevenheadeddragon.network;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Sets up the mod's network channel. Carries the {@link ScreenShakePacket}
 * used by the Centipede boss's walking AoE attack and the {@link RedWorldPacket}
 * used by the Apocalypse Seven Headed Red Dragon's Red World effect.
 */
public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SevenHeadedDragon.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, ScreenShakePacket.class,
                ScreenShakePacket::encode, ScreenShakePacket::decode, ScreenShakePacket::handle);
        CHANNEL.registerMessage(nextId++, RedWorldPacket.class,
                RedWorldPacket::encode, RedWorldPacket::decode, RedWorldPacket::handle);
        CHANNEL.registerMessage(nextId++, WingBoostPacket.class,
                WingBoostPacket::encode, WingBoostPacket::decode, WingBoostPacket::handle);
    }

    private ModNetworking() {}
}
