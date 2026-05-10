package net.gnomecraft.ductwork;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.gnomecraft.ductwork.collector.CollectorScreen;
import net.gnomecraft.ductwork.damper.DamperScreen;
import net.gnomecraft.ductwork.duct.DuctScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class DuctworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Ductwork.COLLECTOR_SCREEN_HANDLER, CollectorScreen::new);
        MenuScreens.register(Ductwork.DAMPER_SCREEN_HANDLER, DamperScreen::new);
        MenuScreens.register(Ductwork.DUCT_SCREEN_HANDLER, DuctScreen::new);

        FabricLoader.getInstance().getModContainer("ductwork").ifPresent(modContainer ->
            ResourceLoader.registerBuiltinPack(
                    Identifier.fromNamespaceAndPath(Ductwork.MOD_ID, "directionalducts"),
                    modContainer,
                    PackActivationType.NORMAL
            )
        );
    }
}