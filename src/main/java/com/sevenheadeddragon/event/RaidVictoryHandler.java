package com.sevenheadeddragon.event;

import com.sevenheadeddragon.entity.FangKingEntity;
import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Spawns the Fang King boss near the players automatically whenever a
 * vanilla Raid ends in victory ("襲撃が全て終わるとプレイヤーのところに召喚される"),
 * every time, with no per-world limit.
 * <p>
 * Implemented via a periodic server-tick scan (using only stable, public
 * vanilla API: {@code ServerLevel#getRaidAt}, {@code Raid#isVictory},
 * {@code Raid#getId}) rather than a Mixin into Raid's internals, since a
 * Mixin into private raid-tick logic would be far more fragile.
 */
public class RaidVictoryHandler {

    /** IDs of raids we've already reacted to, so each victorious raid only spawns one Fang King. */
    private static final Set<Integer> HANDLED_RAID_IDS = new HashSet<>();

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        // Checking once per second is plenty responsive and keeps this cheap.
        if (serverLevel.getGameTime() % 20 != 0) return;

        for (ServerPlayer player : serverLevel.players()) {
            Raid raid = serverLevel.getRaidAt(player.blockPosition());
            if (raid != null && raid.isVictory() && HANDLED_RAID_IDS.add(raid.getId())) {
                spawnFangKing(serverLevel, raid);
            }
        }
    }

    private void spawnFangKing(ServerLevel level, Raid raid) {
        BlockPos center = raid.getCenter();
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, center.getX(), center.getZ());

        FangKingEntity boss = ModEntities.FANG_KING.get().create(level);
        if (boss == null) return;

        boss.moveTo(center.getX() + 0.5, surfaceY, center.getZ() + 0.5, 0.0f, 0.0f);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(center), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(boss);
        level.playSound(null, boss.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0f, 0.8f);
    }
}
