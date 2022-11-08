package net.gnomecraft.ductwork.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.gnomecraft.ductwork.Ductwork;

public class DuctworkItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public DuctworkItemTagProvider(FabricDataGenerator generator) {
        super(generator);
    }

    @Override
    protected void generateTags() {
        this.getOrCreateTagBuilder(Ductwork.DUCT_ITEMS)
                .add(Ductwork.COLLECTOR_ITEM)
                .add(Ductwork.DAMPER_ITEM)
                .add(Ductwork.DUCT_ITEM);
    }
}
