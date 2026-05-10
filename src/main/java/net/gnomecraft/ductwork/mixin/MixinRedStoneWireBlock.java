package net.gnomecraft.ductwork.mixin;

import net.gnomecraft.ductwork.Ductwork;
import net.gnomecraft.ductwork.collector.CollectorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedStoneWireBlock.class)
public class MixinRedStoneWireBlock {
    @Inject(method = "canSurviveOn", at = @At("HEAD"), cancellable = true)
    private void allowWireOnDownwardCollectors(BlockGetter world, BlockPos pos, BlockState floor, CallbackInfoReturnable<Boolean> ci) {
        if (floor.is(Ductwork.COLLECTOR_BLOCK) && floor.getValue(CollectorBlock.INTAKE) == Direction.UP) {
            ci.setReturnValue(true);
        }
    }
}
