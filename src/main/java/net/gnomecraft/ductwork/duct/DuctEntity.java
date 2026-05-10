package net.gnomecraft.ductwork.duct;

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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DuctEntity extends DuctworkBlockEntity {
    public DuctEntity(BlockPos pos, BlockState state) {
        super(Ductwork.DUCT_ENTITY, pos, state, 1);

        this.transferCooldown = 0;
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new DuctScreenHandler(syncId, playerInventory, this);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, DuctEntity entity) {
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

        // If we are out of cooldown and we have inventory, try to push it.
        if (entity.transferCooldown <= 0 && !entity.isEmpty()) {
            if (entity.push(world, pos, state, entity)) {
                entity.transferCooldown = DuctEntity.defaultCooldown;
                dirty = true;
            }
        }

        if (dirty) {
            entity.setChanged();
        }
    }

    // Fabric transfer API implementation; uses coordinated cooldown notification.
    private boolean push(Level world, BlockPos pos, BlockState state, DuctEntity entity) {
        Direction facing = state.getValue(DuctBlock.FACING);
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
}