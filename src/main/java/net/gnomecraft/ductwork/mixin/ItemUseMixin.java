package net.gnomecraft.ductwork.mixin;

import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerInteractionManager.class)
public class ItemUseMixin {
    // We move evaluation of the block interaction preconditions for ItemStack.onUse() into our block code.
    // That allows us to respond to events which would normally be cancelled due to sneaking ... if we want.
    // Fortunately, being player-driven, this is not a particularly performance-sensitive server code path.
    @Inject(method = "interactBlock",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;shouldCancelInteraction()Z",
                    ordinal = 0
            ),
            cancellable = true,
            locals = LocalCapture.NO_CAPTURE
    )
    public void sneakAndUseOnDuctworkings(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.isIn(Ductwork.DUCT_BLOCKS) && hand.equals(player.preferredHand)) {
            ActionResult onUseResult;
            if ((onUseResult = state.onUse(world, player, hitResult)).isAccepted()) {
                Criteria.ITEM_USED_ON_BLOCK.trigger(player, pos, stack.copy());
                cir.setReturnValue(onUseResult);
            }
        }
    }
}