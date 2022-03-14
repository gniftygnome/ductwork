package net.gnomecraft.ductwork;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.gnomecraft.ductwork.collector.CollectorScreen;
import net.gnomecraft.ductwork.damper.DamperScreen;
import net.gnomecraft.ductwork.duct.DuctScreen;

@Environment(EnvType.CLIENT)
public class DuctworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(Ductwork.COLLECTOR_SCREEN_HANDLER, CollectorScreen::new);
        ScreenRegistry.register(Ductwork.DAMPER_SCREEN_HANDLER, DamperScreen::new);
        ScreenRegistry.register(Ductwork.DUCT_SCREEN_HANDLER, DuctScreen::new);
    }
}