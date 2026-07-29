package com.sevenheadeddragon.event;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.dragon.ApocalypseSevenHeadedRedDragonEntity;
import com.sevenheadeddragon.network.ModNetworking;
import com.sevenheadeddragon.network.RedWorldPacket;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spawn trigger and world-state controller for the 終末の七つ頭の赤い竜.
 * <p>
 * <b>Spawn condition</b> (spec section 4): kill <b>10 zombies while under
 * 不吉な予感 (Bad Omen)</b> and the dragon appears immediately. This mirrors how
 * the Centipede boss uses "10 spiders under Bad Omen", keeping the mod's
 * summon grammar consistent - Bad Omen is the shared "something is coming"
 * currency across all of this mod's bosses.
 * <p>
 * <b>Red World</b> (世界演出): while any Red Dragon is alive the entire sky and
 * fog are dyed blood red for every player in the dimension. The state is
 * pushed to clients as a simple on/off flag ({@link RedWorldPacket}) which the
 * client fog handler reads; on defeat the flag is cleared and the sky returns
 * to normal blue.
 */
public class RedDragonSpawnHandler {

    /** Zombie kills required while under Bad Omen. */
    private static final int REQUIRED_ZOMBIE_KILLS = 10;

    /** 実績「世界を救った」. */
    public static final ResourceLocation WORLD_SAVED_ADVANCEMENT =
            new ResourceLocation(SevenHeadedDragon.MODID, "kill_apocalypse_red_dragon");

    /**
     * Per-player zombie kill counts while Bad Omen is active. In-memory only
     * (resets on server restart), matching the existing Centipede handler.
     */
    private static final Map<UUID, Integer> zombieKillCounts = new HashMap<>();

    /** Whether the Red World effect is currently believed to be active. */
    private static boolean redWorldActive = false;

    // ------------------------------------------------------------------
    // Spawn trigger
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        // Plain minecraft:zombie only - not husks, drowned or zombie villagers,
        // so the trigger stays a deliberate act rather than incidental.
        if (event.getEntity().getType() != EntityType.ZOMBIE) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        if (!player.hasEffect(MobEffects.BAD_OMEN)) {
            zombieKillCounts.remove(player.getUUID());
            return;
        }

        int count = zombieKillCounts.merge(player.getUUID(), 1, Integer::sum);

        if (count >= REQUIRED_ZOMBIE_KILLS) {
            zombieKillCounts.remove(player.getUUID());
            spawnRedDragon(player);
        } else {
            // Progress feedback, so the player understands the ritual is working.
            player.displayClientMessage(Component.translatable(
                    "message.sevenheadeddragon.red_dragon_progress",
                    count, REQUIRED_ZOMBIE_KILLS).withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    /** Drops progress for players who lost Bad Omen before finishing the ritual. */
    @SubscribeEvent
    public void onServerTickCleanup(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null || event.getServer().getTickCount() % 100 != 0) return;
        if (zombieKillCounts.isEmpty()) return;

        zombieKillCounts.keySet().removeIf(uuid -> {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
            return player == null || !player.hasEffect(MobEffects.BAD_OMEN);
        });
    }

    /** Spawns the dragon a safe distance from the player, then dyes the world red. */
    private void spawnRedDragon(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ApocalypseSevenHeadedRedDragonEntity dragon = ModEntities.APOCALYPSE_RED_DRAGON.get().create(level);
        if (dragon == null) return;

        // The dragon's hitbox is enormous, so it is placed well clear of the
        // player rather than the 6 blocks the smaller bosses use.
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
        double x = player.getX() + Math.cos(angle) * 20.0D;
        double z = player.getZ() + Math.sin(angle) * 20.0D;
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);

        dragon.moveTo(x, surfaceY, z, (float) Math.toDegrees(-angle), 0.0F);
        dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(dragon.blockPosition()),
                MobSpawnType.EVENT, null, null);
        dragon.setTarget(player);
        level.addFreshEntity(dragon);

        // 世界が赤く染まる
        setRedWorld(level.getServer(), true);

        level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 12.0F, 0.4F);

        // Global announcement - this is the mod's final boss.
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.sevenheadeddragon.red_dragon_awakens")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
    }

    // ------------------------------------------------------------------
    // Red World (世界演出)
    // ------------------------------------------------------------------

    /**
     * Keeps the Red World flag in sync with whether a dragon is actually alive.
     * Runs every second so a dragon removed by {@code /kill}, chunk unload or a
     * server restart can never leave the world permanently red.
     */
    @SubscribeEvent
    public void onServerTickRedWorld(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null || server.getTickCount() % 20 != 0) return;

        boolean anyAlive = false;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ApocalypseSevenHeadedRedDragonEntity dragon && dragon.isAlive()) {
                    anyAlive = true;
                    break;
                }
            }
            if (anyAlive) break;
        }

        if (anyAlive != redWorldActive) {
            setRedWorld(server, anyAlive);
        } else if (anyAlive && server.getTickCount() % 200 == 0) {
            // Periodic re-send so players who joined mid-fight also see red.
            broadcastRedWorld(server, true);
        }
    }

    /** Turns the Red World effect on or off for everyone on the server. */
    public static void setRedWorld(MinecraftServer server, boolean active) {
        redWorldActive = active;
        broadcastRedWorld(server, active);
    }

    private static void broadcastRedWorld(MinecraftServer server, boolean active) {
        if (server == null) return;
        ModNetworking.CHANNEL.send(PacketDistributor.ALL.noArg(), new RedWorldPacket(active));
    }

    public static boolean isRedWorldActive() {
        return redWorldActive;
    }

    /**
     * Called from the dragon's {@code die()}: restores the blue sky and plays
     * the victory fanfare.
     */
    public static void onDragonDefeated(ApocalypseSevenHeadedRedDragonEntity dragon) {
        if (!(dragon.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();

        setRedWorld(server, false);

        level.playSound(null, dragon.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.MASTER, 8.0F, 1.0F);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.sevenheadeddragon.red_dragon_defeated")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
    }

    // ------------------------------------------------------------------
    // Advancement
    // ------------------------------------------------------------------

    /**
     * Grants 実績「世界を救った」 to {@code player}.
     * <p>
     * The advancement is defined as a datapack JSON with an impossible trigger,
     * so it can only ever be awarded through this code path - i.e. by actually
     * killing the dragon. Awarding it manually also produces the vanilla
     * challenge-frame toast and the全体チャット放送 automatically, because the
     * advancement JSON sets {@code announce_to_chat}.
     */
    public static void grantWorldSavedAdvancement(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Advancement advancement = server.getAdvancements().getAdvancement(WORLD_SAVED_ADVANCEMENT);
        if (advancement == null) {
            SevenHeadedDragon.LOGGER.warn("Advancement {} is missing - cannot grant 世界を救った",
                    WORLD_SAVED_ADVANCEMENT);
            return;
        }

        var progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
