package net.gnomecraft.ductwork;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
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
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ductwork implements ModInitializer {
    public static final String MOD_ID = "ductwork";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Block COLLECTOR_BLOCK;
    public static BlockItem COLLECTOR_ITEM;
    public static BlockEntityType<CollectorEntity> COLLECTOR_ENTITY;

    public static Block DAMPER_BLOCK;
    public static BlockItem DAMPER_ITEM;
    public static BlockEntityType<DamperEntity> DAMPER_ENTITY;

    public static Block DUCT_BLOCK;
    public static BlockItem DUCT_ITEM;
    public static BlockEntityType<DuctEntity> DUCT_ENTITY;

    public static final ScreenHandlerType<CollectorScreenHandler> COLLECTOR_SCREEN_HANDLER;
    public static final ScreenHandlerType<DamperScreenHandler> DAMPER_SCREEN_HANDLER;
    public static final ScreenHandlerType<DuctScreenHandler> DUCT_SCREEN_HANDLER;

    public static final Identifier CollectorBlockId = new Identifier(MOD_ID, "collector");
    public static final Identifier DamperBlockId = new Identifier(MOD_ID, "damper");
    public static final Identifier DuctBlockId = new Identifier(MOD_ID, "duct");

    public static final TagKey<Block> DUCT_BLOCKS = TagKey.of(RegistryKeys.BLOCK, new Identifier(MOD_ID, "ducts"));
    public static final TagKey<Item> DUCT_ITEMS = TagKey.of(RegistryKeys.ITEM, new Identifier(MOD_ID, "ducts"));
    public static final TagKey<Item> WRENCHES = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "wrenches"));

    @Override
    public void onInitialize() {
        // Register the Ductwork config
        AutoConfig.register(DuctworkConfig.class, Toml4jConfigSerializer::new);

        // Collector block
        COLLECTOR_BLOCK = Registry.register(Registries.BLOCK, CollectorBlockId, new CollectorBlock(FabricBlockSettings.of(Material.BLOCKS_LIGHT, MapColor.IRON_GRAY).requiresTool().strength(3.0f, 4.8f).sounds(BlockSoundGroup.METAL).nonOpaque()));
        COLLECTOR_ITEM = Registry.register(Registries.ITEM, CollectorBlockId, new BlockItem(COLLECTOR_BLOCK, new Item.Settings()));
        COLLECTOR_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, CollectorBlockId, FabricBlockEntityTypeBuilder.create(CollectorEntity::new, COLLECTOR_BLOCK).build(null));

        // Damper block
        DAMPER_BLOCK = Registry.register(Registries.BLOCK, DamperBlockId, new DamperBlock(FabricBlockSettings.of(Material.BLOCKS_LIGHT, MapColor.IRON_GRAY).requiresTool().strength(3.0f, 4.8f).sounds(BlockSoundGroup.METAL).nonOpaque()));
        DAMPER_ITEM = Registry.register(Registries.ITEM, DamperBlockId, new BlockItem(DAMPER_BLOCK, new Item.Settings()));
        DAMPER_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, DamperBlockId, FabricBlockEntityTypeBuilder.create(DamperEntity::new, DAMPER_BLOCK).build(null));

        // Duct block
        DUCT_BLOCK = Registry.register(Registries.BLOCK, DuctBlockId, new DuctBlock(FabricBlockSettings.of(Material.BLOCKS_LIGHT, MapColor.IRON_GRAY).requiresTool().strength(3.0f, 4.8f).sounds(BlockSoundGroup.METAL).nonOpaque()));
        DUCT_ITEM = Registry.register(Registries.ITEM, DuctBlockId, new BlockItem(DUCT_BLOCK, new Item.Settings()));
        DUCT_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, DuctBlockId, FabricBlockEntityTypeBuilder.create(DuctEntity::new, DUCT_BLOCK).build(null));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register((content) -> {
            content.addAfter(Items.HOPPER, DUCT_ITEM, DAMPER_ITEM, COLLECTOR_ITEM);
        });

        // Initialize modules
        DuctworkResourceConditions.init();

        LOGGER.info("Ductwork makes the Dreamwork!");
    }

    public static DuctworkConfig getConfig() {
        return AutoConfig.getConfigHolder(DuctworkConfig.class).getConfig();
    }

    static {
        COLLECTOR_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(CollectorBlockId, CollectorScreenHandler::new);
        DAMPER_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(DamperBlockId, DamperScreenHandler::new);
        DUCT_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(DuctBlockId, DuctScreenHandler::new);
    }
}