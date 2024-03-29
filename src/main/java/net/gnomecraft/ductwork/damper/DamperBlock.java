package net.gnomecraft.ductwork.damper;

import com.mojang.serialization.MapCodec;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class DamperBlock extends DuctworkBlock {
    public static final MapCodec<DamperBlock> CODEC = DamperBlock.createCodec(DamperBlock::new);
    private static final VoxelShape DAMPER_SHAPE_NS_ENABLED = VoxelShapes.union(
            Block.createCuboidShape(5.0D,  5.0D, 0.0D, 11.0D, 11.0D, 16.0D),
            Block.createCuboidShape(3.0D,  7.0D, 4.0D, 5.0D,  9.0D,  12.0D),
            Block.createCuboidShape(11.0D, 7.0D, 4.0D, 13.0D, 9.0D,  12.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_NS_DISABLED = VoxelShapes.union(
            Block.createCuboidShape(5.0D,  5.0D, 0.0D, 11.0D, 11.0D, 16.0D),
            Block.createCuboidShape(3.0D,  4.0D, 7.0D, 5.0D,  12.0D, 9.0D),
            Block.createCuboidShape(11.0D, 4.0D, 7.0D, 13.0D, 12.0D, 9.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_EW_ENABLED = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 5.0D, 5.0D,  16.0D, 11.0D, 11.0D),
            Block.createCuboidShape(4.0D, 7.0D, 3.0D,  12.0D, 9.0D,  5.0D),
            Block.createCuboidShape(4.0D, 7.0D, 11.0D, 12.0D, 9.0D,  13.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_EW_DISABLED = VoxelShapes.union(
            Block.createCuboidShape(0.0D, 5.0D, 5.0D,  16.0D, 11.0D, 11.0D),
            Block.createCuboidShape(7.0D, 4.0D, 3.0D,  9.0D,  12.0D, 5.0D),
            Block.createCuboidShape(7.0D, 4.0D, 11.0D, 9.0D,  12.0D, 13.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_DU_ENABLED = VoxelShapes.union(
            Block.createCuboidShape(5.0D,  0.0D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.createCuboidShape(3.0D,  4.0D, 7.0D, 5.0D,  12.0D, 9.0D),
            Block.createCuboidShape(11.0D, 4.0D, 7.0D, 13.0D, 12.0D, 9.0D)
    );
    private static final VoxelShape DAMPER_SHAPE_DU_DISABLED = VoxelShapes.union(
            Block.createCuboidShape(5.0D,  0.0D, 5.0D, 11.0D, 16.0D, 11.0D),
            Block.createCuboidShape(3.0D,  7.0D, 4.0D, 5.0D,  9.0D,  12.0D),
            Block.createCuboidShape(11.0D, 7.0D, 4.0D, 13.0D, 9.0D,  12.0D)
    );

    public DamperBlock(Settings settings) {
        super(settings);

        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.DOWN)
                .with(ENABLED,true)
        );
    }

    @Override
    protected MapCodec<DamperBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DamperEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, Ductwork.DAMPER_ENTITY, DamperEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            ItemStack mainStack = player.getMainHandStack();

            if (player.isSneaking()) {
                if (mainStack.isOf(Items.STICK)) {
                    // Sneak + Stick = rotate FACING (pseudowrench)
                    // flags == 0x4 means notify listeners in server only
                    //          0x2 means do update listeners (in general)
                    //          0x1 means do update comparators
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, null));
                } else if (mainStack.isEmpty() && !Ductwork.getConfig().vanilla) {
                    // Sneak + Empty primary = toggle ENABLED
                    world.setBlockState(pos, state.with(ENABLED, !state.get(ENABLED)), 4);
                } else {
                    return ActionResult.PASS;
                }
            } else {
                if (mainStack.isIn(Ductwork.WRENCHES)) {
                    // Wrench in primary = rotate FACING
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, null));
                } else if (Ductwork.getConfig().placement && mainStack.isIn(Ductwork.DUCT_ITEMS)) {
                    // Allow Duct-on-Duct placement if enabled.
                    return ActionResult.PASS;
                } else {
                    // Otherwise = open container
                    this.openContainer(world, pos, player);
                }
            }
        }

        return ActionResult.SUCCESS;
    }

    private void openContainer(World world, BlockPos blockPos, PlayerEntity playerEntity) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity instanceof DamperEntity) {
            playerEntity.openHandledScreen((NamedScreenHandlerFactory) blockEntity);
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getSide().getOpposite();

        if (Ductwork.getConfig().vanilla && facing == Direction.UP) {
            facing = Direction.DOWN;
        }

        @SuppressWarnings({"RedundantSuppression", "UnusedAssignment"})
        BlockState state = super.addPlacementState(this.stateManager.getDefaultState(), ctx)
                .with(FACING, facing);

        return state;
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (world != null && !oldState.isOf(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        if (world != null) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        BlockState newState;

        newState = state.with(ENABLED, !world.isReceivingRedstonePower(pos));
        newState = super.getStateForNeighborUpdate(newState, direction, neighbor, world, pos, neighborPos);

        return newState;
    }

    private void updateEnabled(World world, BlockPos pos, BlockState state) {
        boolean enabled = !world.isReceivingRedstonePower(pos);
        if (enabled != state.get(ENABLED)) {
            BlockState newState = state.with(ENABLED, enabled);
            // flags == 0x4 means don't update listeners in client
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlockState(pos, newState, 2);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof DamperEntity) {
                ItemScatterer.spawn(world, pos, (DamperEntity)blockEntity);
                world.updateComparators(pos,this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, ENABLED);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean enabled = state.get(ENABLED);

        return switch (state.get(FACING)) {
            case NORTH, SOUTH -> enabled ? DAMPER_SHAPE_NS_ENABLED : DAMPER_SHAPE_NS_DISABLED;
            case EAST, WEST -> enabled ? DAMPER_SHAPE_EW_ENABLED : DAMPER_SHAPE_EW_DISABLED;
            default -> enabled ? DAMPER_SHAPE_DU_ENABLED : DAMPER_SHAPE_DU_DISABLED;
        };
    }
}