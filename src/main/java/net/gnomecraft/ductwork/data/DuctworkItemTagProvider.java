package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class DuctworkItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public DuctworkItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(Ductwork.DUCT_ITEMS)
                .add(Ductwork.COLLECTOR_ITEM_KEY)
                .add(Ductwork.DAMPER_ITEM_KEY)
                .add(Ductwork.DUCT_ITEM_KEY);
    }

    @Override
    public String getName() {
        return "Ductwork Item Tags";
    }
}
