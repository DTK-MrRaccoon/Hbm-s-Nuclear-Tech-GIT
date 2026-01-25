package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerMachineMixingVat;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMixingVatTemplate;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineMixingVat;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIMachineMixingVat extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_mixing_vat.png");
	private TileEntityMachineMixingVat mixingVat;
	
	public GUIMachineMixingVat(InventoryPlayer invPlayer, TileEntityMachineMixingVat tedf) {
		super(new ContainerMachineMixingVat(invPlayer, tedf));
		mixingVat = tedf;
		
		this.xSize = 176;
		this.ySize = 222;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		mixingVat.tanks[0].renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 52 - 34, 16, 34);
		mixingVat.tanks[1].renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 52 - 34, 16, 34);
		mixingVat.tanks[2].renderTankInfo(this, mouseX, mouseY, guiLeft + 134, guiTop + 52 - 34, 16, 34);
		mixingVat.tanks[3].renderTankInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 52 - 34, 16, 34);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 44, guiTop + 70 - 52, 16, 52, mixingVat.power, mixingVat.maxPower);
		
		if(mixingVat.getStackInSlot(4) == null || !(mixingVat.getStackInSlot(4).getItem() instanceof ItemMixingVatTemplate)) {
			String[] warningText = I18nUtil.resolveKeyArray("desc.gui.mixingvat.warning");
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 36, 16, 16, guiLeft - 8, guiTop + 36 + 16, warningText);
		}
		
		String[] templateText = I18nUtil.resolveKeyArray("desc.gui.template");
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 16, 16, 16, guiLeft - 8, guiTop + 16 + 16, templateText);
		
		String[] upgradeText = new String[3];
		upgradeText[0] = I18nUtil.resolveKey("desc.gui.upgrade");
		upgradeText[1] = I18nUtil.resolveKey("desc.gui.upgrade.speed");
		upgradeText[2] = I18nUtil.resolveKey("desc.gui.upgrade.power");
		
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 105, guiTop + 40, 8, 8, guiLeft + 105, guiTop + 40 + 16, upgradeText);
	}

	@Override
	protected void drawGuiContainerForegroundLayer( int i, int j) {
		String name = this.mixingVat.hasCustomInventoryName() ? this.mixingVat.getInventoryName() : I18n.format(this.mixingVat.getInventoryName());
		
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		
		int i = (int) (mixingVat.power * 52 / mixingVat.maxPower);
		drawTexturedModalRect(guiLeft + 44, guiTop + 70 - i, 176, 52 - i, 16, i);

		int j = mixingVat.progress * 90 / mixingVat.maxProgress;
		drawTexturedModalRect(guiLeft + 43, guiTop + 89, 0, 222, j, 18);

		this.drawInfoPanel(guiLeft + 105, guiTop + 40, 8, 8, 8);
		
		if(mixingVat.getStackInSlot(4) == null || !(mixingVat.getStackInSlot(4).getItem() instanceof ItemMixingVatTemplate)) {
			this.drawInfoPanel(guiLeft - 16, guiTop + 36, 16, 16, 6);
		}
		
		this.drawInfoPanel(guiLeft - 16, guiTop + 16, 16, 16, 11);
		
		mixingVat.tanks[0].renderTank(guiLeft + 8, guiTop + 52, this.zLevel, 16, 34);
		mixingVat.tanks[1].renderTank(guiLeft + 26, guiTop + 52, this.zLevel, 16, 34);
		mixingVat.tanks[2].renderTank(guiLeft + 134, guiTop + 52, this.zLevel, 16, 34);
		mixingVat.tanks[3].renderTank(guiLeft + 152, guiTop + 52, this.zLevel, 16, 34);
	}
}