package net.gnomecraft.ductwork.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
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
        return BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("smart_pipe"));
    }
}
