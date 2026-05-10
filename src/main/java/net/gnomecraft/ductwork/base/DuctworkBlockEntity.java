package net.gnomecraft.ductwork.base;

import net.gnomecraft.cooldowncoordinator.CoordinatedCooldown;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class DuctworkBlockEntity extends BaseContainerBlockEntity implements CoordinatedCooldown, Container {
    public final static int defaultCooldown = 8;  // 4 redstone ticks, just like vanilla
    protected NonNullList<ItemStack> inventory;
    protected long lastTickTime;
    protected int transferCooldown;

    protected DuctworkBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, int inventorySize) {
        super(blockEntityType, blockPos, blockState);

        if (inventorySize < 1 || inventorySize > 1024) {
            throw new IllegalArgumentException("Inventory size must be between 1 and 1024 inclusive.");
        }

        this.inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.inventory);
        view.putInt("TransferCooldown", this.transferCooldown);

        super.saveAdditional(view);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);

        this.transferCooldown = view.getIntOr("TransferCooldown", 0);
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(view, this.inventory);
    }

    @Override
    public void notifyCooldown() {
        if (level == null) {
            return;
        }

        if (this.lastTickTime >= level.getGameTime()) {
            this.transferCooldown = DuctworkBlockEntity.defaultCooldown - 1;
        } else {
            this.transferCooldown = DuctworkBlockEntity.defaultCooldown;
        }

        this.setChanged();
    }

    @Override
    public int getContainerSize() {
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
     * <p/>
     * This method does not consider nested Inventories because Minecraft does not insert
     * into nested Inventories.  The design may be revised if the Fabric transfer API gains
     * support for nested Inventories.
     * <p/>
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
        } while (!stack.isEmpty() && stack.getCount() >= stack.getMaxStackSize());

        return false;
    }

    @Override
    public ItemStack getItem(int index) {
        return this.inventory.get(index);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.inventory, slot);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.inventory.set(index, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> inventory) {
        if (inventory.size() != this.inventory.size()) {
            throw new IllegalArgumentException("setHeldStacks called with wrong-size inventory");
        }

        this.inventory = inventory;
    }
}