package net.gnomecraft.ductwork.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
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
        return BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("duct")) &&
                neighbor.hasProperty(BlockStateProperties.FACING) && neighbor.getValue(BlockStateProperties.FACING).equals(facing);
    }
}
