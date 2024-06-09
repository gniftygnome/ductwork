package net.gnomecraft.ductwork;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.gnomecraft.ductwork.collector.CollectorBlock;
import net.gnomecraft.ductwork.collector.CollectorEntity;
import net.gnomecraft.ductwork.collector.CollectorScreenHandler;
import net.gnomecraft.ductwork.config.DuctworkConfig;
import net.gnomecraft.ductwork.damper.DamperBlock;
import net.gnomecraft.ductwork.damper.DamperEntity;
import net.gnomecraft.ductwork.damper.DamperScreenHandler;
import net.gnomecraft.ductwork.duct.DuctBlock;
import net.gnomecraft.ductwork.duct.DuctEntity;
import net.gnomecraft.ductwork.duct.DuctScreenHandler;
import net.gnomecraft.ductwork.fabricresourcecondition.DuctworkResourceConditions;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ductwork implements ModInitializer {
    public static final String MOD_ID = "ductwork";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier COLLECTOR_BLOCK_ID = Identifier.of(MOD_ID, "collector");
    public static final Identifier DAMPER_BLOCK_ID = Identifier.of(MOD_ID, "damper");
    public static final Identifier DUCT_BLOCK_ID = Identifier.of(MOD_ID, "duct");

    public static final TagKey<Block> DUCT_BLOCKS = TagKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "ducts"));
    public static final TagKey<Item> DUCT_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "ducts"));
    public static final TagKey<Item> WRENCHES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "wrenches"));

    public static Block COLLECTOR_BLOCK;
    public static BlockItem COLLECTOR_ITEM;
    public static BlockEntityType<CollectorEntity> COLLECTOR_ENTITY;

    public static Block DAMPER_BLOCK;
    public static BlockItem DAMPER_ITEM;
    public static BlockEntityType<DamperEntity> DAMPER_ENTITY;

    public static Block DUCT_BLOCK;
    public static BlockItem DUCT_ITEM;
    public static BlockEntityType<DuctEntity> DUCT_ENTITY;

    public static ScreenHandlerType<CollectorScreenHandler> COLLECTOR_SCREEN_HANDLER;
    public static ScreenHandlerType<DamperScreenHandler> DAMPER_SCREEN_HANDLER;
    public static ScreenHandlerType<DuctScreenHandler> DUCT_SCREEN_HANDLER;

    @Override
    public void onInitialize() {
        // Register the Ductwork config
        AutoConfig.register(DuctworkConfig.class, Toml4jConfigSerializer::new);

        // Collector block
        COLLECTOR_BLOCK = Registry.register(Registries.BLOCK, COLLECTOR_BLOCK_ID, new CollectorBlock(AbstractBlock.Settings.copy(Blocks.HOPPER).mapColor(MapColor.IRON_GRAY)));
        COLLECTOR_ITEM = Registry.register(Registries.ITEM, COLLECTOR_BLOCK_ID, new BlockItem(COLLECTOR_BLOCK, new Item.Settings()));
        COLLECTOR_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, COLLECTOR_BLOCK_ID, BlockEntityType.Builder.create(CollectorEntity::new, COLLECTOR_BLOCK).build(null));
        COLLECTOR_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, COLLECTOR_BLOCK_ID, new ScreenHandlerType<>(CollectorScreenHandler::new, FeatureSet.empty()));

        // Damper block
        DAMPER_BLOCK = Registry.register(Registries.BLOCK, DAMPER_BLOCK_ID, new DamperBlock(AbstractBlock.Settings.copy(COLLECTOR_BLOCK)));
        DAMPER_ITEM = Registry.register(Registries.ITEM, DAMPER_BLOCK_ID, new BlockItem(DAMPER_BLOCK, new Item.Settings()));
        DAMPER_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, DAMPER_BLOCK_ID, BlockEntityType.Builder.create(DamperEntity::new, DAMPER_BLOCK).build(null));
        DAMPER_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, DAMPER_BLOCK_ID, new ScreenHandlerType<>(DamperScreenHandler::new, FeatureSet.empty()));

        // Duct block
        DUCT_BLOCK = Registry.register(Registries.BLOCK, DUCT_BLOCK_ID, new DuctBlock(AbstractBlock.Settings.copy(COLLECTOR_BLOCK)));
        DUCT_ITEM = Registry.register(Registries.ITEM, DUCT_BLOCK_ID, new BlockItem(DUCT_BLOCK, new Item.Settings()));
        DUCT_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, DUCT_BLOCK_ID, BlockEntityType.Builder.create(DuctEntity::new, DUCT_BLOCK).build(null));
        DUCT_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, DUCT_BLOCK_ID, new ScreenHandlerType<>(DuctScreenHandler::new, FeatureSet.empty()));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE)
                .register(content -> content.addAfter(Items.HOPPER, DUCT_ITEM, DAMPER_ITEM, COLLECTOR_ITEM));

        // Initialize modules
        DuctworkResourceConditions.init();

        LOGGER.info("Ductwork makes the Dreamwork!");
    }

    public static DuctworkConfig getConfig() {
        return AutoConfig.getConfigHolder(DuctworkConfig.class).getConfig();
    }
}