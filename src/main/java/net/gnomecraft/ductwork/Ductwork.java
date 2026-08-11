package net.gnomecraft.ductwork;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.gnomecraft.ductwork.collector.CollectorBlock;
import net.gnomecraft.ductwork.collector.CollectorEntity;
import net.gnomecraft.ductwork.collector.CollectorScreenHandler;
import net.gnomecraft.ductwork.compat.NeighborChecks;
import net.gnomecraft.ductwork.config.DuctworkConfig;
import net.gnomecraft.ductwork.damper.DamperBlock;
import net.gnomecraft.ductwork.damper.DamperEntity;
import net.gnomecraft.ductwork.damper.DamperScreenHandler;
import net.gnomecraft.ductwork.duct.DuctBlock;
import net.gnomecraft.ductwork.duct.DuctEntity;
import net.gnomecraft.ductwork.duct.DuctScreenHandler;
import net.gnomecraft.ductwork.fabricresourcecondition.DuctworkResourceConditions;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class Ductwork implements ModInitializer {
    public static final String MOD_ID = "ductwork";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier COLLECTOR_ID = Identifier.fromNamespaceAndPath(MOD_ID, "collector");
    public static final Identifier DAMPER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "damper");
    public static final Identifier DUCT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "duct");

    public static final ResourceKey<Block> COLLECTOR_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, COLLECTOR_ID);
    public static final ResourceKey<Block> DAMPER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, DAMPER_ID);
    public static final ResourceKey<Block> DUCT_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, DUCT_ID);

    public static final ResourceKey<Item> COLLECTOR_ITEM_KEY = ResourceKey.create(Registries.ITEM, COLLECTOR_ID);
    public static final ResourceKey<Item> DAMPER_ITEM_KEY = ResourceKey.create(Registries.ITEM, DAMPER_ID);
    public static final ResourceKey<Item> DUCT_ITEM_KEY = ResourceKey.create(Registries.ITEM, DUCT_ID);

    public static final ResourceKey<BlockEntityType<?>> COLLECTOR_BLOCK_ENTITY_TYPE_KEY = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, COLLECTOR_ID);
    public static final ResourceKey<BlockEntityType<?>> DAMPER_BLOCK_ENTITY_TYPE_KEY = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, DAMPER_ID);
    public static final ResourceKey<BlockEntityType<?>> DUCT_BLOCK_ENTITY_TYPE_KEY = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, DUCT_ID);

    public static final ResourceKey<MenuType<?>> COLLECTOR_MENU_TYPE_KEY = ResourceKey.create(Registries.MENU, COLLECTOR_ID);
    public static final ResourceKey<MenuType<?>> DAMPER_MENU_TYPE_KEY = ResourceKey.create(Registries.MENU, DAMPER_ID);
    public static final ResourceKey<MenuType<?>> DUCT_MENU_TYPE_KEY = ResourceKey.create(Registries.MENU, DUCT_ID);

    public static final TagKey<Block> DUCT_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "ducts"));
    public static final TagKey<Item> DUCT_ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "ducts"));
    public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "wrenches"));

    // Collector block
    public static final Block COLLECTOR_BLOCK = Registry.register(BuiltInRegistries.BLOCK, COLLECTOR_BLOCK_KEY, new CollectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).mapColor(MapColor.METAL).setId(COLLECTOR_BLOCK_KEY)));
    public static final BlockItem COLLECTOR_ITEM = Registry.register(BuiltInRegistries.ITEM, COLLECTOR_ITEM_KEY, new BlockItem(COLLECTOR_BLOCK, new net.minecraft.world.item.Item.Properties().setId(COLLECTOR_ITEM_KEY).useBlockDescriptionPrefix()));
    public static final BlockEntityType<CollectorEntity> COLLECTOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, COLLECTOR_BLOCK_ENTITY_TYPE_KEY, FabricBlockEntityTypeBuilder.create(CollectorEntity::new, COLLECTOR_BLOCK).build());
    public static final MenuType<CollectorScreenHandler> COLLECTOR_SCREEN_HANDLER = Registry.register(BuiltInRegistries.MENU, COLLECTOR_MENU_TYPE_KEY, new MenuType<>(CollectorScreenHandler::new, FeatureFlagSet.of()));

    // Damper block
    public static final Block DAMPER_BLOCK = Registry.register(BuiltInRegistries.BLOCK, DAMPER_BLOCK_KEY, new DamperBlock(BlockBehaviour.Properties.ofFullCopy(COLLECTOR_BLOCK).setId(DAMPER_BLOCK_KEY)));
    public static final BlockItem DAMPER_ITEM = Registry.register(BuiltInRegistries.ITEM, DAMPER_ITEM_KEY, new BlockItem(DAMPER_BLOCK, new net.minecraft.world.item.Item.Properties().setId(DAMPER_ITEM_KEY).useBlockDescriptionPrefix()));
    public static final BlockEntityType<DamperEntity> DAMPER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DAMPER_BLOCK_ENTITY_TYPE_KEY, FabricBlockEntityTypeBuilder.create(DamperEntity::new, DAMPER_BLOCK).build());
    public static final MenuType<DamperScreenHandler> DAMPER_SCREEN_HANDLER = Registry.register(BuiltInRegistries.MENU, DAMPER_MENU_TYPE_KEY, new MenuType<>(DamperScreenHandler::new, FeatureFlagSet.of()));

    // Duct block
    public static final Block DUCT_BLOCK = Registry.register(BuiltInRegistries.BLOCK, DUCT_BLOCK_KEY, new DuctBlock(BlockBehaviour.Properties.ofFullCopy(COLLECTOR_BLOCK).setId(DUCT_BLOCK_KEY)));
    public static final BlockItem DUCT_ITEM = Registry.register(BuiltInRegistries.ITEM, DUCT_ITEM_KEY, new BlockItem(DUCT_BLOCK, new net.minecraft.world.item.Item.Properties().setId(DUCT_ITEM_KEY).useBlockDescriptionPrefix()));
    public static final BlockEntityType<DuctEntity> DUCT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DUCT_BLOCK_ENTITY_TYPE_KEY, FabricBlockEntityTypeBuilder.create(DuctEntity::new, DUCT_BLOCK).build());
    public static final MenuType<DuctScreenHandler> DUCT_SCREEN_HANDLER = Registry.register(BuiltInRegistries.MENU, DUCT_MENU_TYPE_KEY, new MenuType<>(DuctScreenHandler::new, FeatureFlagSet.of()));

    @Override
    public void onInitialize() {
        // Register the Ductwork config
        AutoConfig.register(DuctworkConfig.class, Toml4jConfigSerializer::new);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(content -> content.insertAfter(Items.HOPPER, DUCT_ITEM, DAMPER_ITEM, COLLECTOR_ITEM));

        // Initialize modules
        DuctworkResourceConditions.init();
        NeighborChecks.init();

        LOGGER.info("Ductwork makes the Dreamwork!");
    }

    public static DuctworkConfig getConfig() {
        return AutoConfig.getConfigHolder(DuctworkConfig.class).getConfig();
    }
}