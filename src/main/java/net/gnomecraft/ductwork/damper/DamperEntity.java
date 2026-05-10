package net.gnomecraft.ductwork.damper;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.gnomecraft.cooldowncoordinator.CooldownCoordinator;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DamperEntity extends DuctworkBlockEntity implements WorldlyContainer {
    public DamperEntity(BlockPos pos, BlockState state) {
        super(Ductwork.DAMPER_ENTITY, pos, state, 1);

        this.transferCooldown = 0;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new DamperScreenHandler(syncId, playerInventory, this);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, DamperEntity entity) {
        boolean dirty = false;

        if (world.isClientSide()) {
            return;
        }

        entity.lastTickTime = world.getGameTime();

        // If we are in cooldown, decrement.
        if (entity.transferCooldown > 0) {
            --entity.transferCooldown;
            dirty = true;
        }

        // If we are enabled and out of cooldown and we have inventory, try to push it.
        if (state.getValue(DamperBlock.ENABLED) && entity.transferCooldown <= 0 && !entity.isEmpty()) {
            if (entity.push(world, pos, state, entity)) {
                entity.transferCooldown = DamperEntity.defaultCooldown;
                dirty = true;
            }
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    // Fabric transfer API implementation; uses coordinated cooldown notification.
    private boolean push(Level world, BlockPos pos, BlockState state, DamperEntity entity) {
        Direction facing = state.getValue(DamperBlock.FACING);
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

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        Direction facing = this.getBlockState().getValue(DamperBlock.FACING);

        // insertion only via duct ends
        if (direction == facing || direction == facing.getOpposite()) {
            return this.canPlaceItem(slot, stack);
        } else {
            return false;
        }
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        Direction facing = this.getBlockState().getValue(DamperBlock.FACING);
        boolean enabled = this.getBlockState().getValue(DamperBlock.ENABLED);

        // extraction via duct ends or when disabled via sides
        if (direction == facing || direction == facing.getOpposite()) {
            return true;
        } else {
            return !enabled;
        }
    }
}