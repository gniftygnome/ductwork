package net.gnomecraft.ductwork.collector;

import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.gnomecraft.cooldowncoordinator.CooldownCoordinator;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NullMarked;

/*
 * NOTE:
 * NOTE: CollectorBlock.FACING is the same as all the other Ductwork blocks -- towards the OUTPUT.
 * NOTE: Collecting (extracting) is CollectorBlock.INTAKE which defaults to FACING.getOpposite()...
 * NOTE:
 */
@NullMarked
public class CollectorEntity extends DuctworkBlockEntity implements Hopper {
    public final static int currentBlockRev = 2;  // hack around Fabric's missing DFU API
    private int blockRev;

    // Collector area definitions for Hopper.getInputAreaShape()
    private final static AABB INPUT_AREA_SHAPE_NORTH = Block.box(  0.0D,   0.0D, -16.0D, 16.0D, 16.0D,  0.0D).toAabbs().getFirst();
    private final static AABB INPUT_AREA_SHAPE_EAST  = Block.box( 16.0D,   0.0D,   0.0D, 32.0D, 16.0D, 16.0D).toAabbs().getFirst();
    private final static AABB INPUT_AREA_SHAPE_SOUTH = Block.box(  0.0D,   0.0D,  16.0D, 16.0D, 16.0D, 32.0D).toAabbs().getFirst();
    private final static AABB INPUT_AREA_SHAPE_WEST  = Block.box(-16.0D,   0.0D,   0.0D,  0.0D, 16.0D, 16.0D).toAabbs().getFirst();
    private final static AABB INPUT_AREA_SHAPE_DOWN  = Block.box(  0.0D, -16.0D,   0.0D, 16.0D,  0.0D, 16.0D).toAabbs().getFirst();
    private final static AABB INPUT_AREA_SHAPE_UP    = Block.box(  0.0D,  16.0D,   0.0D, 16.0D, 32.0D, 16.0D).toAabbs().getFirst();

