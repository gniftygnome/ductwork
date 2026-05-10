package net.gnomecraft.ductwork.collector;

import com.mojang.serialization.MapCodec;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public class CollectorBlock extends DuctworkBlock {
    public static final MapCodec<CollectorBlock> CODEC = CollectorBlock.simpleCodec(CollectorBlock::new);
    public static final @Nullable VoxelShape[] COLLECTOR_SHAPE_DICT = new VoxelShape[512];

    public CollectorBlock(Properties settings) {
        super(settings);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(INTAKE, Direction.UP)
                .setValue(NORTH, false)
                .setValue(EAST,  false)
                .setValue(SOUTH, false)
                .setValue(WEST,  false)
                .setValue(DOWN,  false)
                .setValue(UP,    false)
                .setValue(ENABLED,true)
        );

        // Build the static global shape dictionary once when the first DuctBlock is instantiated.
        if (COLLECTOR_SHAPE_DICT[0] == null) {
            buildShapeDict();
        }
    }

    @Override
    protected MapCodec<CollectorBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CollectorEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Ductwork.COLLECTOR_ENTITY, CollectorEntity::tick);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            ItemStack mainStack = player.getMainHandItem();

            if (player.isShiftKeyDown()) {
                if (mainStack.is(Items.STICK)) {
                    // Sneak + Stick = rotate FACING (pseudowrench)
                    // flags == 0x4 means notify listeners in server only
                    //          0x2 means do update listeners (in general)
                    //          0x1 means do update comparators
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, INTAKE));
                } else if (mainStack.isEmpty()) {
                    // Sneak + Empty primary = rotate INTAKE
                    this.reorientIntake(state, world, pos, this.getNextOrientation(state, INTAKE, FACING));
                } else {
                    return InteractionResult.PASS;
                }
            } else {
                if (mainStack.is(Ductwork.WRENCHES)) {
                    // Wrench in primary = rotate FACING
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, INTAKE));
                } else if (Ductwork.getConfig().placement && mainStack.is(Ductwork.DUCT_ITEMS)) {
                    // Allow Duct-on-Duct placement if enabled.
                    return InteractionResult.PASS;
                } else {
                    // Otherwise = open container
                    this.openContainer(world, pos, player);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    // Reorient the primary (FACING) orientation of the block with all necessary updates and notifications.
    @Override
    protected void reorient(BlockState state, Level world, BlockPos pos, Direction direction) {
        Direction previous = state.getValue(FACING);

        if (!direction.equals(previous)) {
            BlockState neighbor1 = world.getBlockState(pos.relative(previous));
            BlockState neighbor2 = world.getBlockState(pos.relative(direction));

            state = state.setValue(FACING, direction);

            state = this.getStateWithNeighbor(state, previous, neighbor1);
            state = this.getStateWithNeighbor(state, direction, neighbor2);

            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlock(pos, state, 6);
        }

    }

    // Reorient the primary (FACING) orientation of the block with all necessary updates and notifications.
    protected void reorientIntake(BlockState state, Level world, BlockPos pos, Direction direction) {
        Direction previous = state.getValue(INTAKE);

        if (!direction.equals(previous)) {
            BlockState neighbor1 = world.getBlockState(pos.relative(previous));
            BlockState neighbor2 = world.getBlockState(pos.relative(direction));

            state = state.setValue(INTAKE, direction);

            state = this.getStateWithNeighbor(state, previous, neighbor1);
            state = this.getStateWithNeighbor(state, direction, neighbor2);

            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlock(pos, state, 7);
        }

    }

    private void openContainer(Level world, BlockPos blockPos, Player playerEntity) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity instanceof CollectorEntity) {
            playerEntity.openMenu((MenuProvider) blockEntity);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction intake = ctx.getClickedFace();
        Direction facing = intake.getOpposite();

        if (Ductwork.getConfig().vanilla) {
            intake = Direction.UP;
            if (facing == Direction.UP) {
                facing = Direction.DOWN;
            }
        }

        BlockState state = super.addPlacementState(this.stateDefinition.any(), ctx)
                .setValue(FACING, facing)
                .setValue(INTAKE, intake);

        state = resetInputConnections(state, ctx.getLevel(), ctx.getClickedPos());

        return state;
    }

    @Override
    protected BlockState resetInputConnections(BlockState state, Level world, BlockPos pos) {
        // To allow CollectorEntity to call this method during fixup.
        return super.resetInputConnections(state, world, pos);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        this.updateEnabled(world, pos, state);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState newState;

        newState = state.setValue(ENABLED, !world.hasNeighborSignal(pos));
        newState = super.updateShape(newState, world, tickView, pos, direction, neighborPos, neighborState, random);
        newState = getStateWithNeighbor(newState, direction, neighborState);

        return newState;
    }

    private void updateEnabled(Level world, BlockPos pos, BlockState state) {
        boolean enabled = !world.hasNeighborSignal(pos);
        if (enabled != state.getValue(ENABLED)) {
            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlock(pos, state.setValue(ENABLED, enabled), 4);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, INTAKE, NORTH, EAST, SOUTH, WEST, DOWN, UP, ENABLED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING))).setValue(INTAKE, rotation.rotate(state.getValue(INTAKE)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // INTAKE as int ID in bits 6, 7, 8
        int shapeId = state.getValue(INTAKE).get3DDataValue() << 6;

        // FACING bool packed in bits 0 - 5
        switch (state.getValue(FACING)) {
            case NORTH -> shapeId |= 1;
            case EAST  -> shapeId |= 2;
            case SOUTH -> shapeId |= 4;
            case WEST  -> shapeId |= 8;
            case DOWN  -> shapeId |= 16;
            case UP    -> shapeId |= 32;
        }

        // Adjacent connections also packed in bits 0 - 5
        // These are shaped the same as the output piece...
        if (state.getValue(NORTH)) { shapeId |= 1; }
        if (state.getValue(EAST))  { shapeId |= 2; }
        if (state.getValue(SOUTH)) { shapeId |= 4; }
        if (state.getValue(WEST))  { shapeId |= 8; }
        if (state.getValue(DOWN))  { shapeId |= 16; }
        if (state.getValue(UP))    { shapeId |= 32; }

        return Objects.requireNonNull(COLLECTOR_SHAPE_DICT[shapeId]);
    }

    private static void buildShapeDict() {
        VoxelShape[] INTAKE_SHAPES = new VoxelShape[6];
        VoxelShape[] ADJACENT_SHAPES = new VoxelShape[6];

        VoxelShape CENTER_SHAPE = Block.box(5.0D,  5.0D,  5.0D,  11.0D, 11.0D, 11.0D);

        INTAKE_SHAPES[Direction.NORTH.get3DDataValue()] = Shapes.or(CENTER_SHAPE,
                Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D,  4.0D),
                Block.box(4.0D, 4.0D, 4.0D, 12.0D, 12.0D,  5.0D)
        );
        INTAKE_SHAPES[Direction.EAST.get3DDataValue()] = Shapes.or(CENTER_SHAPE,
                Block.box(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                Block.box(11.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D)
        );
        INTAKE_SHAPES[Direction.SOUTH.get3DDataValue()] = Shapes.or(CENTER_SHAPE,
                Block.box(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D),
                Block.box(4.0D, 4.0D, 11.0D, 12.0D, 12.0D, 12.0D)
        );
        INTAKE_SHAPES[Direction.WEST.get3DDataValue()] = Shapes.or(CENTER_SHAPE,
                Block.box(0.0D, 0.0D, 0.0D,  4.0D, 16.0D, 16.0D),
                Block.box(4.0D, 4.0D, 4.0D,  5.0D, 12.0D, 12.0D)
        );
        INTAKE_SHAPES[Direction.DOWN.get3DDataValue()] = Shapes.or(CENTER_SHAPE,
                Block.box(0.0D, 0.0D, 0.0D, 16.0D,  4.0D, 16.0D),
                Block.box(4.0D, 4.0D, 4.0D, 12.0D,  5.0D, 12.0D)
        );
        INTAKE_SHAPES[Direction.UP.get3DDataValue()] = Shapes.or(CENTER_SHAPE,
                Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                Block.box(4.0D, 11.0D, 4.0D, 12.0D, 12.0D, 12.0D)
        );

        ADJACENT_SHAPES[Direction.NORTH.get3DDataValue()] =
                Block.box(5.0D,  5.0D,  0.0D,  11.0D, 11.0D, 5.0D);
        ADJACENT_SHAPES[Direction.EAST.get3DDataValue()] =
                Block.box(11.0D, 5.0D,  5.0D,  16.0D, 11.0D, 11.0D);
        ADJACENT_SHAPES[Direction.SOUTH.get3DDataValue()] =
                Block.box(5.0D,  5.0D,  11.0D, 11.0D, 11.0D, 16.0D);
        ADJACENT_SHAPES[Direction.WEST.get3DDataValue()] =
                Block.box(0.0D,  5.0D,  5.0D,  5.0D,  11.0D, 11.0D);
        ADJACENT_SHAPES[Direction.DOWN.get3DDataValue()] =
                Block.box(5.0D,  0.0D,  5.0D,  11.0D, 5.0D,  11.0D);
        ADJACENT_SHAPES[Direction.UP.get3DDataValue()] =
                Block.box(5.0D,  11.0D, 5.0D,  11.0D, 16.0D, 11.0D);

        for (Direction intake: UPDATE_SHAPE_ORDER) {
            int intakeId = intake.get3DDataValue();
            for (int adjacents = 0; adjacents < 64; ++adjacents) {
                COLLECTOR_SHAPE_DICT[(intakeId << 6) | adjacents] = Shapes.or(
                        INTAKE_SHAPES[intakeId],
                        ((adjacents & 1)  != 0) ? ADJACENT_SHAPES[Direction.NORTH.get3DDataValue()] : Shapes.empty(),
                        ((adjacents & 2)  != 0) ? ADJACENT_SHAPES[Direction.EAST.get3DDataValue()]  : Shapes.empty(),
                        ((adjacents & 4)  != 0) ? ADJACENT_SHAPES[Direction.SOUTH.get3DDataValue()] : Shapes.empty(),
                        ((adjacents & 8)  != 0) ? ADJACENT_SHAPES[Direction.WEST.get3DDataValue()]  : Shapes.empty(),
                        ((adjacents & 16) != 0) ? ADJACENT_SHAPES[Direction.DOWN.get3DDataValue()]  : Shapes.empty(),
                        ((adjacents & 32) != 0) ? ADJACENT_SHAPES[Direction.UP.get3DDataValue()]    : Shapes.empty()
                );
            }
        }
    }
}