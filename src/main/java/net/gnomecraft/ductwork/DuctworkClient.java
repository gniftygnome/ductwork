package net.gnomecraft.ductwork;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.gnomecraft.ductwork.collector.CollectorScreen;
import net.gnomecraft.ductwork.damper.DamperScreen;
import net.gnomecraft.ductwork.duct.DuctScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DuctworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(Ductwork.COLLECTOR_SCREEN_HANDLER, CollectorScreen::new);
        HandledScreens.register(Ductwork.DAMPER_SCREEN_HANDLER, DamperScreen::new);
        HandledScreens.register(Ductwork.DUCT_SCREEN_HANDLER, DuctScreen::new);

        FabricLoader.getInstance().getModContainer("ductwork").ifPresent(modContainer -> {
            ResourceManagerHelper.registerBuiltinResourcePack(new Identifier(Ductwork.MOD_ID, "directionalducts"),  modContainer, ResourcePackActivationType.NORMAL);
        });
    }
}