package com.sevenheadeddragon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 操作反転 (Control Reversal)
 * Marker effect only (server-side) - the actual WASD movement inversion
 * (forward/back/left/right keys only; camera/mouse look is completely
 * unaffected) is implemented client-side in
 * {@link com.sevenheadeddragon.client.handler.ControlReversalHandler} via
 * {@code net.minecraftforge.client.event.MovementInputUpdateEvent}.
 */
public class ControlReversalEffect extends MobEffect {

    public ControlReversalEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF69B4);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
