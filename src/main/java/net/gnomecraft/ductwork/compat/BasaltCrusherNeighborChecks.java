package net.gnomecraft.ductwork.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
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
        return BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("gravel_mill")) &&
                neighbor.hasProperty(HorizontalDirectionalBlock.FACING) &&
                neighbor.getValue(HorizontalDirectionalBlock.FACING).equals(facing.getOpposite());
    }
}
