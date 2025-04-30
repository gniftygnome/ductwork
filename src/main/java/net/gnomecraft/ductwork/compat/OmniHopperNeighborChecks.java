package net.gnomecraft.ductwork.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.Consumer;

public class OmniHopperNeighborChecks extends NeighborChecks {
    private static final EnumProperty<Direction> POINTY_BIT = EnumProperty.of("pointy_bit", Direction.class);

    public OmniHopperNeighborChecks() {
        super("omnihopper");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::omniHopper);
    }

    // Connect to OmniHopper mod OmniHoppers.
    private boolean omniHopper(BlockState neighbor, Block neighborBlock, Direction facing) {
        return Registries.BLOCK.getId(neighborBlock).equals(id("omnihopper")) &&
                neighbor.contains(POINTY_BIT) && neighbor.get(POINTY_BIT).equals(facing);
    }
}