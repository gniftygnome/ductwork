package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class DuctworkItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public DuctworkItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries) {
        this.getOrCreateTagBuilder(Ductwork.DUCT_ITEMS)
                .add(Ductwork.COLLECTOR_ITEM)
                .add(Ductwork.DAMPER_ITEM)
                .add(Ductwork.DUCT_ITEM);
    }
}
