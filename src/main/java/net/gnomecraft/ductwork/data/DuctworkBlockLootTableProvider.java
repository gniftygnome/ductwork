package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.gnomecraft.ductwork.Ductwork;

public class DuctworkBlockLootTableProvider extends FabricBlockLootTableProvider {
    protected DuctworkBlockLootTableProvider(FabricDataGenerator generator) {
        super(generator);
    }

    @Override
    public void generateBlockLootTables() {
        addDrop(Ductwork.COLLECTOR_BLOCK);
        addDrop(Ductwork.DAMPER_BLOCK);
        addDrop(Ductwork.DUCT_BLOCK);
    }
}
