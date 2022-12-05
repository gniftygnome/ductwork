package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class DuctworkBlockLootTableProvider extends FabricBlockLootTableProvider {
    protected DuctworkBlockLootTableProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output);
    }

    @Override
    public void generate() {
        addDrop(Ductwork.COLLECTOR_BLOCK);
        addDrop(Ductwork.DAMPER_BLOCK);
        addDrop(Ductwork.DUCT_BLOCK);
    }
}
