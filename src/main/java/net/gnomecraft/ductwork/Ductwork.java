package net.gnomecraft.ductwork;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.gnomecraft.ductwork.collector.CollectorBlock;
import net.gnomecraft.ductwork.collector.CollectorEntity;
import net.gnomecraft.ductwork.collector.CollectorScreenHandler;
import net.gnomecraft.ductwork.damper.DamperBlock;
import net.gnomecraft.ductwork.damper.DamperEntity;
import net.gnomecraft.ductwork.damper.DamperScreenHandler;
import net.gnomecraft.ductwork.duct.DuctBlock;
import net.gnomecraft.ductwork.duct.DuctEntity;
import net.gnomecraft.ductwork.duct.DuctScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ductwork implements ModInitializer {
    public static final String modId = "ductwork";
    public static final Logger LOGGER = LoggerFactory.getLogger(modId);

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

    public static final Identifier CollectorBlockId = new Identifier(modId, "collector");
    public static final Identifier DamperBlockId = new Identifier(modId, "damper");
    public static final Identifier DuctBlockId = new Identifier(modId, "duct");

    /* TODO: Revive this code when we're no longer beholden to Minecraft 1.18.1...
    public static final TagKey<Block> DUCT_BLOCKS = TagKey.of(Registry.BLOCK_KEY, new Identifier("ductwork", "ducts"));
    public static final TagKey<Item> DUCT_ITEMS = TagKey.of(Registry.ITEM_KEY, new Identifier("ductwork", "ducts"));
     */

    @Override
    public void onInitialize() {
        // Collector block
        COLLECTOR_BLOCK = Registry.register(Registry.BLOCK, CollectorBlockId, new CollectorBlock(FabricBlockSettings.of(Material.METAL).hardness(4.0f)));
        COLLECTOR_ITEM = Registry.register(Registry.ITEM, CollectorBlockId, new BlockItem(COLLECTOR_BLOCK, new Item.Settings().group(ItemGroup.REDSTONE)));
        COLLECTOR_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, CollectorBlockId, FabricBlockEntityTypeBuilder.create(CollectorEntity::new, COLLECTOR_BLOCK).build(null));

        // Damper block
        DAMPER_BLOCK = Registry.register(Registry.BLOCK, DamperBlockId, new DamperBlock(FabricBlockSettings.of(Material.METAL).hardness(4.0f)));
        DAMPER_ITEM = Registry.register(Registry.ITEM, DamperBlockId, new BlockItem(DAMPER_BLOCK, new Item.Settings().group(ItemGroup.REDSTONE)));
        DAMPER_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, DamperBlockId, FabricBlockEntityTypeBuilder.create(DamperEntity::new, DAMPER_BLOCK).build(null));

        // Duct block
        DUCT_BLOCK = Registry.register(Registry.BLOCK, DuctBlockId, new DuctBlock(FabricBlockSettings.of(Material.METAL).hardness(4.0f)));
        DUCT_ITEM = Registry.register(Registry.ITEM, DuctBlockId, new BlockItem(DUCT_BLOCK, new Item.Settings().group(ItemGroup.REDSTONE)));
        DUCT_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, DuctBlockId, FabricBlockEntityTypeBuilder.create(DuctEntity::new, DUCT_BLOCK).build(null));

        LOGGER.info("Ductwork makes the Dreamwork!");
    }

    static {
        COLLECTOR_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(CollectorBlockId, CollectorScreenHandler::new);
        DAMPER_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(DamperBlockId, DamperScreenHandler::new);
        DUCT_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(DuctBlockId, DuctScreenHandler::new);
    }
}