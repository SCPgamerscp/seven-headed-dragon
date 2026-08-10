package com.sevenheadeddragon.network;

import com.sevenheadeddragon.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S Packet sent when holding Space key while gliding in "元熾天使の翼".
 * Accelerates player velocity on server and spawns rocket flame particles.
 */
public class WingBoostPacket {

    public WingBoostPacket() {}

    public static void encode(WingBoostPacket msg, FriendlyByteBuf buf) {}

    public static WingBoostPacket decode(FriendlyByteBuf buf) {
        return new WingBoostPacket();
    }

    public static void handle(WingBoostPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.isAlive() && player.isFallFlying()) {
                if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.APOCALYPSE_ELYTRA.get())) {
                    Vec3 look = player.getLookAngle();
                    Vec3 current = player.getDeltaMovement();
                    player.setDeltaMovement(current.add(look.x * 0.18D, look.y * 0.18D, look.z * 0.18D));
                    player.hurtMarked = true;

                    ServerLevel level = player.serverLevel();
                    if (level.getGameTime() % 2 == 0) {
                        level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY(), player.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.05D);
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY(), player.getZ(), 4, 0.1D, 0.1D, 0.1D, 0.02D);
                        level.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY(), player.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.02D);
                        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.5F, 1.3F);
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
