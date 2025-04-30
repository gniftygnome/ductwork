package net.gnomecraft.ductwork.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.Consumer;

public class SmartPipesNeighborChecks extends NeighborChecks {
    public SmartPipesNeighborChecks() {
        super("smart_pipes");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::pipes);
    }

    // Connect to Smart Pipes mod SmartPipes.
    private boolean pipes(BlockState neighbor, Block neighborBlock, Direction facing) {
        // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
        // So I'm being lazy here and just assuming they will...
        return Registries.BLOCK.getId(neighborBlock).equals(id("smart_pipe"));
    }
}
