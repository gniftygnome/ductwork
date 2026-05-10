package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class DuctworkBlockLootTableProvider extends FabricBlockLootTableProvider {
    protected DuctworkBlockLootTableProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate() {
        dropSelf(Ductwork.COLLECTOR_BLOCK);
        dropSelf(Ductwork.DAMPER_BLOCK);
        dropSelf(Ductwork.DUCT_BLOCK);
    }

    @Override
    public String getName() {
        return "Ductwork Block Loot Tables";
    }
}
