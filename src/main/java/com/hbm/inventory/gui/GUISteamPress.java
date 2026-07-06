package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerSteamPress;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.gui.element.GUIElements.Gauge;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.steam.TileEntitySteamPress;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUISteamPress extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_steam_press.png");
	private final TileEntitySteamPress press;

	public GUISteamPress(InventoryPlayer invPlayer, TileEntitySteamPress te) {
		super(new ContainerSteamPress(invPlayer, te));
		this.press = te;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		press.steam.renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 17, 16, 49);
		press.spentSteam.renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 16, 16, 51);
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 146, guiTop + 33, 18, 18, mouseX, mouseY, new String[] { "Pressure: " + press.pressure + " / " + press.maxPressure + " Pa", "Flow: " + press.steamConsumedLastTick + " mB/t" });
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.press.hasCustomInventoryName() ? this.press.getInventoryName() : I18n.format(this.press.getInventoryName());
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int prog = (int)((double)press.progress * 23D / (double)press.maxProgress);
		drawTexturedModalRect(guiLeft + 79, guiTop + 35, 176, 14, prog, 16);

		press.steam.renderTank(guiLeft + 8, guiTop + 68, this.zLevel, 16, 51);
		press.spentSteam.renderTank(guiLeft + 26, guiTop + 68, this.zLevel, 16, 51);

		float fill = (float)press.pressure / (float)press.maxPressure;
		GUIElements.renderGauge(Gauge.ROUND_SMALL, guiLeft + 146, guiTop + 33, this.zLevel, fill);
	}
}
