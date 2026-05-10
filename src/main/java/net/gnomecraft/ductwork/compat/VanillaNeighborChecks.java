package net.gnomecraft.ductwork.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
public class VanillaNeighborChecks extends NeighborChecks {
    public VanillaNeighborChecks() {
        super("minecraft");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::hoppers);
        registry.accept(this::droppers);
    }

    // Connect to Vanilla Hoppers (and some Hopper mods).
    private boolean hoppers(BlockState neighbor, Block neighborBlock, Direction facing) {
        return neighbor.hasProperty(HopperBlock.FACING) && neighbor.getValue(HopperBlock.FACING).equals(facing);
    }

    // Connect to Vanilla Droppers.
    private boolean droppers(BlockState neighbor, Block neighborBlock, Direction facing) {
        return neighborBlock instanceof DropperBlock && neighbor.getValue(DropperBlock.FACING).equals(facing);
    }
}
