package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.tag.BlockTags;

public class DuctworkBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public DuctworkBlockTagProvider(FabricDataGenerator generator) {
        super(generator);
    }

    @Override
    protected void generateTags() {
        this.getOrCreateTagBuilder(Ductwork.DUCT_BLOCKS)
                .add(Ductwork.COLLECTOR_BLOCK)
                .add(Ductwork.DAMPER_BLOCK)
                .add(Ductwork.DUCT_BLOCK);

        this.getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(Ductwork.COLLECTOR_BLOCK)
                .add(Ductwork.DAMPER_BLOCK)
                .add(Ductwork.DUCT_BLOCK);
    }
}
