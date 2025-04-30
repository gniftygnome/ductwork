package net.gnomecraft.ductwork.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.Consumer;

public class SimplePipesNeighborChecks extends NeighborChecks {
    public SimplePipesNeighborChecks() {
        super("simple_pipes");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::pipes);
    }

    // Connect to Simple Pipes mod Pipes.
    private boolean pipes(BlockState neighbor, Block neighborBlock, Direction facing) {
        // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
        // So I'm being lazy here and just assuming they will...
        return Registries.BLOCK.getId(neighborBlock).equals(id("pipe_wooden_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(id("pipe_stone_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(id("pipe_clay_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(id("pipe_iron_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(id("pipe_gold_item")) ||
                Registries.BLOCK.getId(neighborBlock).equals(id("pipe_diamond_item"));
    }
}
