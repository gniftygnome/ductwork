package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.fabricresourcecondition.DuctworkResourceConditions;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class DuctworkRecipeProvider extends FabricRecipeProvider {
    public DuctworkRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        RecipeExporter cheaperExporter = withConditions(exporter, DuctworkResourceConditions.allConfigBooleansEnabled("cheaper"));
        RecipeExporter fullPriceExporter = withConditions(exporter, DefaultResourceConditions.not(DuctworkResourceConditions.anyConfigBooleansEnabled("cheaper")));

        // Cheaper recipes.

        new ShapedRecipeJsonBuilder(RecipeCategory.REDSTONE, Ductwork.COLLECTOR_ITEM, 4)
                .pattern("Iwi")
                .pattern("Irw")
                .pattern("Iwi")
                .input('I', Items.IRON_INGOT)
                .input('i', Items.IRON_NUGGET)
                .input('r', Items.REDSTONE)
                .input('w', ItemTags.PLANKS)
                .criterion("has_iron_and_redstone", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT, Items.REDSTONE))
                .offerTo(cheaperExporter, Identifier.of(Ductwork.MOD_ID, "collector-cheaper"));

        new ShapedRecipeJsonBuilder(RecipeCategory.REDSTONE, Ductwork.DAMPER_ITEM, 4)
                .pattern("iwi")
                .pattern("wrw")
                .pattern("iwi")
                .input('i', Items.IRON_NUGGET)
                .input('r', Items.REDSTONE)
                .input('w', ItemTags.PLANKS)
                .criterion("has_iron_and_redstone", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT, Items.REDSTONE))
                .offerTo(cheaperExporter, Identifier.of(Ductwork.MOD_ID, "damper-cheaper"));

        new ShapedRecipeJsonBuilder(RecipeCategory.REDSTONE, Ductwork.DUCT_ITEM, 4)
                .pattern("iwi")
                .pattern("w w")
                .pattern("iwi")
                .input('i', Items.IRON_NUGGET)
                .input('w', ItemTags.PLANKS)
                .criterion("has_iron", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                .offerTo(cheaperExporter, Identifier.of(Ductwork.MOD_ID, "duct-cheaper"));


        // Full price recipes.

        new ShapedRecipeJsonBuilder(RecipeCategory.REDSTONE, Ductwork.COLLECTOR_ITEM, 1)
                .pattern("iwi")
                .pattern("irw")
                .pattern("iwi")
                .input('i', Items.IRON_INGOT)
                .input('r', Items.REDSTONE)
                .input('w', ItemTags.PLANKS)
                .criterion("has_iron_and_redstone", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT, Items.REDSTONE))
                .offerTo(fullPriceExporter, Identifier.of(Ductwork.MOD_ID, "collector"));

        new ShapedRecipeJsonBuilder(RecipeCategory.REDSTONE, Ductwork.DAMPER_ITEM, 4)
                .pattern("iwi")
                .pattern("wrw")
                .pattern("iwi")
                .input('i', Items.IRON_INGOT)
                .input('r', Items.REDSTONE)
                .input('w', ItemTags.PLANKS)
                .criterion("has_iron_and_redstone", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT, Items.REDSTONE))
                .offerTo(fullPriceExporter, Identifier.of(Ductwork.MOD_ID, "damper"));

        new ShapedRecipeJsonBuilder(RecipeCategory.REDSTONE, Ductwork.DUCT_ITEM, 4)
                .pattern("iwi")
                .pattern("w w")
                .pattern("iwi")
                .input('i', Items.IRON_INGOT)
                .input('w', ItemTags.PLANKS)
                .criterion("has_iron", InventoryChangedCriterion.Conditions.items(Items.IRON_INGOT))
                .offerTo(fullPriceExporter, Identifier.of(Ductwork.MOD_ID, "duct"));
    }

    @Override
    protected Identifier getRecipeIdentifier(Identifier identifier) {
        return Identifier.of(Ductwork.MOD_ID, identifier.getPath());
    }
}
