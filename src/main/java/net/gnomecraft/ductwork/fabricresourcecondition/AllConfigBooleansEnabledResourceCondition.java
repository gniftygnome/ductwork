package net.gnomecraft.ductwork.fabricresourcecondition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.registry.RegistryOps;
import org.jetbrains.annotations.Nullable;

import java.util.List;

record AllConfigBooleansEnabledResourceCondition(List<String> configBooleans) implements ResourceCondition {
    public static final MapCodec<AllConfigBooleansEnabledResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.listOf().fieldOf("values").forGetter(AllConfigBooleansEnabledResourceCondition::configBooleans)
	).apply(instance, AllConfigBooleansEnabledResourceCondition::new));

	@Override
	public ResourceConditionType<?> getType() {
		return DuctworkResourceConditionTypes.ALL_CONFIG_BOOLEANS_ENABLED;
	}

	@Override
	public boolean test(@Nullable RegistryOps.RegistryInfoGetter registryInfo) {
		return DuctworkResourceConditions.configBooleansEnabled(this.configBooleans, true);
	}
}
