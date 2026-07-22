package com.sevenheadeddragon.event;

import com.sevenheadeddragon.entity.CentipedeBossEntity;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spawns the Centipede Boss ("竜殺しのオオムカデ") when a player kills 10 spiders
 * (minecraft:spider only, not Cave Spiders) while under Bad Omen.
 */
public class CentipedeSpawnHandler {

    private static final int REQUIRED_SPIDER_KILLS = 10;

    /** Per-player spider kill counts while Bad Omen is active. In-memory only (resets on server restart), matching this mod's existing raid-tracking approach. */
    private static final Map<UUID, Integer> spiderKillCounts = new HashMap<>();

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().getType() != EntityType.SPIDER) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        if (!player.hasEffect(MobEffects.BAD_OMEN)) {
            spiderKillCounts.remove(player.getUUID());
            return;
        }

        int count = spiderKillCounts.merge(player.getUUID(), 1, Integer::sum);
        if (count >= REQUIRED_SPIDER_KILLS) {
            spiderKillCounts.remove(player.getUUID());
            spawnCentipede(player);
        }
    }

    /** Periodically drops progress for players who lost Bad Omen without reaching the required kill count. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (spiderKillCounts.isEmpty()) return;
        if (event.getServer() == null || event.getServer().getTickCount() % 100 != 0) return;

        spiderKillCounts.keySet().removeIf(uuid -> {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            return player == null || !player.hasEffect(MobEffects.BAD_OMEN);
        });
    }

    @SubscribeEvent
    public void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getSource().is(com.sevenheadeddragon.util.ModDamageTypes.DRAGON_SLAYING_POISON)) {
            net.minecraft.world.entity.LivingEntity entity = event.getEntity();
            if (entity.getHealth() - event.getAmount() < 1.0F) {
                event.setAmount(Math.max(0.0F, entity.getHealth() - 1.0F));
            }
        }
    }

    private void spawnCentipede(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        CentipedeBossEntity boss = ModEntities.CENTIPEDE_BOSS.get().create(level);
        if (boss == null) return;

        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double x = player.getX() + Math.cos(angle) * 6.0;
        double z = player.getZ() + Math.sin(angle) * 6.0;
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);

        boss.moveTo(x, surfaceY, z, 0.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(boss);
    }
}
