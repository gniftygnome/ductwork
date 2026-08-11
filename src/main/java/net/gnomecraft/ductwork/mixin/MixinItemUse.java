package net.gnomecraft.ductwork.mixin;

import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerGameMode.class)
public class MixinItemUse {
    // We move evaluation of the block interaction preconditions for ItemStack.onUse() into our block code.
    // That allows us to respond to events which would normally be cancelled due to sneaking ... if we want.
    // Fortunately, being player-driven, this is not a particularly performance-sensitive server code path.
    @Inject(method = "useItemOn",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z",
                    ordinal = 0
            ),
            cancellable = true,
            locals = LocalCapture.NO_CAPTURE
    )
    public void sneakAndUseOnDuctworkings(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.is(Ductwork.DUCT_BLOCKS) && hand.equals(player.swingingArm)) {
            InteractionResult onUseResult;
            if ((onUseResult = state.useWithoutItem(world, player, hitResult)).consumesAction()) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, stack.copy());
                cir.setReturnValue(onUseResult);
            }
        }
    }
}