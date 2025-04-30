package net.gnomecraft.ductwork.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.Consumer;

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
        return neighbor.contains(HopperBlock.FACING) && neighbor.get(HopperBlock.FACING).equals(facing);
    }

    // Connect to Vanilla Droppers.
    private boolean droppers(BlockState neighbor, Block neighborBlock, Direction facing) {
        return neighborBlock instanceof DropperBlock && neighbor.get(DropperBlock.FACING).equals(facing);
    }
}
