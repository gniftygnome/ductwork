package net.gnomecraft.ductwork.fabricresourcecondition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.RegistryOps;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public record AllConfigBooleansEnabledResourceCondition(List<String> configBooleans) implements ResourceCondition {
    public static final MapCodec<AllConfigBooleansEnabledResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.listOf().fieldOf("values").forGetter(AllConfigBooleansEnabledResourceCondition::configBooleans)
	).apply(instance, AllConfigBooleansEnabledResourceCondition::new));

	@Override
	public ResourceConditionType<?> getType() {
		return DuctworkResourceConditionTypes.ALL_CONFIG_BOOLEANS_ENABLED;
	}

	@Override
	public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
		return DuctworkResourceConditions.configBooleansEnabled(this.configBooleans, true);
	}
}
