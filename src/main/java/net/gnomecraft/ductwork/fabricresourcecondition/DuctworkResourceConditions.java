package net.gnomecraft.ductwork.fabricresourcecondition;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.gnomecraft.ductwork.Ductwork;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public final class DuctworkResourceConditions {
    private DuctworkResourceConditions() {
    }

    /**
     * Create a condition that returns true if all of the passed config booleans are enabled.
     */
    public static ResourceCondition allConfigBooleansEnabled(String... configBooleans) {
        return new AllConfigBooleansEnabledResourceCondition(List.of(configBooleans));
    }

    /**
     * Create a condition that returns true if at least one of the passed config booleans is enabled.
     */
    public static ResourceCondition anyConfigBooleansEnabled(String... configBooleans) {
        return new AnyConfigBooleansEnabledResourceCondition(List.of(configBooleans));
    }

    public static void init() {
        ResourceConditions.register(DuctworkResourceConditionTypes.ALL_CONFIG_BOOLEANS_ENABLED);
        ResourceConditions.register(DuctworkResourceConditionTypes.ANY_CONFIG_BOOLEANS_ENABLED);
    }

    static boolean configBooleansEnabled(List<String> configBooleans, boolean and) {

        for (String configBoolean : configBooleans) {
            if (getConfigBooleanByName(Ductwork.getConfig(), configBoolean) != and) {
                return !and;
            }
        }

        return and;
    }

    @Nullable
    private static Object getConfigFieldByName(@NotNull Object config, @NotNull String name) {
        try {
            Field field = config.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(config);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean getConfigBooleanByName(@NotNull Object config, @NotNull String name) {
        Object value = getConfigFieldByName(config, name);
        if (value instanceof Boolean) {
            return (boolean) value;
        } else {
            Ductwork.LOGGER.warn("ResourceCondition requested invalid boolean config option: {}", name);
            return false;
        }
    }
}
