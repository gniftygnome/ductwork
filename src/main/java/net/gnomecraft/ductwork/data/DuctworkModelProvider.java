package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.block.Block;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.ItemModelGenerator;
import net.minecraft.client.data.ModelIds;

public class DuctworkModelProvider extends FabricModelProvider {
    public DuctworkModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator generator) {
        this.registerBlockItemModel(generator, Ductwork.COLLECTOR_BLOCK);
        this.registerBlockItemModel(generator, Ductwork.DUCT_BLOCK);
    }


    @Override
    public void generateItemModels(ItemModelGenerator generator) {
        generator.register(Ductwork.DAMPER_ITEM);
    }

    /*
     * Shorthand for registering just the item model of a block item which uses its block's model.
     */
    private void registerBlockItemModel(BlockStateModelGenerator generator, Block block) {
        generator.registerParentedItemModel(block, ModelIds.getBlockModelId(block));
    }

    @Override
    public String getName() {
        return "Ductwork Models";
    }
}
