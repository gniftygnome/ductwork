package net.gnomecraft.ductwork.fabricresourcecondition;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.util.Identifier;

public class DuctworkResourceConditions {
    private static final Identifier ALL_CONFIG_BOOLEANS_ENABLED = new Identifier(Ductwork.MOD_ID, "all_config_booleans_enabled");
    private static final Identifier ANY_CONFIG_BOOLEANS_ENABLED = new Identifier(Ductwork.MOD_ID, "any_config_booleans_enabled");

    /**
     * Create a condition that returns true if all of the passed config booleans are enabled.
     */
    public static ConditionJsonProvider allConfigBooleansEnabled(String... configBooleans) {
        return ResourceConditionsImpl.configBooleans(ALL_CONFIG_BOOLEANS_ENABLED, configBooleans);
    }

    /**
     * Create a condition that returns true if at least one of the passed config booleans is enabled.
     */
    public static ConditionJsonProvider anyConfigBooleansEnabled(String... configBooleans) {
        return ResourceConditionsImpl.configBooleans(ANY_CONFIG_BOOLEANS_ENABLED, configBooleans);
    }

    public static void init() {
    }

    static {
        ResourceConditions.register(ALL_CONFIG_BOOLEANS_ENABLED, object -> ResourceConditionsImpl.configBooleanEnabled(object, true));
        ResourceConditions.register(ANY_CONFIG_BOOLEANS_ENABLED, object -> ResourceConditionsImpl.configBooleanEnabled(object, false));
    }
}
