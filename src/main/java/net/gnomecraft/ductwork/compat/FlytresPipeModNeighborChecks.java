package net.gnomecraft.ductwork.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.Consumer;

public class FlytresPipeModNeighborChecks extends NeighborChecks {
    public FlytresPipeModNeighborChecks() {
        super("pipe");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::pipes);
    }

    // Connect to Flytre's Pipe mod Pipes
    private boolean pipes(BlockState neighbor, Block neighborBlock, Direction facing) {
        // Pipe mods are a generally a pain when it comes to figuring out whether they will deliver to our blocks.
        // So I'm being lazy here and instead of duplicating a giant enum property, I just assume they will...
        return Registries.BLOCK.getId(neighborBlock).equals(id("item_pipe")) ||
                Registries.BLOCK.getId(neighborBlock).equals(id("fast_pipe"));
    }
}
