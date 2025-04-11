package net.gnomecraft.ductwork.base;

import com.mojang.serialization.MapCodec;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;

public abstract class DuctworkBlock extends BlockWithEntity implements Waterloggable {
    public static final EnumProperty<Direction> FACING = FacingBlock.FACING;
    public static final EnumProperty<Direction> INTAKE = EnumProperty.of("intake", Direction.class);
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty EAST  = Properties.EAST;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST  = Properties.WEST;
    public static final BooleanProperty DOWN  = Properties.DOWN;
    public static final BooleanProperty UP    = Properties.UP;
    public static final BooleanProperty ENABLED = Properties.ENABLED;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public static final Map<Direction, BooleanProperty> DIR_MAP = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.DOWN, DOWN,
            Direction.UP, UP
    );

    protected DuctworkBlock(Settings settings) {
        super(settings);

        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected abstract MapCodec<? extends DuctworkBlock> getCodec();

    /**
     * This method provides the rotation calculations for all Ductwork blocks when they are to be rotated.
     * The order of the orientations is stable (unless Mojang changes the Directions enum).
     *
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
        Direction orient = state.get(orientable);
        // Coorient defaults to UP; set the axis of rotation based on the coorient.
        Direction coorient = coorientable != null ? state.get(coorientable) : Direction.UP;
        Direction.Axis axis = coorient.getAxis();

        // Build a stable, sorted list of orientations for the use case we're iterating.
        ArrayList<Direction> orientations = new ArrayList<>();
        Direction iterator = Direction.byIndex(0);
        // Get the lowest-numbered off-axis direction.
        if (axis.test(iterator)) {
            iterator = Direction.byIndex(iterator.getIndex() + 1);
            if (axis.test(iterator)) {
                iterator = Direction.byIndex(iterator.getIndex() + 1);
            }
        }
        // Rotate clockwise around the axis.
        for (int i = 0; i < 4; i++) {
            orientations.add(iterator);
            iterator = iterator.rotateClockwise(axis);
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
    protected void reorient(BlockState state, World world, BlockPos pos, Direction direction) {
        Direction previous = state.get(FacingBlock.FACING);

        if (!direction.equals(previous)) {
            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlockState(pos, state.with(FacingBlock.FACING, direction), 6);
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
    protected BlockState resetInputConnections(BlockState state, World world, BlockPos pos) {
        for (Direction direction : DIRECTIONS) {
            BlockState neighbor = world.getBlockState(pos.offset(direction));

            state = getStateWithNeighbor(state, direction, neighbor);
        }

        return state;
    }

    /**
     * This method is called when the relationship between the calling block and a specific neighbor may have
     * changed.  This can be either because the neighbor announced a change or because the calling block's own
     * state has changed in a manner which may impact its relationship with the specified neighbor.
     *
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
        if ((state.contains(FACING) && direction.equals(state.get(FACING))) ||
            (state.contains(INTAKE) && direction.equals(state.get(INTAKE)))) {
            return state.with(DIR_MAP.get(direction), false);
        }

        Block neighborBlock = neighbor.getBlock();

        // Connect to Ductwork blocks.
        if (neighbor.isIn(Ductwork.DUCT_BLOCKS) && neighbor.get(FACING).equals(direction.getOpposite())) {
            return state.with(DIR_MAP.get(direction), true);
        }

        // Connect to Vanilla Hoppers (and some Hopper mods).
        if (neighbor.contains(HopperBlock.FACING) && neighbor.get(HopperBlock.FACING).equals(direction.getOpposite())) {
            return state.with(DIR_MAP.get(direction), true);
        }

        // Connect to Vanilla Droppers.
        if (neighborBlock instanceof DropperBlock && neighbor.get(DropperBlock.FACING).equals(direction.getOpposite())) {
            return state.with(DIR_MAP.get(direction), true);
        }

        /*
         * The remaining connections are to blocks provided by mods to which we support connecting.
         * We use a rather circuitous method of testing these blocks because our mod must compile
         * and run without these other mods being present.
         */

        // Connect to Basalt Crusher Gravel Mills.
        if (Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("basalt-crusher", "gravel_mill")) &&
                neighbor.contains(HorizontalFacingBlock.FACING) && neighbor.get(HorizontalFacingBlock.FACING).equals(direction)) {
            return state.with(DIR_MAP.get(direction), true);
        }

        // Connect to Ducts mod Ducts.
        if (Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("ducts", "duct")) &&
                neighbor.contains(Properties.FACING) && neighbor.get(Properties.FACING).equals(direction.getOpposite())) {
            return state.with(DIR_MAP.get(direction), true);
        }

        // Connect to OmniHopper mod OmniHoppers.
        if (Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("omnihopper", "omnihopper"))) {
            EnumProperty<Direction> POINTY_BIT = EnumProperty.of("pointy_bit", Direction.class);

            if (neighbor.contains(POINTY_BIT) && neighbor.get(POINTY_BIT).equals(direction.getOpposite())) {
                return state.with(DIR_MAP.get(direction), true);
            }
        }

        // Connect to Flytre's Pipe mod Pipes
        if (Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("pipe", "item_pipe")) ||
                Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("pipe", "fast_pipe"))) {

            // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
            // So I'm being lazy here and instead of duplicating a giant enum property, I just assume they will...
            return state.with(DIR_MAP.get(direction), true);
        }

        // Connect to Simple Pipes mod Pipes.
        if (Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("simple_pipes", "pipe_wooden_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("simple_pipes", "pipe_stone_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("simple_pipes", "pipe_clay_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("simple_pipes", "pipe_iron_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("simple_pipes", "pipe_gold_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("simple_pipes", "pipe_diamond_item"))) {

            // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
            // So I'm being lazy here and just assuming they will...
            return state.with(DIR_MAP.get(direction), true);
        }

        // Connect to Smart Pipes mod SmartPipes.
        if (Registries.BLOCK.getId(neighborBlock).equals(Identifier.of("smart_pipes", "smart_pipe"))) {

            // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
            // So I'm being lazy here and just assuming they will...
            return state.with(DIR_MAP.get(direction), true);
        }

        return state.with(DIR_MAP.get(direction), false);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    public BlockState addPlacementState(BlockState state, ItemPlacementContext ctx) {
        return state.with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    @Override
    public boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FacingBlock.FACING, rotation.rotate(state.get(FacingBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FacingBlock.FACING)));
    }
}