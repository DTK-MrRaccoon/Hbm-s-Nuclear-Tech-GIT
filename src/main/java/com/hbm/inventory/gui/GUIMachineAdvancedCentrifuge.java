package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerMachineAdvancedCentrifuge;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineAdvancedCentrifuge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIMachineAdvancedCentrifuge extends GuiInfoContainer {

    public static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_advanced_centrifuge.png");
    private TileEntityMachineAdvancedCentrifuge centrifuge;
    
    public GUIMachineAdvancedCentrifuge(InventoryPlayer invPlayer, TileEntityMachineAdvancedCentrifuge tedf) {
        super(new ContainerMachineAdvancedCentrifuge(invPlayer, tedf));
        centrifuge = tedf;
        
        this.xSize = 176;
        this.ySize = 243;
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 9, guiTop + 13, 16, 34, centrifuge.power, centrifuge.maxPower);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int j) {
        this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, 152, 4210752);
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        
        if(centrifuge.hasPower()) {
            int i1 = centrifuge.getPowerRemainingScaled(34);
            drawTexturedModalRect(guiLeft + 9, guiTop + 47 - i1, 176, 34 - i1, 16, i1);
        }

        if(centrifuge.isProgressing) {
            int p = centrifuge.getProgressScaled(145);
            
            for(int i = 0; i < 4; i++) {
                int h = Math.min(p, 36);
                drawTexturedModalRect(guiLeft + 65 + i * 20, guiTop + 14 + 36 - h, 176, 71 - h, 12, h);
                p -= h;
                if(p <= 0)
                    break;
            }
        }
    }
}
