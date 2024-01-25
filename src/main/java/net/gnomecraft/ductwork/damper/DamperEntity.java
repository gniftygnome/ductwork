package net.gnomecraft.ductwork.damper;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.gnomecraft.cooldowncoordinator.CooldownCoordinator;
import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.base.DuctworkBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@SuppressWarnings("UnstableApiUsage")
public class DamperEntity extends DuctworkBlockEntity implements SidedInventory {
    public DamperEntity(BlockPos pos, BlockState state) {
        super(Ductwork.DAMPER_ENTITY, pos, state);

        this.inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
        this.transferCooldown = 0;
    }

    @Override
    public ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new DamperScreenHandler(syncId, playerInventory, this);
    }

    public static void tick(World world, BlockPos pos, BlockState state, DamperEntity entity) {
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

        // If we are enabled and out of cooldown and we have inventory, try to push it.
        if (state.get(DamperBlock.ENABLED) && entity.transferCooldown <= 0 && !entity.isEmpty()) {
            if (entity.push(world, pos, state, entity)) {
                entity.transferCooldown = DamperEntity.defaultCooldown;
                dirty = true;
            }
        }

        if (dirty) {
            entity.markDirty();
        }
    }

    // Fabric transfer API implementation; uses coordinated cooldown notification.
    private boolean push(World world, BlockPos pos, BlockState state, DamperEntity entity) {
        Direction facing = state.get(DamperBlock.FACING);
        BlockEntity targetEntity = world.getBlockEntity(pos.offset(facing));
        Storage<ItemVariant> sourceStorage = ItemStorage.SIDED.find(world, pos, state, entity, facing);
        Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(world, pos.offset(facing), facing.getOpposite());

        if (sourceStorage != null && targetStorage != null) {
            boolean targetEmpty = CooldownCoordinator.isStorageEmpty(targetStorage);

            if (StorageUtil.move(sourceStorage, targetStorage, variant -> true, Ductwork.getConfig().maxItemStackDamper, null) > 0) {
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
    public int[] getAvailableSlots(Direction side) {
        return new int[] {0};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction direction) {
        Direction facing = this.getCachedState().get(DamperBlock.FACING);

        // insertion only via duct ends
        if (facing != null && (direction == facing || direction == facing.getOpposite())) {
            return this.isValid(slot, stack);
        } else {
            return false;
        }
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction direction) {
        Direction facing = this.getCachedState().get(DamperBlock.FACING);
        boolean enabled = this.getCachedState().get(DamperBlock.ENABLED);

        // extraction via duct ends or when disabled via sides
        if (direction == facing || direction == facing.getOpposite()) {
            return true;
        } else {
            return !enabled;
        }
    }
}