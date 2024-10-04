package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.fabricresourcecondition.DuctworkResourceConditions;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeGenerator;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class DuctworkRecipeProvider extends FabricRecipeProvider {
    public DuctworkRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup registryLookup, RecipeExporter exporter) {
        return new RecipeGenerator(registryLookup, exporter) {
            @Override
            public void generate() {
                RecipeExporter cheaperExporter = withConditions(exporter, DuctworkResourceConditions.allConfigBooleansEnabled("cheaper"));
                RecipeExporter fullPriceExporter = withConditions(exporter, ResourceConditions.not(DuctworkResourceConditions.anyConfigBooleansEnabled("cheaper")));

                // Cheaper recipes.

                createShaped(RecipeCategory.REDSTONE, Ductwork.COLLECTOR_ITEM, 4)
                        .pattern("Iwi")
                        .pattern("Irw")
                        .pattern("Iwi")
                        .input('I', Items.IRON_INGOT)
                        .input('i', Items.IRON_NUGGET)
                        .input('r', Items.REDSTONE)
                        .input('w', ItemTags.PLANKS)
                        .criterion("has_iron_and_redstone", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT, Items.REDSTONE))
                        .offerTo(cheaperExporter, "collector-cheaper");

                createShaped(RecipeCategory.REDSTONE, Ductwork.DAMPER_ITEM, 4)
                        .pattern("iwi")
                        .pattern("wrw")
                        .pattern("iwi")
                        .input('i', Items.IRON_NUGGET)
                        .input('r', Items.REDSTONE)
                        .input('w', ItemTags.PLANKS)
                        .criterion("has_iron_and_redstone", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT, Items.REDSTONE))
                        .offerTo(cheaperExporter, "damper-cheaper");

                createShaped(RecipeCategory.REDSTONE, Ductwork.DUCT_ITEM, 4)
                        .pattern("iwi")
                        .pattern("w w")
                        .pattern("iwi")
                        .input('i', Items.IRON_NUGGET)
                        .input('w', ItemTags.PLANKS)
                        .criterion("has_iron", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                        .offerTo(cheaperExporter, "duct-cheaper");


                // Full price recipes.

                createShaped(RecipeCategory.REDSTONE, Ductwork.COLLECTOR_ITEM, 1)
                        .pattern("iwi")
                        .pattern("irw")
                        .pattern("iwi")
                        .input('i', Items.IRON_INGOT)
                        .input('r', Items.REDSTONE)
                        .input('w', ItemTags.PLANKS)
                        .criterion("has_iron_and_redstone", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT, Items.REDSTONE))
                        .offerTo(fullPriceExporter, "collector");

                createShaped(RecipeCategory.REDSTONE, Ductwork.DAMPER_ITEM, 4)
                        .pattern("iwi")
                        .pattern("wrw")
                        .pattern("iwi")
                        .input('i', Items.IRON_INGOT)
                        .input('r', Items.REDSTONE)
                        .input('w', ItemTags.PLANKS)
                        .criterion("has_iron_and_redstone", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT, Items.REDSTONE))
                        .offerTo(fullPriceExporter, "damper");

                createShaped(RecipeCategory.REDSTONE, Ductwork.DUCT_ITEM, 4)
                        .pattern("iwi")
                        .pattern("w w")
                        .pattern("iwi")
                        .input('i', Items.IRON_INGOT)
                        .input('w', ItemTags.PLANKS)
                        .criterion("has_iron", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                        .offerTo(fullPriceExporter, "duct");
            }
        };
    }

    @Override
    public String getName() {
        return "Ductwork Recipes";
    }

    @Override
    protected Identifier getRecipeIdentifier(Identifier identifier) {
        return Identifier.of(Ductwork.MOD_ID, identifier.getPath());
    }
}
