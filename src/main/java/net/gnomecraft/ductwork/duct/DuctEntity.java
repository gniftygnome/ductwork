package net.gnomecraft.ductwork.duct;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.gnomecraft.cooldowncoordinator.*;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Iterator;

@SuppressWarnings("UnstableApiUsage")
public class DuctEntity extends LockableContainerBlockEntity implements CoordinatedCooldown, Inventory {
    public final static int defaultCooldown = 8;  // 4 redstone ticks, just like vanilla
    private DefaultedList<ItemStack> inventory;
    private long lastTickTime;
    private int transferCooldown;

    public DuctEntity(BlockPos pos, BlockState state) {
        super(Ductwork.DUCT_ENTITY, pos, state);

        this.inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
        this.transferCooldown = 0;
    }

    @Override
    public ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new DuctScreenHandler(syncId, playerInventory, this);
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

    public static void tick(World world, BlockPos pos, BlockState state, DuctEntity entity) {
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

        // If we are out of cooldown and we have inventory, try to push it.
        if (entity.transferCooldown <= 0 && !entity.isEmpty()) {
            if (entity.push(world, pos, state, entity)) {
                entity.transferCooldown = DuctEntity.defaultCooldown;
                dirty = true;
            }
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    // Fabric transfer API implementation; uses coordinated cooldown notification.
    private boolean push(World world, BlockPos pos, BlockState state, DuctEntity entity) {
        Direction facing = state.get(DuctBlock.FACING);
        BlockEntity targetEntity = world.getBlockEntity(pos.offset(facing));
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(world, pos, state, entity, facing);
        Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(world, pos.offset(facing), facing.getOpposite());

        if (sourceStorage != null && targetStorage != null) {
            boolean targetEmpty = CooldownCoordinator.isItemStorageEmpty(targetStorage);

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

    @Override
    public void notifyCooldown() {
        if (world == null) {
            return;
        }

        if (this.lastTickTime >= world.getTime()) {
            this.transferCooldown = DuctEntity.defaultCooldown - 1;
        } else {
            this.transferCooldown = DuctEntity.defaultCooldown;
        }

        this.markDirty();
    }

    @Override
    public int size() {
        return this.inventory.size();
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
        } while (stack.isEmpty() || stack.getCount() < stack.getMaxCount());

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

}