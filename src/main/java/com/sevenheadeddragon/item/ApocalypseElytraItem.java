package com.sevenheadeddragon.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.UUID;

/**
 * 🪽 元熾天使の翼 (Apocalypse Elytra).
 * <p>
 * A legendary boss reward item providing:
 * <ul>
 *   <li>Infinite, indestructible Elytra flight (never breaks, never stops gliding)</li>
 *   <li>+20 Armor (+10 Netherite Chestplate equivalent)</li>
 *   <li>+10 Armor Toughness</li>
 *   <li>+1.0 Knockback Resistance (100% knockback immunity)</li>
 *   <li>Enchanted with Protection 100 & Unbreakable</li>
 * </ul>
 */
public class ApocalypseElytraItem extends ElytraItem {

    private static final UUID CHEST_ARMOR_UUID = UUID.fromString("9F3D476D-C118-4544-8065-64F4BE884B20");
    private static final UUID CHEST_TOUGHNESS_UUID = UUID.fromString("D84AF4CB-F300-4F7C-AB5B-888B357B9612");
    private static final UUID CHEST_KNOCKBACK_UUID = UUID.fromString("64B47069-42E0-466D-B072-E400B68E65D7");

    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public ApocalypseElytraItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC).durability(0).fireResistant());

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ARMOR, new AttributeModifier(CHEST_ARMOR_UUID, "Armor modifier", 20.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(CHEST_TOUGHNESS_UUID, "Armor toughness modifier", 10.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(CHEST_KNOCKBACK_UUID, "Armor knockback modifier", 1.0D, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }
}
