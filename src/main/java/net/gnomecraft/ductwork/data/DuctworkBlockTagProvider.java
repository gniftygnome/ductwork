package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class DuctworkBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public DuctworkBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries) {
        this.getOrCreateTagBuilder(Ductwork.DUCT_BLOCKS)
                .add(Ductwork.COLLECTOR_BLOCK)
                .add(Ductwork.DAMPER_BLOCK)
                .add(Ductwork.DUCT_BLOCK);

        this.getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(Ductwork.COLLECTOR_BLOCK)
                .add(Ductwork.DAMPER_BLOCK)
                .add(Ductwork.DUCT_BLOCK);
    }

    @Override
    public String getName() {
        return "Ductwork Block Tags";
    }
}
