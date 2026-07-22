package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom sound events for the Centipede Boss (walking loop, occasional cry).
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SevenHeadedDragon.MODID);

    public static final RegistryObject<SoundEvent> CENTIPEDE_WALK = SOUND_EVENTS.register("centipede.walk",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SevenHeadedDragon.MODID, "centipede.walk")));

    public static final RegistryObject<SoundEvent> CENTIPEDE_YOUCHU = SOUND_EVENTS.register("centipede.youchu",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SevenHeadedDragon.MODID, "centipede.youchu")));

    private ModSounds() {}
}
