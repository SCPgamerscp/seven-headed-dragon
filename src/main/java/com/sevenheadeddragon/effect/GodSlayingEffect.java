package com.sevenheadeddragon.effect;

import com.sevenheadeddragon.util.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 神殺し (God Slaying).
 * <p>
 * Applied by a direct hit from the dragon's ロンギヌスの槍 (Longinus Spear).
 * Each second it strips 1/10 (10%) of the victim's <em>maximum</em> health -
 * a percentage-based damage-over-time that scales with (and therefore
 * completely ignores) any max-health buffs the player has stacked, per the
 * spec's "最大HPの1/10（10%）ずつを削り取る強力な割合継続ダメージ".
 * <p>
 * Unlike vanilla Wither/Poison this <em>can</em> kill, and it uses its own
 * data-driven DamageType so a kill produces the dedicated death message
 * ("死.attack.sevenheadeddragon.god_slaying").
 */
public class GodSlayingEffect extends MobEffect {

    /** 1/10 of MAX HP per interval, per amplifier level. */
    public static final float PERCENT_PER_TICK = 0.10F;

    /** Damage interval: once per second. */
    private static final int TICK_INTERVAL = 20;

    public GodSlayingEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFF6C8);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        float damage = entity.getMaxHealth() * PERCENT_PER_TICK * (amplifier + 1);
        if (damage <= 0.0F) return;
        entity.hurt(ModDamageTypes.source(entity, ModDamageTypes.GOD_SLAYING), damage);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }
}
