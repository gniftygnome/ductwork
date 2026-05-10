package net.gnomecraft.ductwork.compat;

import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
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
        return neighbor.is(Ductwork.DUCT_BLOCKS) && neighbor.getValue(DuctworkBlock.FACING).equals(facing);
    }
}
