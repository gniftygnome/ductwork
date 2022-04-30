package net.gnomecraft.ductwork.base;

import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class DuctworkBlock extends BlockWithEntity {
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