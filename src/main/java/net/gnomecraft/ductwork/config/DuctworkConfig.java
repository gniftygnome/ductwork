package net.gnomecraft.ductwork.config;

import blue.endless.jankson.Comment;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.gnomecraft.ductwork.Ductwork;

// Configuration file definition.
@SuppressWarnings("unused")
@Config(name = Ductwork.modId)
public class DuctworkConfig implements ConfigData {
    @Comment("Limit Ductworkings to Vanilla parity feature set?")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean vanilla = false;

    @Comment("Place Ductworkings on Ductwork blocks instead of opening inventory?")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public boolean placement = false;
}
