package net.gnomecraft.ductwork.fabricresourcecondition;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class ResourceConditionsImpl {
    protected static ConditionJsonProvider configBooleans(Identifier id, String... configBooleans) {
        Preconditions.checkArgument(configBooleans.length > 0, "Must register at least one config boolean.");

        return new ConditionJsonProvider() {
            @Override
            public Identifier getConditionId() {
                return id;
            }

            @Override
            public void writeParameters(JsonObject object) {
                JsonArray array = new JsonArray();

                for (String fieldName : configBooleans) {
                    array.add(fieldName);
                }

                object.add("values", array);
            }
        };
    }

    protected static boolean configBooleanEnabled(JsonObject object, boolean and) {
        JsonArray array = JsonHelper.getArray(object, "values");

        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                if (getConfigBooleanByName(Ductwork.getConfig(), element.getAsString()) != and) {
                    return !and;
                }
            } else {
                throw new JsonParseException("Invalid config field entry: " + element);
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
