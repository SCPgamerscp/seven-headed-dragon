package com.sevenheadeddragon.client.handler;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.registry.ModEffects;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Implements the actual WASD-inversion behavior for the "操作反転"
 * (Control Reversal) custom effect. Per spec, ONLY the forward/back/
 * left/right movement keys are inverted - mouse look/camera direction is
 * completely unaffected ("マウス操作は反転しません、WASDのみ反転します").
 * <p>
 * This is a pure client-side input transform: while the local player has
 * the Control Reversal MobEffect active, the raw keyboard-derived movement
 * input vector is negated before it's used to compute actual movement.
 */
<<<<<<< HEAD
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
=======
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
>>>>>>> origin/genspark_ai_developer
public final class ControlReversalHandler {

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.CONTROL_REVERSAL.get())) {
            return;
        }

        Input input = event.getInput();

        // Invert the raw impulse axes (forward/back, strafe left/right).
        input.forwardImpulse = -input.forwardImpulse;
        input.leftImpulse = -input.leftImpulse;

        // Also swap the discrete key-state booleans so any code reading
        // them directly (rather than the impulse floats) sees consistent
        // inverted directions too.
        boolean up = input.up;
        boolean down = input.down;
        boolean left = input.left;
        boolean right = input.right;
        input.up = down;
        input.down = up;
        input.left = right;
        input.right = left;
    }

    private ControlReversalHandler() {}
}
