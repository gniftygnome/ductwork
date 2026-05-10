package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class DuctworkItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public DuctworkItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.valueLookupBuilder(Ductwork.DUCT_ITEMS)
                .add(Ductwork.COLLECTOR_ITEM)
                .add(Ductwork.DAMPER_ITEM)
                .add(Ductwork.DUCT_ITEM);
    }

    @Override
    public String getName() {
        return "Ductwork Item Tags";
    }
}
