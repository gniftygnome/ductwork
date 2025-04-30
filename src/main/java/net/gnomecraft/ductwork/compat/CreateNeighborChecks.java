package net.gnomecraft.ductwork.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Locale;
import java.util.function.Consumer;

public class CreateNeighborChecks extends NeighborChecks {
    private static final BooleanProperty EXTRACTING = BooleanProperty.of("extracting");

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
        if (Registries.BLOCK.getId(neighborBlock).equals(id("mechanical_crafter")) &&
                neighbor.contains(Properties.HORIZONTAL_FACING)) {

            for (Property<?> property : neighbor.getProperties()) {
                if ("pointing".equals(property.getName())) {
                    if (CreatePointing.valueOf(neighbor.get(property).toString())
                            .getCombinedDirection(neighbor.get(Properties.HORIZONTAL_FACING))
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
                Registries.BLOCK.getId(neighborBlock).equals(id("andesite_funnel")) ||
                Registries.BLOCK.getId(neighborBlock).equals(id("brass_funnel"))
            ) && (
                neighbor.contains(Properties.FACING) && neighbor.get(Properties.FACING).equals(Direction.UP) &&
                neighbor.contains(EXTRACTING) && neighbor.get(EXTRACTING).equals(false) &&
                facing.equals(Direction.DOWN)
        );
    }

    // Can connect from below unless the chute connects sideways.
    private boolean chute(BlockState neighbor, Block neighborBlock, Direction facing) {
        return Registries.BLOCK.getId(neighborBlock).equals(id("chute")) &&
                neighbor.contains(Properties.HOPPER_FACING) && neighbor.get(Properties.HOPPER_FACING).equals(Direction.DOWN) &&
                facing.equals(Direction.DOWN);
    }

    // Can connect from below.
    private boolean smartChute(BlockState neighbor, Block neighborBlock, Direction facing) {
        return Registries.BLOCK.getId(neighborBlock).equals(id("smart_chute")) &&
                facing.equals(Direction.DOWN);
    }


    /*
     * This equivalent implementation of Create's Pointing class is used exclusively for Create compatibility.
     * (Except ... it's not equivalent ... for some reason I had to swap LEFT and RIGHT (??!!))
     */
    @SuppressWarnings("unused")
    public enum CreatePointing implements StringIdentifiable {
        UP(0), RIGHT(90), DOWN(180), LEFT(270);

        private final int xRotation;

        CreatePointing(int xRotation) {
            this.xRotation = xRotation;
        }

        @Override
        public String asString() {
            return name().toLowerCase(Locale.ROOT);
        }

        public int getXRotation() {
            return xRotation;
        }

        public Direction getCombinedDirection(Direction direction) {
            Direction.Axis axis = direction.getAxis();
            Direction top = axis == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
            int rotations = direction.getDirection() == Direction.AxisDirection.NEGATIVE ? 4 - ordinal() : ordinal();

            for (int i = 0; i < rotations; ++i) {
                top = top.rotateClockwise(axis);
            }

            return top;
        }
    }
}
