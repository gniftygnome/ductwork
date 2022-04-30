package net.gnomecraft.ductwork.duct;

import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;
import java.util.List;

public class DuctBlock extends DuctworkBlock {
    public static final DirectionProperty FACING = FacingBlock.FACING;
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty EAST  = BooleanProperty.of("east");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty WEST  = BooleanProperty.of("west");
    public static final BooleanProperty DOWN  = BooleanProperty.of("down");
    public static final BooleanProperty UP    = BooleanProperty.of("up");
    public static final VoxelShape[] DUCT_SHAPE_DICT = new VoxelShape[64];

    public DuctBlock(Settings settings) {
        super(settings);

        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.DOWN)
                .with(NORTH, false)
                .with(EAST,  false)
                .with(SOUTH, false)
                .with(WEST,  false)
                .with(DOWN,  false)
                .with(UP,    false)
        );

        // Build the static global shape dictionary once when the first DuctBlock is instantiated.
        if (DUCT_SHAPE_DICT[0] == null) {
            buildShapeDict();
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DuctEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, Ductwork.DUCT_ENTITY, DuctEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            ItemStack mainStack = player.getMainHandStack();
            Direction facing = state.get(FACING);

            if (player.isSneaking()) {
                if (mainStack.isOf(Items.STICK)) {
                    // Sneak + Stick = rotate FACING (pseudowrench)
                    // flags == 0x4 means notify listeners in server only
                    //          0x2 means do update listeners (in general)
                    //          0x1 means do update comparators
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, null));
                } else if (mainStack.isEmpty()) {
                    // Sneak + Empty primary = reverse FACING
                    this.reorient(state, world, pos, facing.getOpposite());
                } else {
                    return ActionResult.PASS;
                }
            } else {
                if (mainStack.isIn(Ductwork.WRENCHES)) {
                    // Wrench in primary = rotate FACING
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, null));
                    // TODO: else if duct-on-duct enabled and main hand is Ductwork, return PASS
                } else {
                    // Otherwise = open container
                    this.openContainer(world, pos, player);
                }
            }
        }

        return ActionResult.SUCCESS;
    }

    // Reorient the primary (FACING) orientation of the block with all necessary updates and notifications.
    @Override
    protected void reorient(BlockState state, World world, BlockPos pos, Direction direction) {
        Direction previous = state.get(FACING);

        if (!direction.equals(previous)) {
            BlockState neighbor1 = world.getBlockState(pos.offset(previous));
            BlockState neighbor2 = world.getBlockState(pos.offset(direction));

            state = state.with(FACING, direction);

            state = this.getStateWithNeighbor(state, previous, neighbor1);
            state = this.getStateWithNeighbor(state, direction, neighbor2);

            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlockState(pos, state, 6);
        }

    }

    private void openContainer(World world, BlockPos blockPos, PlayerEntity playerEntity) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity instanceof DuctEntity) {
            playerEntity.openHandledScreen((NamedScreenHandlerFactory) blockEntity);
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState blockState, LootContext.Builder lootContext$Builder) {
        ArrayList<ItemStack> dropList = new ArrayList<ItemStack>();
        dropList.add(new ItemStack(this));
        return dropList;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = this.getDefaultState().with(FACING, ctx.getSide().getOpposite());

        for (Direction direction : DIRECTIONS) {
            if (direction.equals(state.get(FACING))) { continue; }
            BlockState neighbor = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(direction));

            state = getStateWithNeighbor(state, direction, neighbor);
        }

        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction.equals(state.get(FACING))) { return state; }

        return getStateWithNeighbor(state, direction, neighbor);
    }

    private BlockState getStateWithNeighbor(BlockState state, Direction direction, BlockState neighbor) {
        if (direction.equals(state.get(FACING))) {
            return state.with(BooleanProperty.of(direction.toString()), false);
        }

        Block neighborBlock = neighbor.getBlock();

        // Connect to Ductwork blocks.
        if (neighbor.isIn(Ductwork.DUCT_BLOCKS) && neighbor.get(FACING).equals(direction.getOpposite())) {
            return state.with(BooleanProperty.of(direction.toString()), true);
        }

        // Connect to Vanilla Hoppers (and some Hopper mods).
        if (neighbor.contains(HopperBlock.FACING) && neighbor.get(HopperBlock.FACING).equals(direction.getOpposite())) {
            return state.with(BooleanProperty.of(direction.toString()), true);
        }

        // Connect to Vanilla Droppers.
        if (neighborBlock instanceof DropperBlock && neighbor.get(DropperBlock.FACING).equals(direction.getOpposite())) {
            return state.with(BooleanProperty.of(direction.toString()), true);
        }

        /*
         * The remaining connections are to blocks provided by mods to which we support connecting.
         * We use a rather circuitous method of testing these blocks because our mod must compile
         * and run without these other mods being present.
         */

        // Connect to Basalt Crusher Gravel Mills.
        if (Registry.BLOCK.getId(neighborBlock).equals(new Identifier("basalt-crusher", "gravel_mill")) &&
                neighbor.contains(HorizontalFacingBlock.FACING) && neighbor.get(HorizontalFacingBlock.FACING).equals(direction)) {
            return state.with(BooleanProperty.of(direction.toString()), true);
        }

        // Connect to Ducts mod Ducts.
        if (Registry.BLOCK.getId(neighborBlock).equals(new Identifier("ducts", "duct")) &&
                neighbor.contains(Properties.FACING) && neighbor.get(Properties.FACING).equals(direction.getOpposite())) {
            return state.with(BooleanProperty.of(direction.toString()), true);
        }

        // Connect to OmniHopper mod OmniHoppers.
        if (Registry.BLOCK.getId(neighborBlock).equals(new Identifier("omnihopper", "omnihopper"))) {
            EnumProperty<Direction> POINTY_BIT = DirectionProperty.of("pointy_bit", Direction.values());

            if (neighbor.contains(POINTY_BIT) && neighbor.get(POINTY_BIT).equals(direction.getOpposite())) {
                return state.with(BooleanProperty.of(direction.toString()), true);
            }
        }

        // Connect to Flytre's Pipe mod Pipes
        if (Registry.BLOCK.getId(neighborBlock).equals(new Identifier("pipe", "item_pipe")) ||
            Registry.BLOCK.getId(neighborBlock).equals(new Identifier("pipe", "fast_pipe"))) {

            // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
            // So I'm being lazy here and instead of duplicating a giant enum property, I just assume they will...
            return state.with(BooleanProperty.of(direction.toString()), true);
        }

        // Connect to Simple Pipes mod Pipes.
        if (Registry.BLOCK.getId(neighborBlock).equals(new Identifier("simple_pipes", "pipe_wooden_item")) ||
            Registry.BLOCK.getId(neighborBlock).equals(new Identifier("simple_pipes", "pipe_stone_item")) ||
            Registry.BLOCK.getId(neighborBlock).equals(new Identifier("simple_pipes", "pipe_clay_item")) ||
            Registry.BLOCK.getId(neighborBlock).equals(new Identifier("simple_pipes", "pipe_iron_item")) ||
            Registry.BLOCK.getId(neighborBlock).equals(new Identifier("simple_pipes", "pipe_gold_item")) ||
            Registry.BLOCK.getId(neighborBlock).equals(new Identifier("simple_pipes", "pipe_diamond_item"))) {

            // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
            // So I'm being lazy here and just assuming they will...
            return state.with(BooleanProperty.of(direction.toString()), true);
        }

        // Connect to Smart Pipes mod SmartPipes.
        if (Registry.BLOCK.getId(neighborBlock).equals(new Identifier("smart_pipes", "smart_pipe"))) {

            // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
            // So I'm being lazy here and just assuming they will...
            return state.with(BooleanProperty.of(direction.toString()), true);
        }

        return state.with(BooleanProperty.of(direction.toString()), false);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomName()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof DuctEntity) {
                ((DuctEntity)blockEntity).setCustomName(itemStack.getName());
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof DuctEntity) {
                ItemScatterer.spawn(world, pos, (DuctEntity)blockEntity);
                world.updateComparators(pos,this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING).add(NORTH).add(EAST).add(SOUTH).add(WEST).add(DOWN).add(UP);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        int shapeId;

        switch (state.get(FACING)) {
            case NORTH -> shapeId = 1;
            case EAST  -> shapeId = 2;
            case SOUTH -> shapeId = 4;
            case WEST  -> shapeId = 8;
            case DOWN  -> shapeId = 16;
            case UP    -> shapeId = 32;
            default    -> shapeId = 0;
        }

        if (state.get(NORTH)) { shapeId |= 1; }
        if (state.get(EAST))  { shapeId |= 2; }
        if (state.get(SOUTH)) { shapeId |= 4; }
        if (state.get(WEST))  { shapeId |= 8; }
        if (state.get(DOWN))  { shapeId |= 16; }
        if (state.get(UP))    { shapeId |= 32; }

        return DUCT_SHAPE_DICT[shapeId];
    }

    private static void buildShapeDict() {
        VoxelShape CENTER_SHAPE = Block.createCuboidShape(5.0D,  5.0D,  5.0D,  11.0D, 11.0D, 11.0D);
        VoxelShape NORTH_SHAPE  = Block.createCuboidShape(5.0D,  5.0D,  0.0D,  11.0D, 11.0D, 5.0D);
        VoxelShape EAST_SHAPE   = Block.createCuboidShape(11.0D, 5.0D,  5.0D,  16.0D, 11.0D, 11.0D);
        VoxelShape SOUTH_SHAPE  = Block.createCuboidShape(5.0D,  5.0D,  11.0D, 11.0D, 11.0D, 16.0D);
        VoxelShape WEST_SHAPE   = Block.createCuboidShape(0.0D,  5.0D,  5.0D,  5.0D,  11.0D, 11.0D);
        VoxelShape DOWN_SHAPE   = Block.createCuboidShape(5.0D,  0.0D,  5.0D,  11.0D, 5.0D,  11.0D);
        VoxelShape UP_SHAPE     = Block.createCuboidShape(5.0D,  11.0D, 5.0D,  11.0D, 16.0D, 11.0D);

        for (int i = 0; i < 64; ++i) {
            DUCT_SHAPE_DICT[i] = VoxelShapes.union(CENTER_SHAPE,
                    ((i & 1)  != 0) ? NORTH_SHAPE : VoxelShapes.empty(),
                    ((i & 2)  != 0) ? EAST_SHAPE  : VoxelShapes.empty(),
                    ((i & 4)  != 0) ? SOUTH_SHAPE : VoxelShapes.empty(),
                    ((i & 8)  != 0) ? WEST_SHAPE  : VoxelShapes.empty(),
                    ((i & 16) != 0) ? DOWN_SHAPE  : VoxelShapes.empty(),
                    ((i & 32) != 0) ? UP_SHAPE    : VoxelShapes.empty()
            );
        }
    }
}