package net.gnomecraft.ductwork.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

@NullMarked
public abstract class NeighborChecks {
    private static final Collection<TriFunction<BlockState, Block, Direction, Boolean>> NEIGHBOR_CHECKS = new ArrayList<>(16);
    private final String modId;

    protected NeighborChecks(String modId) {
        this.modId = modId;
    }

    public final void register(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        if (FabricLoader.getInstance().isModLoaded(this.modId)) {
            this.registerChecks(registry);
        }
    }

    protected abstract void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry);

    protected Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(this.modId, path);
    }


    public static void init() {
        // Core integrations
        new DuctworkNeighborChecks().register(NEIGHBOR_CHECKS::add);
        new VanillaNeighborChecks().register(NEIGHBOR_CHECKS::add);

        // Optional mod integrations
        new BasaltCrusherNeighborChecks().register(NEIGHBOR_CHECKS::add);
        new CreateNeighborChecks().register(NEIGHBOR_CHECKS::add);
        new DuctsModNeighborChecks().register(NEIGHBOR_CHECKS::add);
        new OmniHopperNeighborChecks().register(NEIGHBOR_CHECKS::add);
        new FlytresPipeModNeighborChecks().register(NEIGHBOR_CHECKS::add);
        new SimplePipesNeighborChecks().register(NEIGHBOR_CHECKS::add);
        new SmartPipesNeighborChecks().register(NEIGHBOR_CHECKS::add);
    }

    public static boolean checkNeighbor(BlockState neighbor, Direction facing) {
        Block neighborBlock = neighbor.getBlock();

        for (TriFunction<BlockState, Block, Direction, Boolean> check : NEIGHBOR_CHECKS) {
            if (check.apply(neighbor, neighborBlock, facing)) {
                return true;
            }
        }

        return false;
    }
}
