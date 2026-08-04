package com.sevenheadeddragon.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.sevenheadeddragon.client.wing.WingArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * 🪽 元熾天使の翼 (Apocalypse Elytra).
 * <p>
 * Custom GeckoLib 3D Animated Armor/Elytra featuring:
 * <ul>
 *   <li>3D animated wing model (wings_idle / fall animations)</li>
 *   <li>+3000 Defense / Armor</li>
 *   <li>+300 Armor Toughness</li>
 *   <li>+1.0 Knockback Resistance (100% Knockback immunity)</li>
 *   <li>Protection 100 & Unbreakable</li>
 *   <li>Permanent Elytra Flight + Sprint Rocket Boost in mid-air</li>
 * </ul>
 */
public class ApocalypseElytraItem extends ArmorItem implements GeoItem {

    private static final UUID CHEST_ARMOR_UUID = UUID.fromString("9F3D476D-C118-4544-8065-64F4BE884B20");
    private static final UUID CHEST_TOUGHNESS_UUID = UUID.fromString("D84AF4CB-F300-4F7C-AB5B-888B357B9612");
    private static final UUID CHEST_KNOCKBACK_UUID = UUID.fromString("64B47069-42E0-466D-B072-E400B68E65D7");

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("wings_idle");
    private static final RawAnimation ANIM_FALL = RawAnimation.begin().thenLoop("fall");
    private static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("wings_fly");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public ApocalypseElytraItem(Properties properties) {
        super(DUMMY_MATERIAL, Type.CHESTPLATE, properties.rarity(Rarity.EPIC).durability(0).fireResistant());

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ARMOR, new AttributeModifier(CHEST_ARMOR_UUID, "Armor modifier", 3000.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(CHEST_TOUGHNESS_UUID, "Armor toughness modifier", 300.0D, AttributeModifier.Operation.ADDITION));
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
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player && player.getItemBySlot(EquipmentSlot.CHEST) == stack) {
            if (player.isFallFlying()) {
                // Sprint / Space boost in flight!
                if (player.isSprinting()) {
                    Vec3 look = player.getLookAngle();
                    Vec3 current = player.getDeltaMovement();
                    player.setDeltaMovement(current.add(look.x * 0.18D, look.y * 0.18D, look.z * 0.18D));
                    if (level instanceof ServerLevel serverLevel && level.getGameTime() % 2 == 0) {
                        serverLevel.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY(), player.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.05D);
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY(), player.getZ(), 4, 0.1D, 0.1D, 0.1D, 0.02D);
                        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.6F, 1.3F);
                    }
                }
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            Object entity = event.getData(DataTickets.ENTITY);
            if (entity instanceof LivingEntity living) {
                if (living.isFallFlying()) {
                    return event.setAndContinue(ANIM_FLY);
                } else if (!living.onGround() && living.getDeltaMovement().y < -0.1D) {
                    return event.setAndContinue(ANIM_FALL);
                }
            }
            return event.setAndContinue(ANIM_IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private WingArmorRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null) {
                    this.renderer = new WingArmorRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    private static final ArmorMaterial DUMMY_MATERIAL = new ArmorMaterial() {
        @Override public int getDurabilityForType(Type type) { return 0; }
        @Override public int getDefenseForType(Type type) { return 3000; }
        @Override public int getEnchantmentValue() { return 25; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        @Override public String getName() { return "sevenheadeddragon:apocalypse_elytra"; }
        @Override public float getToughness() { return 300.0F; }
        @Override public float getKnockbackResistance() { return 1.0F; }
    };
}
