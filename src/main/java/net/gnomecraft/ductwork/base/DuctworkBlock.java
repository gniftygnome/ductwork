package net.gnomecraft.ductwork.base;

import com.mojang.serialization.MapCodec;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.compat.NeighborChecks;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.Containers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;

@NullMarked
public abstract class DuctworkBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final EnumProperty<Direction> INTAKE = EnumProperty.create("intake", Direction.class);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, BooleanProperty> DIR_MAP = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.DOWN, DOWN,
            Direction.UP, UP
    );

    protected DuctworkBlock(Properties settings) {
        super(settings);

        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected abstract MapCodec<? extends DuctworkBlock> codec();

    /**
     * This method provides the rotation calculations for all Ductwork blocks when they are to be rotated.
     * The order of the orientations is stable (unless Mojang changes the Directions enum).
     * <p/>
     * If the coorientable is specified, it defines the axis for primary rotation, and the direction specified by
     * the coorientable property will be skipped when selecting a new direction for the orientable property.
     * Otherwise (if coorientable is null), the Up/Down axis will be selected but all directions will be eligible
     * for selection as the new value of the orientable property.
     *
     * @param state The block state of the block to rotate
     * @param orientable The direction property specifying the orientation to be changed
     * @param coorientable The (optional) direction property to rotate around (and avoid selecting)
     * @return The new direction the block should be oriented to
     */
    protected Direction getNextOrientation(BlockState state, EnumProperty<Direction> orientable, @Nullable EnumProperty<Direction> coorientable) {
        Direction orient = state.getValue(orientable);
        // Coorient defaults to UP; set the axis of rotation based on the coorient.
        Direction coorient = coorientable != null ? state.getValue(coorientable) : Direction.UP;
        Direction.Axis axis = coorient.getAxis();

        // Build a stable, sorted list of orientations for the use case we're iterating.
        ArrayList<Direction> orientations = new ArrayList<>();
        Direction iterator = Direction.from3DDataValue(0);
        // Get the lowest-numbered off-axis direction.
        if (axis.test(iterator)) {
            iterator = Direction.from3DDataValue(iterator.get3DDataValue() + 1);
            if (axis.test(iterator)) {
                iterator = Direction.from3DDataValue(iterator.get3DDataValue() + 1);
            }
        }
        // Rotate clockwise around the axis.
        for (int i = 0; i < 4; i++) {
            orientations.add(iterator);
            iterator = iterator.getClockWise(axis);
        }
        // Add the coorient if it was unspecified.
        if (coorientable == null) {
            orientations.add(coorient);
        }
        // Add the coorient's opposite pole.
        orientations.add(coorient.getOpposite());

        // Special considerations for Vanilla mode:
        // FACING must not be UP
        // INTAKE must be UP
        if (Ductwork.getConfig().vanilla) {
            if (orientable.getName().equalsIgnoreCase("facing")) {
                orientations.remove(Direction.UP);
            } else if (orientable.getName().equalsIgnoreCase("intake")) {
                return Direction.UP;
            }
        }

        // Return the next valid orientation.
        return orientations.get((orientations.indexOf(orient) + 1) % orientations.size());
    }

    /**
     * Reorient the primary (FACING) orientation of the block with all necessary updates and notifications.
     * Override this if f.e. the block also needs to pay attention to its own orientation...
     *
     * @param state The block state of the block being reoriented
     * @param world The world in which the block resides
     * @param pos The block position of the block
     * @param direction The new primary orientation for the block
     */
    protected void reorient(BlockState state, Level world, BlockPos pos, Direction direction) {
        Direction previous = state.getValue(DirectionalBlock.FACING);

        if (!direction.equals(previous)) {
            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlock(pos, state.setValue(DirectionalBlock.FACING, direction), 6);
        }

    }

    /**
     * This method re-checks all the input connections of the provided block state and pos.  Typically this is
     * only needed when the block is first placed, but it can be useful if we doubt the state for some reason.
     *
     * @param state BlockState to re-scan for input connections
     * @param world World in which we are working
     * @param pos BlockPos of the BlockState in question
     * @return New BlockState with re-evaluated life choices
     */
    protected BlockState resetInputConnections(BlockState state, Level world, BlockPos pos) {
        for (Direction direction : UPDATE_SHAPE_ORDER) {
            BlockState neighbor = world.getBlockState(pos.relative(direction));

            state = getStateWithNeighbor(state, direction, neighbor);
        }

        return state;
    }

    /**
     * This method is called when the relationship between the calling block and a specific neighbor may have
     * changed.  This can be either because the neighbor announced a change or because the calling block's own
     * state has changed in a manner which may impact its relationship with the specified neighbor.
     * <p/>
     * Input connections to the neighboring block will be suppressed if they interfere with the output or
     * intake orientations of the calling block.  Broken input connections will also be disabled.  Newly
     * available input connections will be enabled.
     *
     * @param state BlockState to be modified if relationship to neighbor has changed
     * @param direction Direction of the neighbor in question relative to this block
     * @param neighbor BlockState of the neighbor in question
     * @return New BlockState based on updated neighbor relationship
     */
    protected BlockState getStateWithNeighbor(BlockState state, Direction direction, BlockState neighbor) {
        if ((state.hasProperty(FACING) && direction.equals(state.getValue(FACING))) ||
            (state.hasProperty(INTAKE) && direction.equals(state.getValue(INTAKE)))) {
            return state.setValue(DIR_MAP.get(direction), false);
        }

        // Check neighbors for connections.
        return state.setValue(DIR_MAP.get(direction),
                NeighborChecks.checkNeighbor(neighbor, direction.getOpposite()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    public BlockState addPlacementState(BlockState state, BlockPlaceContext ctx) {
        return state.setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        Containers.updateNeighboursAfterDestroy(state, world, pos);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
    }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(DirectionalBlock.FACING, rotation.rotate(state.getValue(DirectionalBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(DirectionalBlock.FACING)));
    }
}