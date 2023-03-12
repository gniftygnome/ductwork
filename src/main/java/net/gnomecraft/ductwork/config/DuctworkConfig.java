package net.gnomecraft.ductwork.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.gnomecraft.ductwork.Ductwork;

// Configuration file definition.
@SuppressWarnings("unused")
@Config(name = Ductwork.MOD_ID)
public class DuctworkConfig implements ConfigData {
    @Comment("Limit Ductworkings to Vanilla parity feature set?")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean vanilla = false;

    @Comment("Place Ductworkings on Ductwork blocks instead of opening inventory?")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean placement = false;

    @Comment("Use low-iron recipes for Ductwork blocks?")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean cheaper = false;
}
