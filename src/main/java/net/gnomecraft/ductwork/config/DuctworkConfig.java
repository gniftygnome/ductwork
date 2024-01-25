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

    @Comment("Cooldown for item transfer (8 = 4 redstone ticks, just like vanilla)")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public int itemTransferCooldown = 8;

    @Comment("Max number of itemstacks that can be moved simultaneously for duct")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public int maxItemStackDuct = 1;

    @Comment("Max number of itemstacks that can be moved simultaneously for collector")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public int maxItemStackCollector = 1;

    @Comment("Max number of itemstacks that can be moved simultaneously for damper")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.Gui.Tooltip
    public int maxItemStackDamper = 1;


}
