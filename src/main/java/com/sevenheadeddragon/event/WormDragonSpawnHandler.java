package com.sevenheadeddragon.event;

import com.sevenheadeddragon.entity.WormDragonEntity;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Summons the Worm Dragon after ten skeleton kills while the killer has Bad Omen. */
public class WormDragonSpawnHandler {
    private static final int REQUIRED_KILLS = 10;
    private static final Map<UUID, Integer> KILLS = new HashMap<>();

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity().getType() != EntityType.SKELETON) return;
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;
        if (!player.hasEffect(MobEffects.BAD_OMEN)) {
            KILLS.remove(player.getUUID());
            return;
        }
        int count = KILLS.merge(player.getUUID(), 1, Integer::sum);
        if (count >= REQUIRED_KILLS) {
            KILLS.remove(player.getUUID());
            summon(player);
        } else {
            player.displayClientMessage(Component.translatable("message.sevenheadeddragon.worm_dragon_progress", count, REQUIRED_KILLS)
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    private static void summon(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        WormDragonEntity boss = ModEntities.WORM_DRAGON.get().create(level);
        if (boss == null) return;
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
        double x = player.getX() + Math.cos(angle) * 35.0D;
        double z = player.getZ() + Math.sin(angle) * 35.0D;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
        boss.moveTo(x, y, z, (float) Math.toDegrees(-angle), 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.EVENT, null, null);
        boss.setTarget(player);
        level.addFreshEntity(boss);
        level.playSound(null, boss.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 12.0F, 0.35F);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.sevenheadeddragon.worm_dragon_awakens")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
    }

    @SubscribeEvent
    public void cleanup(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 100 != 0) return;
        KILLS.keySet().removeIf(id -> {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            return player == null || !player.hasEffect(MobEffects.BAD_OMEN);
        });
    }
}
