package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class DuctworkBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public DuctworkBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.valueLookupBuilder(Ductwork.DUCT_BLOCKS)
                .add(Ductwork.COLLECTOR_BLOCK)
                .add(Ductwork.DAMPER_BLOCK)
                .add(Ductwork.DUCT_BLOCK);

        this.valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(Ductwork.COLLECTOR_BLOCK)
                .add(Ductwork.DAMPER_BLOCK)
                .add(Ductwork.DUCT_BLOCK);
    }

    @Override
    public String getName() {
        return "Ductwork Block Tags";
    }
}
