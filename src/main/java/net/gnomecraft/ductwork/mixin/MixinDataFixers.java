package net.gnomecraft.ductwork.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import net.gnomecraft.ductwork.datafixer.AddDuctworkingsSchema;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.fixes.AddNewChoices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.function.BiFunction;

@Mixin(DataFixers.class)
public class MixinDataFixers {
    /*
     * This fix adds the Ductwork block entities to the DFU as of the first public release (just before 1.18),
     * allowing items stored in ductworkings to be updated by the DFU.
     */
    @WrapOperation(method = "addFixers",
            slice = @Slice(
                    from = @At(value = "NEW", target = "net/minecraft/util/datafix/fixes/WorldGenSettingsDisallowOldCustomWorldsFix", ordinal = 1),
                    to = @At(value = "NEW", target = "net/minecraft/util/datafix/fixes/StructureSettingsFlattenFix", ordinal = 0)
            ),
            at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/DataFixerBuilder;addSchema(ILjava/util/function/BiFunction;)Lcom/mojang/datafixers/schemas/Schema;", ordinal = 0)
    )
    @SuppressWarnings("unused")
    private static Schema ductwork$injectAddDuctworkingsFix(DataFixerBuilder builder, int version, BiFunction<Integer, Schema, Schema> factory, Operation<Schema> original) {
        Schema addDuctworkingsSchema = builder.addSchema(2859, AddDuctworkingsSchema::new);
        builder.addFixer(new AddNewChoices(addDuctworkingsSchema, "Added Ductwork Collector", References.BLOCK_ENTITY));
        builder.addFixer(new AddNewChoices(addDuctworkingsSchema, "Added Ductwork Damper", References.BLOCK_ENTITY));
        builder.addFixer(new AddNewChoices(addDuctworkingsSchema, "Added Ductwork Duct", References.BLOCK_ENTITY));

        return original.call(builder, version, factory);
    }
}
