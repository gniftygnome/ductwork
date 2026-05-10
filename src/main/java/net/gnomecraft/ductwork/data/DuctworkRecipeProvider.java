package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.fabricresourcecondition.DuctworkResourceConditions;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class DuctworkRecipeProvider extends FabricRecipeProvider {
    public DuctworkRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public RecipeProvider createRecipeProvider(HolderLookup.Provider registryLookup, RecipeOutput exporter) {
        return new RecipeProvider(registryLookup, exporter) {
            @Override
            public void buildRecipes() {
                RecipeOutput cheaperExporter = withConditions(output, DuctworkResourceConditions.allConfigBooleansEnabled("cheaper"));
                RecipeOutput fullPriceExporter = withConditions(output, ResourceConditions.not(DuctworkResourceConditions.anyConfigBooleansEnabled("cheaper")));

                // Cheaper recipes.

                shaped(RecipeCategory.REDSTONE, Ductwork.COLLECTOR_ITEM, 4)
                        .pattern("Iwi")
                        .pattern("Irw")
                        .pattern("Iwi")
                        .define('I', Items.IRON_INGOT)
                        .define('i', Items.IRON_NUGGET)
                        .define('r', Items.REDSTONE)
                        .define('w', ItemTags.PLANKS)
                        .unlockedBy("has_iron_and_redstone", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT, Items.REDSTONE))
                        .save(cheaperExporter, "collector-cheaper");

                shaped(RecipeCategory.REDSTONE, Ductwork.DAMPER_ITEM, 4)
                        .pattern("iwi")
                        .pattern("wrw")
                        .pattern("iwi")
                        .define('i', Items.IRON_NUGGET)
                        .define('r', Items.REDSTONE)
                        .define('w', ItemTags.PLANKS)
                        .unlockedBy("has_iron_and_redstone", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT, Items.REDSTONE))
                        .save(cheaperExporter, "damper-cheaper");

                shaped(RecipeCategory.REDSTONE, Ductwork.DUCT_ITEM, 4)
                        .pattern("iwi")
                        .pattern("w w")
                        .pattern("iwi")
                        .define('i', Items.IRON_NUGGET)
                        .define('w', ItemTags.PLANKS)
                        .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                        .save(cheaperExporter, "duct-cheaper");


                // Full price recipes.

                shaped(RecipeCategory.REDSTONE, Ductwork.COLLECTOR_ITEM, 1)
                        .pattern("iwi")
                        .pattern("irw")
                        .pattern("iwi")
                        .define('i', Items.IRON_INGOT)
                        .define('r', Items.REDSTONE)
                        .define('w', ItemTags.PLANKS)
                        .unlockedBy("has_iron_and_redstone", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT, Items.REDSTONE))
                        .save(fullPriceExporter, "collector");

                shaped(RecipeCategory.REDSTONE, Ductwork.DAMPER_ITEM, 4)
                        .pattern("iwi")
                        .pattern("wrw")
                        .pattern("iwi")
                        .define('i', Items.IRON_INGOT)
                        .define('r', Items.REDSTONE)
                        .define('w', ItemTags.PLANKS)
                        .unlockedBy("has_iron_and_redstone", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT, Items.REDSTONE))
                        .save(fullPriceExporter, "damper");

                shaped(RecipeCategory.REDSTONE, Ductwork.DUCT_ITEM, 4)
                        .pattern("iwi")
                        .pattern("w w")
                        .pattern("iwi")
                        .define('i', Items.IRON_INGOT)
                        .define('w', ItemTags.PLANKS)
                        .unlockedBy("has_iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                        .save(fullPriceExporter, "duct");
            }
        };
    }

    @Override
    public String getName() {
        return "Ductwork Recipes";
    }

    @Override
    protected Identifier getRecipeIdentifier(Identifier identifier) {
        return Identifier.fromNamespaceAndPath(Ductwork.MOD_ID, identifier.getPath());
    }
}
