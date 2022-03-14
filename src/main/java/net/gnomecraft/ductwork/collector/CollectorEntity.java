package net.gnomecraft.ductwork.collector;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.gnomecraft.cooldowncoordinator.*;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.Iterator;

/*
 * NOTE:
 * NOTE: CollectorBlock.FACING is the same as all the other Ductwork blocks -- towards the OUTPUT.
 * NOTE: That means when collecting (extracting) everything is backward (FACING.getOpposite())...
 * NOTE:
 */
public class CollectorEntity extends LockableContainerBlockEntity implements CoordinatedCooldown, Hopper, SidedInventory {
    public final static int defaultCooldown = 8;  // 4 redstone ticks, just like vanilla
    private final static int[] SLOTS = new int[] {0, 1, 2, 3, 4};
    private DefaultedList<ItemStack> inventory;
    private long lastTickTime;
    private int transferCooldown;

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
    }

    @Override
    public ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new CollectorScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public Text getContainerName() {
        return new TranslatableText(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        Inventories.writeNbt(tag, this.inventory);
        tag.putShort("TransferCooldown", (short)this.transferCooldown);

        super.writeNbt(tag);
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);

        this.transferCooldown = tag.getShort("TransferCooldown");
        inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(tag, this.inventory);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CollectorEntity entity) {
        boolean dirty = false;

        if (world == null || world.isClient()) {
            return;
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
            boolean targetEmpty = CooldownCoordinator.isItemStorageEmpty(targetStorage);

            if (StorageUtil.move(sourceStorage, targetStorage, variant -> true, 1, null) > 0) {
                if (targetEmpty) {
                    CooldownCoordinator.notify(targetEntity);
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
        Direction intake = state.get(CollectorBlock.FACING).getOpposite();
        Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(world, pos, state, entity, intake);
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(world, pos.offset(intake), intake.getOpposite());

        // First try to pull from an attached storage or inventory.
        if (sourceStorage != null && targetStorage != null) {
            return (StorageUtil.move(sourceStorage, targetStorage, variant -> true, 1, null) > 0);
        }

        // Then if nothing was attached, try to pull entities through the intake of the collector.
        // We implement the Hopper interface so we can do this small bit of code reuse.
        for (ItemEntity itemEntity : HopperBlockEntity.getInputItemEntities(world, entity)) {
            if (HopperBlockEntity.extract(entity, itemEntity)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void notifyCooldown() {
        if (world == null) {
            return;
        }

        if (this.lastTickTime >= world.getTime()) {
            this.transferCooldown = CollectorEntity.defaultCooldown - 1;
        } else {
            this.transferCooldown = CollectorEntity.defaultCooldown;
        }

        this.markDirty();
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsert(int index, ItemStack stack, Direction direction) {
        Direction facing = this.getCachedState().get(CollectorBlock.FACING);
        // The direction == null lunacy is required by the Hopper collector implementation...
        if (facing != null && (direction == null || direction == facing || direction == facing.getOpposite())) {
            return this.isValid(index, stack);
        }

        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        Iterator<ItemStack> invIterator = this.inventory.iterator();

        ItemStack stack;
        do {
            if (!invIterator.hasNext()) {
                return true;
            }

            stack = invIterator.next();
        } while (stack.isEmpty());

        return false;
    }

    public boolean isFull() {
        Iterator<ItemStack> invIterator = this.inventory.iterator();

        ItemStack stack;
        do {
            if (!invIterator.hasNext()) {
                return true;
            }

            stack = invIterator.next();
        } while (!stack.isEmpty() && stack.getCount() >= stack.getMaxCount());

        return false;
    }

    @Override
    public ItemStack getStack(int index) {
        return this.inventory.get(index);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int index, ItemStack stack) {
        this.inventory.set(index, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }

        this.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world == null || this.world.getBlockEntity(this.pos) != this) {
            return false;
        } else {
            return player.squaredDistanceTo((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public VoxelShape getInputAreaShape() {
        // Extraction on the opposite side from facing.
        return switch (this.getCachedState().get(CollectorBlock.FACING).getOpposite()) {
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