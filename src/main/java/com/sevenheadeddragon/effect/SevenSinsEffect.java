package com.sevenheadeddragon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 七つの大罪デバフ (The Seven Deadly Sins).
 * <p>
 * One shared implementation for all seven sin debuffs applied by the
 * Apocalypse Seven Headed Red Dragon's seven-bite combo. Every sin has the
 * exact same mechanical payload per the spec:
 * <ul>
 *   <li>攻撃力 -50% (attack damage multiplied by 0.5)</li>
 *   <li>移動速度 -50% (movement speed multiplied by 0.5)</li>
 * </ul>
 * The individual sins differ only in their name, particle colour and the
 * (unique per sin) attribute-modifier UUIDs, which is what allows all seven
 * to stack simultaneously on the same victim - getting bitten by all seven
 * heads is therefore strictly worse than getting bitten by one.
 * <p>
 * {@link AttributeModifier.Operation#MULTIPLY_TOTAL} with an amount of
 * {@code -0.5} yields exactly "half of the final value", matching the spec's
 * "-50%" wording rather than a flat subtraction.
 */
public class SevenSinsEffect extends MobEffect {

    /** -50%, expressed as a MULTIPLY_TOTAL modifier amount. */
    public static final double HALVE = -0.5D;

    private final Sin sin;

    public SevenSinsEffect(Sin sin) {
        super(MobEffectCategory.HARMFUL, sin.color);
        this.sin = sin;

        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, sin.attackModifierUuid, HALVE,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, sin.speedModifierUuid, HALVE,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public Sin getSin() {
        return this.sin;
    }

    /**
     * The seven sins, in the exact order the dragon's seven heads bite
     * (head 1 = Pride ... head 7 = Lust).
     */
    public enum Sin {
        PRIDE("pride", 0xF2C14E, "1f4d2ba0-0001-4c00-9a01-7c1e5c0a0001", "1f4d2ba0-0001-4c00-9a01-7c1e5c0a0002"),
        WRATH("wrath", 0xD32F2F, "1f4d2ba0-0002-4c00-9a01-7c1e5c0a0001", "1f4d2ba0-0002-4c00-9a01-7c1e5c0a0002"),
        ENVY("envy", 0x2E7D32, "1f4d2ba0-0003-4c00-9a01-7c1e5c0a0001", "1f4d2ba0-0003-4c00-9a01-7c1e5c0a0002"),
        SLOTH("sloth", 0x6D4C41, "1f4d2ba0-0004-4c00-9a01-7c1e5c0a0001", "1f4d2ba0-0004-4c00-9a01-7c1e5c0a0002"),
        GREED("greed", 0xFFD700, "1f4d2ba0-0005-4c00-9a01-7c1e5c0a0001", "1f4d2ba0-0005-4c00-9a01-7c1e5c0a0002"),
        GLUTTONY("gluttony", 0xFF8F00, "1f4d2ba0-0006-4c00-9a01-7c1e5c0a0001", "1f4d2ba0-0006-4c00-9a01-7c1e5c0a0002"),
        LUST("lust", 0xC2185B, "1f4d2ba0-0007-4c00-9a01-7c1e5c0a0001", "1f4d2ba0-0007-4c00-9a01-7c1e5c0a0002");

        public final String id;
        public final int color;
        public final String attackModifierUuid;
        public final String speedModifierUuid;

        Sin(String id, int color, String attackModifierUuid, String speedModifierUuid) {
            this.id = id;
            this.color = color;
            this.attackModifierUuid = attackModifierUuid;
            this.speedModifierUuid = speedModifierUuid;
        }
    }
}
