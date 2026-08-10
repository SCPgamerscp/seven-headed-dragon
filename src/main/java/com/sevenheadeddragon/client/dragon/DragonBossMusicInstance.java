package com.sevenheadeddragon.client.dragon;

import com.sevenheadeddragon.client.RedWorldManager;
import com.sevenheadeddragon.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Looping boss music instance for the Apocalypse Seven Headed Red Dragon.
 * Plays Beethoven's 9th Symphony (Ode to Joy) in the SoundSource.MUSIC channel.
 * Fades out smoothly when the boss is defeated.
 */
public class DragonBossMusicInstance extends AbstractTickableSoundInstance {

    private static final int FADE_TICKS = 40;

    public DragonBossMusicInstance() {
        super(ModSounds.DRAGON_BGM.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (RedWorldManager.isActive()) {
            if (this.volume < 1.0F) {
                this.volume = Math.min(1.0F, this.volume + (1.0F / FADE_TICKS));
            }
        } else {
            this.volume = Math.max(0.0F, this.volume - (1.0F / FADE_TICKS));
            if (this.volume <= 0.0F) {
                this.stop();
            }
        }
    }
}
