package com.sevenheadeddragon.registry;

import com.sevenheadeddragon.SevenHeadedDragon;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Currently unused placeholder for future custom particle types
 * (the mod presently reuses vanilla particle types for all visual effects).
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SevenHeadedDragon.MODID);

    private ModParticles() {}
}
