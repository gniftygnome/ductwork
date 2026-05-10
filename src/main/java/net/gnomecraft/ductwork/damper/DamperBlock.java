package net.gnomecraft.ductwork.damper;

import com.mojang.serialization.MapCodec;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DamperBlock extends DuctworkBlock {
    public static final MapCodec<DamperBlock> CODEC = DamperBlock.simpleCodec(DamperBlock::new);
    private static final VoxelShape DAMPER_SHAPE_NS_ENABLED = Shapes.or(
            Block.box(5.0D,  5.0D, 0.0D, 11.0D, 11.0D, 16.0D),
            Block.box(3.0D,  7.0D, 4.0D, 5.0D,  9.0D,  12.0D),
            Block.box(11.0D, 7.0D, 4.0D, 13.0D, 9.0D,  12.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_NS_DISABLED = Shapes.or(
            Block.box(5.0D,  5.0D, 0.0D, 11.0D, 11.0D, 16.0D),
            Block.box(3.0D,  4.0D, 7.0D, 5.0D,  12.0D, 9.0D),
            Block.box(11.0D, 4.0D, 7.0D, 13.0D, 12.0D, 9.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_EW_ENABLED = Shapes.or(
            Block.box(0.0D, 5.0D, 5.0D,  16.0D, 11.0D, 11.0D),
            Block.box(4.0D, 7.0D, 3.0D,  12.0D, 9.0D,  5.0D),
            Block.box(4.0D, 7.0D, 11.0D, 12.0D, 9.0D,  13.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_EW_DISABLED = Shapes.or(
            Block.box(0.0D, 5.0D, 5.0D,  16.0D, 11.0D, 11.0D),
            Block.box(7.0D, 4.0D, 3.0D,  9.0D,  12.0D, 5.0D),
            Block.box(7.0D, 4.0D, 11.0D, 9.0D,  12.0D, 13.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_DU_ENABLED = Shapes.or(
            Block.box(5.0D,  0.0D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.box(3.0D,  4.0D, 7.0D, 5.0D,  12.0D, 9.0D),
            Block.box(11.0D, 4.0D, 7.0D, 13.0D, 12.0D, 9.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_DU_DISABLED = Shapes.or(
            Block.box(5.0D,  0.0D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.box(3.0D,  7.0D, 4.0D, 5.0D,  9.0D,  12.0D),
            Block.box(11.0D, 7.0D, 4.0D, 13.0D, 9.0D,  12.0D)
    );

    public DamperBlock(Properties settings) {
        super(settings);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED,true)
        );
    }

    @Override
    protected MapCodec<DamperBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DamperEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Ductwork.DAMPER_ENTITY, DamperEntity::tick);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            ItemStack mainStack = player.getMainHandItem();

            if (player.isShiftKeyDown()) {
                if (mainStack.is(Items.STICK)) {
                    // Sneak + Stick = rotate FACING (pseudowrench)
                    // flags == 0x4 means notify listeners in server only
                    //          0x2 means do update listeners (in general)
                    //          0x1 means do update comparators
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, null));
                } else if (mainStack.isEmpty() && !Ductwork.getConfig().vanilla) {
                    // Sneak + Empty primary = toggle ENABLED
                    world.setBlock(pos, state.setValue(ENABLED, !state.getValue(ENABLED)), 4);
                } else {
                    return InteractionResult.PASS;
                }
            } else {
                if (mainStack.is(Ductwork.WRENCHES)) {
                    // Wrench in primary = rotate FACING
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, null));
                } else if (Ductwork.getConfig().placement && mainStack.is(Ductwork.DUCT_ITEMS)) {
                    // Allow Duct-on-Duct placement if enabled.
                    return InteractionResult.PASS;
                } else {
                    // Otherwise = open container
                    this.openContainer(world, pos, player);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    private void openContainer(Level world, BlockPos blockPos, Player playerEntity) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity instanceof DamperEntity) {
            playerEntity.openMenu((MenuProvider) blockEntity);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getClickedFace().getOpposite();

        if (Ductwork.getConfig().vanilla && facing == Direction.UP) {
            facing = Direction.DOWN;
        }

        @SuppressWarnings({"RedundantSuppression", "UnusedAssignment"})
        BlockState state = super.addPlacementState(this.stateDefinition.any(), ctx)
                .setValue(FACING, facing);

        return state;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        this.updateEnabled(world, pos, state);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState newState;

        newState = state.setValue(ENABLED, !world.hasNeighborSignal(pos));
        newState = super.updateShape(newState, world, tickView, pos, direction, neighborPos, neighborState, random);

        return newState;
    }

    private void updateEnabled(Level world, BlockPos pos, BlockState state) {
        boolean enabled = !world.hasNeighborSignal(pos);
        if (enabled != state.getValue(ENABLED)) {
            BlockState newState = state.setValue(ENABLED, enabled);
            // flags == 0x4 means don't update listeners in client
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlock(pos, newState, 2);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ENABLED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        boolean enabled = state.getValue(ENABLED);

        return switch (state.getValue(FACING)) {
            case NORTH, SOUTH -> enabled ? DAMPER_SHAPE_NS_ENABLED : DAMPER_SHAPE_NS_DISABLED;
            case EAST, WEST -> enabled ? DAMPER_SHAPE_EW_ENABLED : DAMPER_SHAPE_EW_DISABLED;
            default -> enabled ? DAMPER_SHAPE_DU_ENABLED : DAMPER_SHAPE_DU_DISABLED;
        };
    }
}