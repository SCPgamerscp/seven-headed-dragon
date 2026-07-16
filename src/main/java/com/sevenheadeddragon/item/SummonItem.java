package com.sevenheadeddragon.item;

import com.sevenheadeddragon.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.BlockPos;

/**
 * 召喚アイテム (Boss Summon Item)
 * Right-clicking on the ground with this item immediately spawns the
 * Potion Master boss at that location, with a magic-circle summon visual.
 * The item is crafted from Redstone Blocks + Glowstone (see data/recipes).
 */
public class SummonItem extends Item {

    public SummonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel serverLevel && !context.getLevel().isClientSide) {
            BlockPos pos = context.getClickedPos().above();

            var boss = ModEntities.POTION_MASTER.get().create(serverLevel);
            if (boss != null) {
                boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        serverLevel.random.nextFloat() * 360.0f, 0.0f);
                serverLevel.addFreshEntity(boss);

                serverLevel.sendParticles(ParticleTypes.WITCH,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        60, 0.5, 0.5, 0.5, 0.1);
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                        80, 0.6, 0.1, 0.6, 0.2);
                serverLevel.playSound(null, pos, SoundEvents.EVOKER_PREPARE_SUMMON,
                        SoundSource.HOSTILE, 2.0f, 0.8f);

                if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
