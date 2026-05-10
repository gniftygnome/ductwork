package net.gnomecraft.ductwork.damper;

import net.gnomecraft.ductwork.Ductwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DamperScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Ductwork.MOD_ID, "textures/gui/container/damper_screen.png");

    public DamperScreen(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133);

        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void init() {
        super.init();

        // Center the title
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }
}