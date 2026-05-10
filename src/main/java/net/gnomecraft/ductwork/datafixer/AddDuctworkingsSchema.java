package net.gnomecraft.ductwork.datafixer;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

import java.util.Map;
import java.util.function.Supplier;

public class AddDuctworkingsSchema extends NamespacedSchema {
    public AddDuctworkingsSchema(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
        Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);

        schema.register(map, "ductwork:collector", () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema))));
        schema.register(map, "ductwork:damper", () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema))));
        schema.register(map, "ductwork:duct", () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema))));

        return map;
    }
}
