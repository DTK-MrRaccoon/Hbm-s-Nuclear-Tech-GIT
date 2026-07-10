package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerSteamHammer;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.gui.element.GUIElements.Gauge;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.steam.TileEntitySteamHammer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUISteamHammer extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_steam_hammer.png");
	private static final ResourceLocation bronzeTexture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_steam_hammer_bronze.png");
	private final TileEntitySteamHammer hammer;

	public GUISteamHammer(InventoryPlayer invPlayer, TileEntitySteamHammer te) {
		super(new ContainerSteamHammer(invPlayer, te));
		this.hammer = te;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		hammer.steam.renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 17, 16, 49);
		hammer.spentSteam.renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 16, 16, 51);
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 146, guiTop + 33, 18, 18, mouseX, mouseY, new String[] { "Pressure: " + hammer.pressure + " / " + hammer.maxPressure + " Pa", "Steam: " + hammer.steamConsumedLastTick + " mB/t" });
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.hammer.hasCustomInventoryName() ? this.hammer.getInventoryName() : I18n.format(this.hammer.getInventoryName());
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(this.hammer.isBronze() ? bronzeTexture : texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int prog = (int)((double)hammer.progress * 22D / (double)hammer.maxProgress);
		drawTexturedModalRect(guiLeft + 74, guiTop + 38, 176, 0, 61, prog);

		hammer.steam.renderTank(guiLeft + 8, guiTop + 68, this.zLevel, 16, 51);
		hammer.spentSteam.renderTank(guiLeft + 26, guiTop + 68, this.zLevel, 16, 51);

		float fill = (float)hammer.pressure / (float)Math.max(1, hammer.maxPressure);
		GUIElements.renderGauge(Gauge.ROUND_SMALL, guiLeft + 146, guiTop + 33, this.zLevel, fill);
	}
}
