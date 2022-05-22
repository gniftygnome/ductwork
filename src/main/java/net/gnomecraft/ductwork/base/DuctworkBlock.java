package net.gnomecraft.ductwork.base;

import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class DuctworkBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = FacingBlock.FACING;
    public static final DirectionProperty INTAKE = DirectionProperty.of("intake");
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty EAST  = BooleanProperty.of("east");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty WEST  = BooleanProperty.of("west");
    public static final BooleanProperty DOWN  = BooleanProperty.of("down");
    public static final BooleanProperty UP    = BooleanProperty.of("up");
    public static final BooleanProperty ENABLED = BooleanProperty.of("enabled");

    protected DuctworkBlock(Settings settings) {
        super(settings);
    }

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
    protected Direction getNextOrientation(BlockState state, DirectionProperty orientable, @Nullable DirectionProperty coorientable) {
        Direction orient = state.get(orientable);
        // Coorient defaults to UP; set the axis of rotation based on the coorient.
        Direction coorient = coorientable != null ? state.get(coorientable) : Direction.UP;
        Direction.Axis axis = coorient.getAxis();

        // Build a stable, sorted list of orientations for the use case we're iterating.
        ArrayList<Direction> orientations = new ArrayList<>();
        Direction iterator = Direction.byId(0);
        // Get the lowest-numbered off-axis direction.
        if (axis.test(iterator)) {
            iterator = Direction.byId(iterator.getId() + 1);
            if (axis.test(iterator)) {
                iterator = Direction.byId(iterator.getId() + 1);
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
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
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