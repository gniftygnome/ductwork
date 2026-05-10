package net.gnomecraft.ductwork.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
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
        return BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("pipe_wooden_item")) ||
                BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("pipe_stone_item")) ||
                BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("pipe_clay_item")) ||
                BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("pipe_iron_item")) ||
                BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("pipe_gold_item")) ||
                BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("pipe_diamond_item"));
    }
}
