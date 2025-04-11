package net.gnomecraft.ductwork.base;

import net.gnomecraft.cooldowncoordinator.CoordinatedCooldown;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public abstract class DuctworkBlockEntity extends LockableContainerBlockEntity implements CoordinatedCooldown, Inventory {
    public final static int defaultCooldown = 8;  // 4 redstone ticks, just like vanilla
    protected DefaultedList<ItemStack> inventory;
    protected long lastTickTime;
    protected int transferCooldown;

    protected DuctworkBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public Text getContainerName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public void writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.writeNbt(tag, this.inventory, registryLookup);
        tag.putShort("TransferCooldown", (short)this.transferCooldown);

        super.writeNbt(tag, registryLookup);
    }

    @Override
    public void readNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(tag, registryLookup);

        this.transferCooldown = tag.getShort("TransferCooldown").orElse((short) 0);
        inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(tag, this.inventory, registryLookup);
    }

    @Override
    public void notifyCooldown() {
        if (world == null) {
            return;
        }

        if (this.lastTickTime >= world.getTime()) {
            this.transferCooldown = DuctworkBlockEntity.defaultCooldown - 1;
        } else {
            this.transferCooldown = DuctworkBlockEntity.defaultCooldown;
        }

        this.markDirty();
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    /**
     * Standard Inventory method to evaluate whether an Inventory is completely empty.
     * This method can be used to short-circuit when considering an extraction from the
     * Inventory because a true result guarantees the extraction cannot possibly succeed.
     *
     * @return True when there are no items in the Inventory; False otherwise
     */
    @Override
    public boolean isEmpty() {
        java.util.Iterator<ItemStack> invIterator = this.inventory.iterator();

        ItemStack stack;
        do {
            if (!invIterator.hasNext()) {
                return true;
            }

            stack = invIterator.next();
        } while (stack.isEmpty());

        return false;
    }

    /**
     * Complement for the standard Inventory method isEmpty().  This method can be used to
     * short-circuit when considering an insertion because a true result guarantees the
     * insertion cannot possibly succeed.
     *
     * This method does not consider nested Inventories because Minecraft does not insert
     * into nested Inventories.  The design may be revised if the Fabric transfer API gains
     * support for nested Inventories.
     *
     * In practice, isFull() is most useful when the target Inventory is relatively likely
     * to be full.  The exception is when capturing items from the World, which involves
     * computationally expensive iterations and vector mathematics.
     *
     * @return True when no item could be added to the Inventory; False otherwise
     */
    public boolean isFull() {
        java.util.Iterator<ItemStack> invIterator = this.inventory.iterator();

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
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        if (inventory.size() != this.inventory.size()) {
            throw new IllegalArgumentException("setHeldStacks called with wrong-size inventory");
        }

        this.inventory = inventory;
    }
}