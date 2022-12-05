package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class DuctworkDatagen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator dataGenerator) {
        FabricDataGenerator.Pack pack = dataGenerator.createPack();

        pack.addProvider(DuctworkBlockLootTableProvider::new);
        pack.addProvider(DuctworkBlockTagProvider::new);
        pack.addProvider(DuctworkItemTagProvider::new);
        pack.addProvider(DuctworkRecipeProvider::new);
    }
}
