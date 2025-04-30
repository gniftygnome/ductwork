package net.gnomecraft.ductwork.compat;

import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.Consumer;

public class DuctworkNeighborChecks extends NeighborChecks {
    public DuctworkNeighborChecks() {
        super("ductwork");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::ductworkings);
    }

    // Connect to Ductwork blocks.
    private boolean ductworkings(BlockState neighbor, Block neighborBlock, Direction facing) {
        return neighbor.isIn(Ductwork.DUCT_BLOCKS) && neighbor.get(DuctworkBlock.FACING).equals(facing);
    }
}
