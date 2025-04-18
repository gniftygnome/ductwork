package net.gnomecraft.ductwork.collector;

import com.mojang.serialization.MapCodec;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;

public class CollectorBlock extends DuctworkBlock {
    public static final MapCodec<CollectorBlock> CODEC = CollectorBlock.createCodec(CollectorBlock::new);
    public static final VoxelShape[] COLLECTOR_SHAPE_DICT = new VoxelShape[512];

    public CollectorBlock(Settings settings) {
        super(settings);

        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.DOWN)
                .with(INTAKE, Direction.UP)
                .with(NORTH, false)
                .with(EAST,  false)
                .with(SOUTH, false)
                .with(WEST,  false)
                .with(DOWN,  false)
                .with(UP,    false)
                .with(ENABLED,true)
        );

        // Build the static global shape dictionary once when the first DuctBlock is instantiated.
        if (COLLECTOR_SHAPE_DICT[0] == null) {
            buildShapeDict();
        }
    }

    @Override
    protected MapCodec<CollectorBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CollectorEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, Ductwork.COLLECTOR_ENTITY, CollectorEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            ItemStack mainStack = player.getMainHandStack();

            if (player.isSneaking()) {
                if (mainStack.isOf(Items.STICK)) {
                    // Sneak + Stick = rotate FACING (pseudowrench)
                    // flags == 0x4 means notify listeners in server only
                    //          0x2 means do update listeners (in general)
                    //          0x1 means do update comparators
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, INTAKE));
                } else if (mainStack.isEmpty()) {
                    // Sneak + Empty primary = rotate INTAKE
                    this.reorientIntake(state, world, pos, this.getNextOrientation(state, INTAKE, FACING));
                } else {
                    return ActionResult.PASS;
                }
            } else {
                if (mainStack.isIn(Ductwork.WRENCHES)) {
                    // Wrench in primary = rotate FACING
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, INTAKE));
                } else if (Ductwork.getConfig().placement && mainStack.isIn(Ductwork.DUCT_ITEMS)) {
                    // Allow Duct-on-Duct placement if enabled.
                    return ActionResult.PASS;
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

    // Reorient the primary (FACING) orientation of the block with all necessary updates and notifications.
    protected void reorientIntake(BlockState state, World world, BlockPos pos, Direction direction) {
        Direction previous = state.get(INTAKE);

        if (!direction.equals(previous)) {
            BlockState neighbor1 = world.getBlockState(pos.offset(previous));
            BlockState neighbor2 = world.getBlockState(pos.offset(direction));

            state = state.with(INTAKE, direction);

            state = this.getStateWithNeighbor(state, previous, neighbor1);
            state = this.getStateWithNeighbor(state, direction, neighbor2);

            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlockState(pos, state, 7);
        }

    }

    private void openContainer(World world, BlockPos blockPos, PlayerEntity playerEntity) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity instanceof CollectorEntity) {
            playerEntity.openHandledScreen((NamedScreenHandlerFactory) blockEntity);
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction intake = ctx.getSide();
        Direction facing = intake.getOpposite();

        if (Ductwork.getConfig().vanilla) {
            intake = Direction.UP;
            if (facing == Direction.UP) {
                facing = Direction.DOWN;
            }
        }

        BlockState state = super.addPlacementState(this.stateManager.getDefaultState(), ctx)
                .with(FACING, facing)
                .with(INTAKE, intake);

        state = resetInputConnections(state, ctx.getWorld(), ctx.getBlockPos());

        return state;
    }

    @Override
    protected BlockState resetInputConnections(BlockState state, World world, BlockPos pos) {
        // To allow CollectorEntity to call this method during fixup.
        return super.resetInputConnections(state, world, pos);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (world != null && !oldState.isOf(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify) {
        if (world != null) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        BlockState newState;

        newState = state.with(ENABLED, !world.isReceivingRedstonePower(pos));
        newState = super.getStateForNeighborUpdate(newState, world, tickView, pos, direction, neighborPos, neighborState, random);
        newState = getStateWithNeighbor(newState, direction, neighborState);

        return newState;
    }

    private void updateEnabled(World world, BlockPos pos, BlockState state) {
        boolean enabled = !world.isReceivingRedstonePower(pos);
        if (enabled != state.get(ENABLED)) {
            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlockState(pos, state.with(ENABLED, enabled), 4);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, INTAKE, NORTH, EAST, SOUTH, WEST, DOWN, UP, ENABLED);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING))).with(INTAKE, rotation.rotate(state.get(INTAKE)));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // INTAKE as int ID in bits 6, 7, 8
        int shapeId = state.get(INTAKE).getIndex() << 6;

        // FACING bool packed in bits 0 - 5
        switch (state.get(FACING)) {
            case NORTH -> shapeId |= 1;
            case EAST  -> shapeId |= 2;
            case SOUTH -> shapeId |= 4;
            case WEST  -> shapeId |= 8;
            case DOWN  -> shapeId |= 16;
            case UP    -> shapeId |= 32;
        }

        // Adjacent connections also packed in bits 0 - 5
        // These are shaped the same as the output piece...
        if (state.get(NORTH)) { shapeId |= 1; }
        if (state.get(EAST))  { shapeId |= 2; }
        if (state.get(SOUTH)) { shapeId |= 4; }
        if (state.get(WEST))  { shapeId |= 8; }
        if (state.get(DOWN))  { shapeId |= 16; }
        if (state.get(UP))    { shapeId |= 32; }

        return COLLECTOR_SHAPE_DICT[shapeId];
    }

    private static void buildShapeDict() {
        VoxelShape[] INTAKE_SHAPES = new VoxelShape[6];
        VoxelShape[] ADJACENT_SHAPES = new VoxelShape[6];

        VoxelShape CENTER_SHAPE = Block.createCuboidShape(5.0D,  5.0D,  5.0D,  11.0D, 11.0D, 11.0D);

        INTAKE_SHAPES[Direction.NORTH.getIndex()] = VoxelShapes.union(CENTER_SHAPE,
                Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D,  4.0D),
                Block.createCuboidShape(4.0D, 4.0D, 4.0D, 12.0D, 12.0D,  5.0D)
        );
        INTAKE_SHAPES[Direction.EAST.getIndex()] = VoxelShapes.union(CENTER_SHAPE,
                Block.createCuboidShape(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                Block.createCuboidShape(11.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D)
        );
        INTAKE_SHAPES[Direction.SOUTH.getIndex()] = VoxelShapes.union(CENTER_SHAPE,
                Block.createCuboidShape(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D),
                Block.createCuboidShape(4.0D, 4.0D, 11.0D, 12.0D, 12.0D, 12.0D)
        );
        INTAKE_SHAPES[Direction.WEST.getIndex()] = VoxelShapes.union(CENTER_SHAPE,
                Block.createCuboidShape(0.0D, 0.0D, 0.0D,  4.0D, 16.0D, 16.0D),
                Block.createCuboidShape(4.0D, 4.0D, 4.0D,  5.0D, 12.0D, 12.0D)
        );
        INTAKE_SHAPES[Direction.DOWN.getIndex()] = VoxelShapes.union(CENTER_SHAPE,
                Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D,  4.0D, 16.0D),
                Block.createCuboidShape(4.0D, 4.0D, 4.0D, 12.0D,  5.0D, 12.0D)
        );
        INTAKE_SHAPES[Direction.UP.getIndex()] = VoxelShapes.union(CENTER_SHAPE,
                Block.createCuboidShape(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                Block.createCuboidShape(4.0D, 11.0D, 4.0D, 12.0D, 12.0D, 12.0D)
        );

        ADJACENT_SHAPES[Direction.NORTH.getIndex()] =
                Block.createCuboidShape(5.0D,  5.0D,  0.0D,  11.0D, 11.0D, 5.0D);
        ADJACENT_SHAPES[Direction.EAST.getIndex()] =
                Block.createCuboidShape(11.0D, 5.0D,  5.0D,  16.0D, 11.0D, 11.0D);
        ADJACENT_SHAPES[Direction.SOUTH.getIndex()] =
                Block.createCuboidShape(5.0D,  5.0D,  11.0D, 11.0D, 11.0D, 16.0D);
        ADJACENT_SHAPES[Direction.WEST.getIndex()] =
                Block.createCuboidShape(0.0D,  5.0D,  5.0D,  5.0D,  11.0D, 11.0D);
        ADJACENT_SHAPES[Direction.DOWN.getIndex()] =
                Block.createCuboidShape(5.0D,  0.0D,  5.0D,  11.0D, 5.0D,  11.0D);
        ADJACENT_SHAPES[Direction.UP.getIndex()] =
                Block.createCuboidShape(5.0D,  11.0D, 5.0D,  11.0D, 16.0D, 11.0D);

        for (Direction intake: DIRECTIONS) {
            int intakeId = intake.getIndex();
            for (int adjacents = 0; adjacents < 64; ++adjacents) {
                COLLECTOR_SHAPE_DICT[(intakeId << 6) | adjacents] = VoxelShapes.union(
                        INTAKE_SHAPES[intakeId],
                        ((adjacents & 1)  != 0) ? ADJACENT_SHAPES[Direction.NORTH.getIndex()] : VoxelShapes.empty(),
                        ((adjacents & 2)  != 0) ? ADJACENT_SHAPES[Direction.EAST.getIndex()]  : VoxelShapes.empty(),
                        ((adjacents & 4)  != 0) ? ADJACENT_SHAPES[Direction.SOUTH.getIndex()] : VoxelShapes.empty(),
                        ((adjacents & 8)  != 0) ? ADJACENT_SHAPES[Direction.WEST.getIndex()]  : VoxelShapes.empty(),
                        ((adjacents & 16) != 0) ? ADJACENT_SHAPES[Direction.DOWN.getIndex()]  : VoxelShapes.empty(),
                        ((adjacents & 32) != 0) ? ADJACENT_SHAPES[Direction.UP.getIndex()]    : VoxelShapes.empty()
                );
            }
        }
    }
}