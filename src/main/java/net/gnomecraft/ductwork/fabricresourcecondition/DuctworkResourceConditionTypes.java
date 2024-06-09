package net.gnomecraft.ductwork.fabricresourcecondition;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.util.Identifier;

public final class DuctworkResourceConditionTypes {
    public static final ResourceConditionType<AllConfigBooleansEnabledResourceCondition> ALL_CONFIG_BOOLEANS_ENABLED = createResourceConditionType("all_config_booleans_enabled", AllConfigBooleansEnabledResourceCondition.CODEC);
    public static final ResourceConditionType<AnyConfigBooleansEnabledResourceCondition> ANY_CONFIG_BOOLEANS_ENABLED = createResourceConditionType("any_config_booleans_enabled", AnyConfigBooleansEnabledResourceCondition.CODEC);

    private DuctworkResourceConditionTypes() {
    }

    private static <T extends ResourceCondition> ResourceConditionType<T> createResourceConditionType(String name, MapCodec<T> codec) {
        return ResourceConditionType.create(Identifier.of(Ductwork.MOD_ID, name), codec);
    }
}
