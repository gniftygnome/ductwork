package net.gnomecraft.ductwork.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.Consumer;

public class BasaltCrusherNeighborChecks extends NeighborChecks {
    public BasaltCrusherNeighborChecks() {
        super("basalt-crusher");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::gravelMill);
    }

    // Connect to Basalt Crusher Gravel Mills.
    private boolean gravelMill(BlockState neighbor, Block neighborBlock, Direction facing) {
        return Registries.BLOCK.getId(neighborBlock).equals(id("gravel_mill")) &&
                neighbor.contains(HorizontalFacingBlock.FACING) &&
                neighbor.get(HorizontalFacingBlock.FACING).equals(facing.getOpposite());
    }
}
