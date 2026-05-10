package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DuctworkModelProvider extends FabricModelProvider {
    public DuctworkModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        this.registerBlockItemModel(generator, Ductwork.COLLECTOR_BLOCK);
        this.registerBlockItemModel(generator, Ductwork.DUCT_BLOCK);
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        generator.declareCustomModelItem(Ductwork.DAMPER_ITEM);
    }

    /*
     * Shorthand for registering just the item model of a block item which uses its block's model.
     */
    private void registerBlockItemModel(BlockModelGenerators generator, Block block) {
        generator.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block));
    }

    @Override
    public String getName() {
        return "Ductwork Models";
    }
}
