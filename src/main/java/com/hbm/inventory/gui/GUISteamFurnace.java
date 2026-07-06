package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerSteamFurnace;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.gui.element.GUIElements.Gauge;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.steam.TileEntitySteamFurnace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUISteamFurnace extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_steam_furnace.png");
	private final TileEntitySteamFurnace furnace;

	public GUISteamFurnace(InventoryPlayer invPlayer, TileEntitySteamFurnace te) {
		super(new ContainerSteamFurnace(invPlayer, te));
		this.furnace = te;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		furnace.steam.renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 16, 16, 51);
		furnace.spentSteam.renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 16, 16, 51);
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 146, guiTop + 33, 18, 18, mouseX, mouseY, new String[] { "Temperature: " + furnace.temperature + " / " + furnace.maxTemperature + "°C", "Flow: " + furnace.steamConsumedLastTick + " mB/t" });
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.furnace.hasCustomInventoryName() ? this.furnace.getInventoryName() : I18n.format(this.furnace.getInventoryName());
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(this.furnace.temperature > 20) {
			int b = (this.furnace.temperature - 20) * 13 / (this.furnace.maxTemperature - 20);
			if(b > 13) b = 13;
			this.drawTexturedModalRect(guiLeft + 57, guiTop + 36 + 12 - b, 176, 12 - b, 13, b + 1);
		}

		if(this.furnace.progress > 0) {
			int p = this.furnace.progress * 23 / this.furnace.maxProgress;
			this.drawTexturedModalRect(guiLeft + 79, guiTop + 35, 176, 14, p + 1, 16);
		}

		furnace.steam.renderTank(guiLeft + 8, guiTop + 68, this.zLevel, 16, 51);
		furnace.spentSteam.renderTank(guiLeft + 26, guiTop + 68, this.zLevel, 16, 51);

		float fill = (float)(furnace.temperature - 20) / (float)(furnace.maxTemperature - 20);
		GUIElements.renderGauge(Gauge.ROUND_SMALL, guiLeft + 146, guiTop + 33, this.zLevel, fill);
	}
}
