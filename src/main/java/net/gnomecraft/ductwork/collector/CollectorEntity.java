package net.gnomecraft.ductwork.collector;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.gnomecraft.cooldowncoordinator.CooldownCoordinator;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/*
 * NOTE:
 * NOTE: CollectorBlock.FACING is the same as all the other Ductwork blocks -- towards the OUTPUT.
 * NOTE: Collecting (extracting) is CollectorBlock.INTAKE which defaults to FACING.getOpposite()...
 * NOTE:
 */
@SuppressWarnings("UnstableApiUsage")
public class CollectorEntity extends DuctworkBlockEntity implements Hopper {
    public final static int currentBlockRev = 2;  // hack around Fabric's missing DFU API
    private int blockRev;

    // Collector area definitions for Hopper.getInputAreaShape()
    private final static VoxelShape INPUT_AREA_SHAPE_NORTH = Block.createCuboidShape(  0.0D,   0.0D, -16.0D, 16.0D, 16.0D,  0.0D);
    private final static VoxelShape INPUT_AREA_SHAPE_EAST  = Block.createCuboidShape( 16.0D,   0.0D,   0.0D, 32.0D, 16.0D, 16.0D);
    private final static VoxelShape INPUT_AREA_SHAPE_SOUTH = Block.createCuboidShape(  0.0D,   0.0D,  16.0D, 16.0D, 16.0D, 32.0D);
    private final static VoxelShape INPUT_AREA_SHAPE_WEST  = Block.createCuboidShape(-16.0D,   0.0D,   0.0D,  0.0D, 16.0D, 16.0D);
    private final static VoxelShape INPUT_AREA_SHAPE_DOWN  = Block.createCuboidShape(  0.0D, -16.0D,   0.0D, 16.0D,  0.0D, 16.0D);
    private final static VoxelShape INPUT_AREA_SHAPE_UP    = Block.createCuboidShape(  0.0D,  16.0D,   0.0D, 16.0D, 32.0D, 16.0D);

    public CollectorEntity(BlockPos pos, BlockState state) {
        super(Ductwork.COLLECTOR_ENTITY, pos, state);

        this.inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
        this.transferCooldown = 0;
        this.blockRev = -1;
    }

    @Override
    public ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new CollectorScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        // Implement hack around Fabric's missing DFU API.
        if (this.blockRev >= 0) {
            tag.putShort("BlockRev", (short) this.blockRev);
        }

        super.writeNbt(tag);
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        // Implement hack around Fabric's missing DFU API.
        this.blockRev = tag.getShort("BlockRev");
    }

    public static void tick(World world, BlockPos pos, BlockState state, CollectorEntity entity) {
        boolean dirty = false;

        if (world == null || world.isClient()) {
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
                    Direction intake = state.get(CollectorBlock.FACING).getOpposite();
                    Ductwork.LOGGER.info("Collector at (" + pos.toShortString() + ") has BlockRev " + entity.blockRev + "; setting INTAKE to " + intake);
                    world.setBlockState(pos, state.with(CollectorBlock.INTAKE, intake));
                    entity.blockRev = 1;
                case 1:
                    // BlockRev 1 did not connect to inputs, so re-check connections.
                    world.setBlockState(pos, ((CollectorBlock) state.getBlock()).resetInputConnections(state, world, pos));
                    entity.blockRev = 2;
                default:
                    if (entity.blockRev != CollectorEntity.currentBlockRev) {
                        Ductwork.LOGGER.warn("Collector at " + pos + " has rev " + entity.blockRev
                                + " but our latest known rev is " + CollectorEntity.currentBlockRev
                                + " ... expect trouble!");
                    }
            }
            dirty = true;
        }

        entity.lastTickTime = world.getTime();

        // If we are in cooldown, decrement.
        if (entity.transferCooldown > 0) {
            --entity.transferCooldown;
            dirty = true;
        }

        // If we are enabled and out of cooldown,
        if (state.get(CollectorBlock.ENABLED) && entity.transferCooldown <= 0) {
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
            entity.markDirty();
        }
    }

    // Fabric transfer API implementation; uses coordinated cooldown notification.
    private boolean push(World world, BlockPos pos, BlockState state, CollectorEntity entity) {
        Direction facing = state.get(CollectorBlock.FACING);
        BlockEntity targetEntity = world.getBlockEntity(pos.offset(facing));
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(world, pos, state, entity, facing);
        Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(world, pos.offset(facing), facing.getOpposite());

        if (sourceStorage != null && targetStorage != null) {
            boolean targetEmpty = CooldownCoordinator.isStorageEmpty(targetStorage);

            if (StorageUtil.move(sourceStorage, targetStorage, variant -> true, 1, null) > 0) {
                if (targetEmpty) {
                    CooldownCoordinator.notify(targetEntity);
                }
                if (targetEntity != null) {
                    targetEntity.markDirty();
                }

                return true;
            }
        }

        return false;
    }

    // Fabric transfer API implementation required here too because hopper extract only looks up...
    // No cooldown coordination is required when extracting; only the local (target) cooldown needs to be updated.
    private boolean pull(World world, BlockPos pos, BlockState state, CollectorEntity entity) {
        // Extraction (intake) on the opposite side, in the opposite direction...
        Direction intake = state.get(CollectorBlock.INTAKE);
        Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(world, pos, state, entity, intake);

        // Try to find storage first.  This will also find stationary inventories.
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(world, pos.offset(intake), intake.getOpposite());

        // If we don't find storage, try to find an inventory like a hopper would.
        // This will find mobile inventories (hopper or chest minecart, chest boat?, etc.).
        // HopperBlockEntity.getInventoryAt() will pick one of the available mobile inventories at random.
        if (sourceStorage == null) {
            Inventory sourceInventory =  HopperBlockEntity.getInventoryAt(world, pos.offset(intake));
            if (sourceInventory != null) {
                sourceStorage = InventoryStorage.of(sourceInventory, intake.getOpposite());
            }
        }

        // Try to pull from any discovered storage or inventory...
        if (sourceStorage != null && targetStorage != null) {
            boolean result =  (StorageUtil.move(sourceStorage, targetStorage, variant -> true, 1, null) > 0);
            BlockEntity sourceEntity = world.getBlockEntity(pos.offset(intake));
            if (sourceEntity != null) {
                sourceEntity.markDirty();
            }
            return result;
        }

        // Then if no inventory was found, try to pull entities through the intake of the collector.
        // We implement the Hopper interface so we can do this small bit of code reuse.
        for (ItemEntity itemEntity : HopperBlockEntity.getInputItemEntities(world, entity)) {
            if (HopperBlockEntity.extract(entity, itemEntity)) {
                return true;
            }
        }

        // At this point no items could be found by any means and pull() has moved nothing.
        return false;
    }

    @Override
    public VoxelShape getInputAreaShape() {
        return switch (this.getCachedState().get(CollectorBlock.INTAKE)) {
            case NORTH -> INPUT_AREA_SHAPE_NORTH;
            case EAST  -> INPUT_AREA_SHAPE_EAST;
            case SOUTH -> INPUT_AREA_SHAPE_SOUTH;
            case WEST  -> INPUT_AREA_SHAPE_WEST;
            case DOWN  -> INPUT_AREA_SHAPE_DOWN;
            case UP    -> INPUT_AREA_SHAPE_UP;
        };
    }

    @Override
    public double getHopperX() {
        return (double)this.pos.getX() + 0.5;
    }

    @Override
    public double getHopperY() {
        return (double)this.pos.getY() + 0.5;
    }

    @Override
    public double getHopperZ() {
        return (double)this.pos.getZ() + 0.5;
    }

}