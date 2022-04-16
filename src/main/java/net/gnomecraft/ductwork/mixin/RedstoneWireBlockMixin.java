package net.gnomecraft.ductwork.mixin;

import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.collector.CollectorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin {
    @Inject(method = "canRunOnTop", at = @At("HEAD"), cancellable = true)
    private void allowWireOnDownwardCollectors(BlockView world, BlockPos pos, BlockState floor, CallbackInfoReturnable<Boolean> ci) {
        if (floor.isOf(Ductwork.COLLECTOR_BLOCK) && floor.get(CollectorBlock.FACING) == Direction.DOWN) {
            ci.setReturnValue(true);
        }
    }
}
