package net.gnomecraft.ductwork.fabricresourcecondition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

record AnyConfigBooleansEnabledResourceCondition(List<String> configBooleans) implements ResourceCondition {
    public static final MapCodec<AnyConfigBooleansEnabledResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.listOf().fieldOf("values").forGetter(AnyConfigBooleansEnabledResourceCondition::configBooleans)
	).apply(instance, AnyConfigBooleansEnabledResourceCondition::new));

	@Override
	public ResourceConditionType<?> getType() {
		return DuctworkResourceConditionTypes.ANY_CONFIG_BOOLEANS_ENABLED;
	}

	@Override
	public boolean test(@Nullable RegistryWrapper.WrapperLookup registryLookup) {
		return DuctworkResourceConditions.configBooleansEnabled(this.configBooleans, false);
	}
}
