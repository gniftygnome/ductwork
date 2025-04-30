package net.gnomecraft.ductwork.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.Consumer;

public class DuctsModNeighborChecks extends NeighborChecks {
    public DuctsModNeighborChecks() {
        super("ducts");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::duct);
    }

    // Connect to Ducts mod Ducts.
    private boolean duct(BlockState neighbor, Block neighborBlock, Direction facing) {
        return Registries.BLOCK.getId(neighborBlock).equals(id("duct")) &&
                neighbor.contains(Properties.FACING) && neighbor.get(Properties.FACING).equals(facing);
    }
}
