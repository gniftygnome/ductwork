package net.gnomecraft.ductwork.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.util.StringRepresentable;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;
import java.util.function.Consumer;

@NullMarked
public class CreateNeighborChecks extends NeighborChecks {
    private static final BooleanProperty EXTRACTING = BooleanProperty.create("extracting");

    public CreateNeighborChecks() {
        super("create");
    }

    @Override
    public void registerChecks(Consumer<TriFunction<BlockState, Block, Direction, Boolean>> registry) {
        registry.accept(this::mechanicalCrafter);
        registry.accept(this::funnels);
        registry.accept(this::chute);
        registry.accept(this::smartChute);
    }

    // Connect to Create's Mechanical Crafters (really ugly *sigh*)
    private boolean mechanicalCrafter(BlockState neighbor, Block neighborBlock, Direction facing) {
        if (BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("mechanical_crafter")) &&
                neighbor.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {

            for (Property<?> property : neighbor.getProperties()) {
                if ("pointing".equals(property.getName())) {
                    if (CreatePointing.valueOf(neighbor.getValue(property).toString())
                            .getCombinedDirection(neighbor.getValue(BlockStateProperties.HORIZONTAL_FACING))
                            .equals(facing)) {
                        return true;
                    }

                    break;
                }
            }
        }

        return false;
    }

    // Can connect from below when exporting down.
    private boolean funnels(BlockState neighbor, Block neighborBlock, Direction facing) {
        return (
                BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("andesite_funnel")) ||
                BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("brass_funnel"))
            ) && (
                neighbor.hasProperty(BlockStateProperties.FACING) && neighbor.getValue(BlockStateProperties.FACING).equals(Direction.UP) &&
                neighbor.hasProperty(EXTRACTING) && neighbor.getValue(EXTRACTING).equals(false) &&
                facing.equals(Direction.DOWN)
        );
    }

    // Can connect from below unless the chute connects sideways.
    private boolean chute(BlockState neighbor, Block neighborBlock, Direction facing) {
        return BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("chute")) &&
                neighbor.hasProperty(BlockStateProperties.FACING_HOPPER) && neighbor.getValue(BlockStateProperties.FACING_HOPPER).equals(Direction.DOWN) &&
                facing.equals(Direction.DOWN);
    }

    // Can connect from below.
    private boolean smartChute(BlockState neighbor, Block neighborBlock, Direction facing) {
        return BuiltInRegistries.BLOCK.getKey(neighborBlock).equals(id("smart_chute")) &&
                facing.equals(Direction.DOWN);
    }


    /*
     * This equivalent implementation of Create's Pointing class is used exclusively for Create compatibility.
     * (Except ... it's not equivalent ... for some reason I had to swap LEFT and RIGHT (??!!))
     */
    @SuppressWarnings("unused")
    public enum CreatePointing implements StringRepresentable {
        UP(0), RIGHT(90), DOWN(180), LEFT(270);

        private final int xRotation;

        CreatePointing(int xRotation) {
            this.xRotation = xRotation;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int getXRotation() {
            return xRotation;
        }

        public Direction getCombinedDirection(Direction direction) {
            Direction.Axis axis = direction.getAxis();
            Direction top = axis == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
            int rotations = direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 4 - ordinal() : ordinal();

            for (int i = 0; i < rotations; ++i) {
                top = top.getClockWise(axis);
            }

            return top;
        }
    }
}
