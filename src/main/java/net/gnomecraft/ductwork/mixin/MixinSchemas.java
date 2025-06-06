package net.gnomecraft.ductwork.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import net.gnomecraft.ductwork.datafixer.AddDuctworkingsSchema;
import net.minecraft.datafixer.Schemas;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.fix.ChoiceTypesFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.function.BiFunction;

@Mixin(Schemas.class)
public class MixinSchemas {
    /*
     * This fix adds the Ductwork block entities to the DFU as of the first public release (just before 1.18),
     * allowing items stored in ductworkings to be updated by the DFU.
     */
    @WrapOperation(method = "build",
            slice = @Slice(
                    from = @At(value = "NEW", target = "net/minecraft/datafixer/fix/WorldGenSettingsDisallowOldCustomWorldsFix", ordinal = 1),
                    to = @At(value = "NEW", target = "net/minecraft/datafixer/fix/StructureSettingsFlattenFix", ordinal = 0)
            ),
            at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/DataFixerBuilder;addSchema(ILjava/util/function/BiFunction;)Lcom/mojang/datafixers/schemas/Schema;", ordinal = 0)
    )
    @SuppressWarnings("unused")
    private static Schema ductwork$injectAddDuctworkingsFix(DataFixerBuilder builder, int version, BiFunction<Integer, Schema, Schema> factory, Operation<Schema> original) {
        Schema addDuctworkingsSchema = builder.addSchema(2859, AddDuctworkingsSchema::new);
        builder.addFixer(new ChoiceTypesFix(addDuctworkingsSchema, "Added Ductwork Collector", TypeReferences.BLOCK_ENTITY));
        builder.addFixer(new ChoiceTypesFix(addDuctworkingsSchema, "Added Ductwork Damper", TypeReferences.BLOCK_ENTITY));
        builder.addFixer(new ChoiceTypesFix(addDuctworkingsSchema, "Added Ductwork Duct", TypeReferences.BLOCK_ENTITY));

        return original.call(builder, version, factory);
    }
}
