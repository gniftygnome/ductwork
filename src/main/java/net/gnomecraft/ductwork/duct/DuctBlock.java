package net.gnomecraft.ductwork.duct;

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
import net.minecraft.world.level.ScheduledTickAccess;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public class DuctBlock extends DuctworkBlock {
    public static final MapCodec<DuctBlock> CODEC = DuctBlock.simpleCodec(DuctBlock::new);
    public static final @Nullable VoxelShape[] DUCT_SHAPE_DICT = new VoxelShape[64];

    public DuctBlock(Properties settings) {
        super(settings);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(NORTH, false)
                .setValue(EAST,  false)
                .setValue(SOUTH, false)
                .setValue(WEST,  false)
                .setValue(DOWN,  false)
                .setValue(UP,    false)
        );

        // Build the static global shape dictionary once when the first DuctBlock is instantiated.
        if (DUCT_SHAPE_DICT[0] == null) {
            buildShapeDict();
        }
    }

    @Override
    protected MapCodec<DuctBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DuctEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, Ductwork.DUCT_ENTITY, DuctEntity::tick);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            ItemStack mainStack = player.getMainHandItem();
            Direction facing = state.getValue(FACING);

            if (player.isShiftKeyDown()) {
                if (mainStack.is(Items.STICK)) {
                    // Sneak + Stick = rotate FACING (pseudowrench)
                    // flags == 0x4 means notify listeners in server only
                    //          0x2 means do update listeners (in general)
                    //          0x1 means do update comparators
                    this.reorient(state, world, pos, this.getNextOrientation(state, FACING, null));
                } else if (mainStack.isEmpty()) {
                    // Sneak + Empty primary = reverse FACING
                    if (!Ductwork.getConfig().vanilla || !facing.equals(Direction.DOWN)) {
                        this.reorient(state, world, pos, facing.getOpposite());
                    }
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

    // Reorient the primary (FACING) orientation of the block with all necessary updates and notifications.
    @Override
    protected void reorient(BlockState state, Level world, BlockPos pos, Direction direction) {
        Direction previous = state.getValue(FACING);

        if (!direction.equals(previous)) {
            BlockState neighbor1 = world.getBlockState(pos.relative(previous));
            BlockState neighbor2 = world.getBlockState(pos.relative(direction));

            state = state.setValue(FACING, direction);

            state = this.getStateWithNeighbor(state, previous, neighbor1);
            state = this.getStateWithNeighbor(state, direction, neighbor2);

            // flags == 0x4 means notify listeners in server only
            //          0x2 means do update listeners (in general)
            //          0x1 means do update comparators
            world.setBlock(pos, state, 6);
        }

    }

    private void openContainer(Level world, BlockPos blockPos, Player playerEntity) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity instanceof DuctEntity) {
            playerEntity.openMenu((MenuProvider) blockEntity);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getClickedFace().getOpposite();

        if (Ductwork.getConfig().vanilla && facing == Direction.UP) {
            facing = Direction.DOWN;
        }

        BlockState state = super.addPlacementState(this.stateDefinition.any(), ctx)
                .setValue(FACING, facing);

        state = resetInputConnections(state, ctx.getLevel(), ctx.getClickedPos());

        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState newState;

        newState = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (!direction.equals(state.getValue(FACING))) {
            newState = getStateWithNeighbor(newState, direction, neighborState);
        }

        return newState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, NORTH, EAST, SOUTH, WEST, DOWN, UP);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        int shapeId;

        switch (state.getValue(FACING)) {
            case NORTH -> shapeId = 1;
            case EAST  -> shapeId = 2;
            case SOUTH -> shapeId = 4;
            case WEST  -> shapeId = 8;
            case DOWN  -> shapeId = 16;
            case UP    -> shapeId = 32;
            default    -> shapeId = 0;
        }

        if (state.getValue(NORTH)) { shapeId |= 1; }
        if (state.getValue(EAST))  { shapeId |= 2; }
        if (state.getValue(SOUTH)) { shapeId |= 4; }
        if (state.getValue(WEST))  { shapeId |= 8; }
        if (state.getValue(DOWN))  { shapeId |= 16; }
        if (state.getValue(UP))    { shapeId |= 32; }

        return Objects.requireNonNull(DUCT_SHAPE_DICT[shapeId]);
    }

    private static void buildShapeDict() {
        VoxelShape CENTER_SHAPE = Block.box(5.0D,  5.0D,  5.0D,  11.0D, 11.0D, 11.0D);
        VoxelShape NORTH_SHAPE  = Block.box(5.0D,  5.0D,  0.0D,  11.0D, 11.0D, 5.0D);
        VoxelShape EAST_SHAPE   = Block.box(11.0D, 5.0D,  5.0D,  16.0D, 11.0D, 11.0D);
        VoxelShape SOUTH_SHAPE  = Block.box(5.0D,  5.0D,  11.0D, 11.0D, 11.0D, 16.0D);
        VoxelShape WEST_SHAPE   = Block.box(0.0D,  5.0D,  5.0D,  5.0D,  11.0D, 11.0D);
        VoxelShape DOWN_SHAPE   = Block.box(5.0D,  0.0D,  5.0D,  11.0D, 5.0D,  11.0D);
        VoxelShape UP_SHAPE     = Block.box(5.0D,  11.0D, 5.0D,  11.0D, 16.0D, 11.0D);

        for (int adjacents = 0; adjacents < 64; ++adjacents) {
            DUCT_SHAPE_DICT[adjacents] = Shapes.or(CENTER_SHAPE,
                    ((adjacents & 1)  != 0) ? NORTH_SHAPE : Shapes.empty(),
                    ((adjacents & 2)  != 0) ? EAST_SHAPE  : Shapes.empty(),
                    ((adjacents & 4)  != 0) ? SOUTH_SHAPE : Shapes.empty(),
                    ((adjacents & 8)  != 0) ? WEST_SHAPE  : Shapes.empty(),
                    ((adjacents & 16) != 0) ? DOWN_SHAPE  : Shapes.empty(),
                    ((adjacents & 32) != 0) ? UP_SHAPE    : Shapes.empty()
            );
        }
    }
}