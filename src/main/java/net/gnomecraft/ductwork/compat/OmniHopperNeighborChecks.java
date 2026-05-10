package net.gnomecraft.ductwork.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
public class OmniHopperNeighborChecks extends NeighborChecks {
    private static final EnumProperty<Direction> POINTY_BIT = EnumProperty.create("pointy_bit", Direction.class);

    public OmniHopperNeighborChecks() {
        super("omnihopper");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::omniHopper);
    }

    // Connect to OmniHopper mod OmniHoppers.
    private boolean omniHopper(BlockState neighbor, Block neighborBlock, Direction facing) {
        return BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("omnihopper")) &&
                neighbor.hasProperty(POINTY_BIT) && neighbor.getValue(POINTY_BIT).equals(facing);
    }
}