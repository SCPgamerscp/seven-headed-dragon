package com.sevenheadeddragon.mixin;

import com.sevenheadeddragon.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getMobType", at = @At("HEAD"), cancellable = true)
    private void modifyMobType(CallbackInfoReturnable<MobType> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // If the entity has the Undead Curse, it is treated as UNDEAD
        if (self.hasEffect(ModEffects.UNDEAD_CURSE.get())) {
            cir.setReturnValue(MobType.UNDEAD);
            return;
        }

        // If the entity has Insectify, it is treated as ARTHROPOD
        if (self.hasEffect(ModEffects.INSECTIFY.get())) {
            cir.setReturnValue(MobType.ARTHROPOD);
            return;
        }
    }
}