    public CollectorEntity(BlockPos pos, BlockState state) {
        super(Ductwork.COLLECTOR_ENTITY, pos, state, 5);

        this.transferCooldown = 0;
        this.blockRev = -1;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new CollectorScreenHandler(syncId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        // Implement hack around Fabric's missing DFU API.
        if (this.blockRev >= 0) {
            view.putInt("BlockRev", this.blockRev);
        }

        super.saveAdditional(view);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);

        // Implement hack around Fabric's missing DFU API.
        this.blockRev = view.getIntOr("BlockRev", 0);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, CollectorEntity entity) {
        boolean dirty = false;

        if (world.isClientSide()) {
            return;
        }

        // "Later Fixer Upper" -- Implement hack around Fabric's missing DFU API.
        if (CollectorEntity.currentBlockRev > entity.blockRev) {
            switch (entity.blockRev) {
                case -1:
                    // Newly created block; assume clean entity.
                    entity.blockRev = CollectorEntity.currentBlockRev;
                    break;
                case 0:
                    // BlockRev 0 INTAKE was hard-coded to opposite of facing; reinitialize it.
                    Direction intake = state.getValue(CollectorBlock.FACING).getOpposite();
                    Ductwork.LOGGER.info("Collector at ({}) has BlockRev {}; setting INTAKE to {}", pos.toShortString(), entity.blockRev, intake);
                    world.setBlockAndUpdate(pos, state.setValue(CollectorBlock.INTAKE, intake));
                    entity.blockRev = 1;
                case 1:
                    // BlockRev 1 did not connect to inputs, so re-check connections.
                    world.setBlockAndUpdate(pos, ((CollectorBlock) state.getBlock()).resetInputConnections(state, world, pos));
                    entity.blockRev = 2;
                default:
                    if (entity.blockRev != CollectorEntity.currentBlockRev) {
                        Ductwork.LOGGER.warn("Collector at {} has rev {} but our latest known rev is {} ... expect trouble!", pos, entity.blockRev, CollectorEntity.currentBlockRev);
                    }
            }
            dirty = true;
        }

        entity.lastTickTime = world.getGameTime();

        // If we are in cooldown, decrement.
        if (entity.transferCooldown > 0) {
            --entity.transferCooldown;
            dirty = true;
        }

        // If we are enabled and out of cooldown,
        if (state.getValue(CollectorBlock.ENABLED) && entity.transferCooldown <= 0) {
            // If we have inventory, try to push it.
            if (!entity.isEmpty() && entity.push(world, pos, state, entity)) {
                entity.transferCooldown = CollectorEntity.defaultCooldown;
                dirty = true;
            }

            // If we have space, try to fill it.
            if (!entity.isFull() && entity.pull(world, pos, state, entity)) {
                entity.transferCooldown = CollectorEntity.defaultCooldown;
                dirty = true;
            }
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    // Fabric transfer API implementation; uses coordinated cooldown notification.
    private boolean push(Level world, BlockPos pos, BlockState state, CollectorEntity entity) {
        Direction facing = state.getValue(CollectorBlock.FACING);
        BlockEntity targetEntity = world.getBlockEntity(pos.relative(facing));
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(world, pos, state, entity, facing);
        Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(world, pos.relative(facing), facing.getOpposite());

        if (sourceStorage != null && targetStorage != null) {
            boolean targetEmpty = CooldownCoordinator.isStorageEmpty(targetStorage);

            if (StorageUtil.move(sourceStorage, targetStorage, variant -> true, 1, null) > 0) {
                if (targetEmpty) {
                    CooldownCoordinator.notify(targetEntity);
                }
                if (targetEntity != null) {
                    targetEntity.setChanged();
                }

                return true;
            }
        }

        return false;
    }

    // Fabric transfer API implementation required here too because hopper extract only looks up...
    // No cooldown coordination is required when extracting; only the local (target) cooldown needs to be updated.
    private boolean pull(Level world, BlockPos pos, BlockState state, CollectorEntity entity) {
        // Extraction (intake) on the opposite side, in the opposite direction...
        Direction intake = state.getValue(CollectorBlock.INTAKE);
        BlockPos sourcePos = pos.relative(intake);
        Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(world, pos, state, entity, intake);

        // Try to find storage first.  This will also find stationary inventories.
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(world, sourcePos, intake.getOpposite());

        // If we don't find storage, try to find an inventory like a hopper would.
        // This will find mobile inventories (hopper or chest minecart, chest boat?, etc.).
        // HopperBlockEntity.getInventoryAt() will pick one of the available mobile inventories at random.
        if (sourceStorage == null) {
            Container sourceInventory =  HopperBlockEntity.getContainerAt(world, sourcePos);
            if (sourceInventory != null) {
                sourceStorage = ContainerStorage.of(sourceInventory, intake.getOpposite());
            }
        }

        // Try to pull from any discovered storage or inventory...
        if (sourceStorage != null && targetStorage != null) {
            boolean result =  (StorageUtil.move(sourceStorage, targetStorage, variant -> true, 1, null) > 0);
            BlockEntity sourceEntity = world.getBlockEntity(sourcePos);
            if (sourceEntity != null) {
                sourceEntity.setChanged();
            }
            return result;
        }

        // Then if no inventory was found, try to pull entities through the intake of the collector.
        // We implement the Hopper interface so we can do this small bit of code reuse.
        BlockState sourceState = world.getBlockState(sourcePos);
        if (!sourceState.isCollisionShapeFullBlock(world, sourcePos) || sourceState.is(BlockTags.DOES_NOT_BLOCK_HOPPERS)) {
            for (ItemEntity itemEntity : HopperBlockEntity.getItemsAtAndAbove(world, entity)) {
                if (HopperBlockEntity.addItem(entity, itemEntity)) {
                    return true;
                }
            }
        }

        // At this point no items could be found by any means and pull() has moved nothing.
        return false;
    }

    @Override
    public AABB getSuckAabb() {
        return switch (this.getBlockState().getValue(CollectorBlock.INTAKE)) {
            case NORTH -> INPUT_AREA_SHAPE_NORTH;
            case EAST  -> INPUT_AREA_SHAPE_EAST;
            case SOUTH -> INPUT_AREA_SHAPE_SOUTH;
            case WEST  -> INPUT_AREA_SHAPE_WEST;
            case DOWN  -> INPUT_AREA_SHAPE_DOWN;
            case UP    -> INPUT_AREA_SHAPE_UP;
        };
    }

    @Override
    public double getLevelX() {
        return (double)this.worldPosition.getX() + 0.5;
    }

    @Override
    public double getLevelY() {
        return (double)this.worldPosition.getY() + 0.5;
    }

    @Override
    public double getLevelZ() {
        return (double)this.worldPosition.getZ() + 0.5;
    }

    @Override
    public boolean isGridAligned() {
        // We implement our own sided version of the associated logic in the `pull` method.
        return false;
    }
}