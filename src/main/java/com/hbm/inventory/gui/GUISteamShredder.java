package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerSteamShredder;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.gui.element.GUIElements.Gauge;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.steam.TileEntitySteamShredder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUISteamShredder extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_steam_shredder.png");
	private final TileEntitySteamShredder shredder;

	public GUISteamShredder(InventoryPlayer invPlayer, TileEntitySteamShredder te) {
		super(new ContainerSteamShredder(invPlayer, te));
		this.shredder = te;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		shredder.steam.renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 16, 16, 51);
		shredder.spentSteam.renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 16, 16, 51);
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 135, guiTop + 30, 18, 18, mouseX, mouseY, new String[] { "Speed: " + shredder.speed + " / " + shredder.maxSpeed + " RPM", "Flow: " + shredder.steamConsumedLastTick + " mB/t" });
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.shredder.hasCustomInventoryName() ? this.shredder.getInventoryName() : I18n.format(this.shredder.getInventoryName());
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int prog = (int)((double)shredder.progress * 26D / (double)shredder.maxProgress);
		drawTexturedModalRect(guiLeft + 66, guiTop + 36, 176, 0, 61, prog);

		shredder.steam.renderTank(guiLeft + 8, guiTop + 68, this.zLevel, 16, 51);
		shredder.spentSteam.renderTank(guiLeft + 26, guiTop + 68, this.zLevel, 16, 51);

		float fill = (float)shredder.speed / (float)shredder.maxSpeed;
		GUIElements.renderGauge(Gauge.ROUND_SMALL, guiLeft + 135, guiTop + 30, this.zLevel, fill);
	}
}
