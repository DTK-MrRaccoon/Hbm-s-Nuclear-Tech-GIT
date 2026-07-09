package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerSteamBoiler;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.gui.element.GUIElements.Gauge;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.steam.TileEntitySteamBoiler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUISteamBoiler extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_steam_boiler_small.png");
	private static final ResourceLocation bronzeTexture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_steam_boiler_small_bronze.png");
	private final TileEntitySteamBoiler boiler;

	public GUISteamBoiler(InventoryPlayer invPlayer, TileEntitySteamBoiler te) {
		super(new ContainerSteamBoiler(invPlayer, te));
		this.boiler = te;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		boiler.water.renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 17, 16, 52);
		boiler.steam.renderTankInfo(this, mouseX, mouseY, guiLeft + 98, guiTop + 17, 16, 52);
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 133, guiTop + 16, 18, 18, mouseX, mouseY, new String[] { "Fuel: " + boiler.burnTime + " / " + boiler.maxBurnTime, "Heat: " + boiler.heat + " / 500", "Water: " + boiler.water.getFill() + " / " + boiler.water.getMaxFill() + " mB", "Steam: " + boiler.steam.getFill() + " / " + boiler.steam.getMaxFill() + " mB"});
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		String name = this.boiler.hasCustomInventoryName() ? this.boiler.getInventoryName() : I18n.format(this.boiler.getInventoryName());
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 0xffffff);
		this.fontRendererObj.drawString(I18n.format("container.inventory", new Object[0]), 8, this.ySize - 96 + 2, 0xffffff);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(this.boiler.isBronze() ? bronzeTexture : texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, this.xSize, this.ySize);

		if(this.boiler.burnTime > 0 && this.boiler.maxBurnTime > 0) {
			int burn = this.boiler.burnTime * 13 / this.boiler.maxBurnTime;
			this.drawTexturedModalRect(guiLeft + 62, guiTop + 37 + 12 - burn, 176, 12 - burn, 14, burn + 1);
		}

		if(this.boiler.heat > 0) {
			int heat = this.boiler.heat * 15 / 500;
			this.drawTexturedModalRect(guiLeft + 67, guiTop + 17 + 15 - heat, 194, 15 - heat, 14, heat + 1);
		}

		GUIElements.renderGauge(Gauge.ROUND_SMALL, guiLeft + 133, guiTop + 16, this.zLevel, this.boiler.water.getFill() / 1000.0D);

		boiler.water.renderTank(guiLeft + 26, guiTop + 69, this.zLevel, 16, 52);
		boiler.steam.renderTank(guiLeft + 98, guiTop + 69, this.zLevel, 16, 52);
	}
}
