package com.sevenheadeddragon.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers vanilla brewing-stand recipes for all 14 custom potions, so
 * players can actually brew them (in addition to obtaining them as a
 * guaranteed one-of-each drop from the Potion Master boss's loot table).
 * <p>
 * Every recipe starts from a vanilla Awkward Potion (the standard base for
 * all custom effect potions) and adds a themed ingredient chosen to evoke
 * each debuff's flavor. Call {@link #register()} once during mod setup
 * (e.g. {@code FMLCommonSetupEvent}).
 */
public final class ModPotionRecipes {

    public static void register() {
        addMix(Items.SPIDER_EYE, ModPotions.PERCENT_POISON);
        addMix(Items.ROTTEN_FLESH, ModPotions.UNDEAD_CURSE);
        addMix(Items.COBWEB, ModPotions.INSECTIFY);
        addMix(Items.FERMENTED_SPIDER_EYE, ModPotions.INSECTICIDE);
        addMix(Items.BLAZE_POWDER, ModPotions.SCORCH);
        addMix(Items.PUFFERFISH, ModPotions.ASPHYXIATION);
        addMix(Items.NETHER_WART, ModPotions.POISON_AMPLIFY);
        addMix(Items.MAGMA_CREAM, ModPotions.MAGMA);
        addMix(Items.FEATHER, ModPotions.FALL_AMPLIFY);
        addMix(Items.SPONGE, ModPotions.WATER_VULNERABILITY);
        addMix(Items.TRIDENT, ModPotions.LIGHTNING_MARK);
        addMix(Items.ENDER_PEARL, ModPotions.CONTROL_REVERSAL);
        addMix(Items.CHORUS_FRUIT, ModPotions.TELEPORT_MARK);
        addMix(Items.FIRE_CHARGE, ModPotions.FIREWORK_MARK);
    }

    private static void addMix(Item ingredient, RegistryObject<Potion> potion) {
        BrewingRecipeRegistry.addRecipe(
                awkwardPotionIngredient(),
                Ingredient.of(ingredient),
                PotionUtils.setPotion(new ItemStack(Items.POTION), potion.get()));
    }

    private static Ingredient awkwardPotionIngredient() {
        return Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD));
    }

    private ModPotionRecipes() {}
}
