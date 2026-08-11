package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class DuctworkBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    public DuctworkBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(Ductwork.DUCT_BLOCKS)
                .add(Ductwork.COLLECTOR_BLOCK_KEY)
                .add(Ductwork.DAMPER_BLOCK_KEY)
                .add(Ductwork.DUCT_BLOCK_KEY);

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(Ductwork.COLLECTOR_BLOCK_KEY)
                .add(Ductwork.DAMPER_BLOCK_KEY)
                .add(Ductwork.DUCT_BLOCK_KEY);
    }

    @Override
    public String getName() {
        return "Ductwork Block Tags";
    }
}
