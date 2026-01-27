package com.hbm.inventory.gui;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerTransporterRocket;
import com.hbm.lib.RefStrings;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityTransporterRocket;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class GUITransporterRocket extends GuiInfoContainer {

	protected static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/machine/gui_transporter.png");
	private GuiTextField transporterName;
	private TileEntityTransporterRocket transporter;

	public GUITransporterRocket(InventoryPlayer invPlayer, TileEntityTransporterRocket transporter) {
		super(new ContainerTransporterRocket(invPlayer, transporter));
		this.transporter = transporter;
		xSize = 230;
		ySize = 236;
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);
		transporterName = new GuiTextField(this.fontRendererObj, guiLeft + 8, guiTop + 12, 122, 12);
		transporterName.setTextColor(0x00ff00);
		transporterName.setEnableBackgroundDrawing(false);
		transporterName.setText(transporter.getTransporterName());
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		for(int i = 0; i < 4; i++) {
			transporter.tanks[i].renderTankInfo(this, mouseX, mouseY, guiLeft + 8 + i * 18, guiTop + 70, 16, 34);
			transporter.tanks[i + 4].renderTankInfo(this, mouseX, mouseY, guiLeft + 98 + i * 18, guiTop + 70, 16, 34);
		}

		for(int i = 0; i < 2; i++) {
			transporter.tanks[i + 8].renderTankInfo(this, mouseX, mouseY, guiLeft + 188 + i * 18, guiTop + 18, 16, 70);
		}

		int mass = transporter.getTotalMass();
		int cost = transporter.getSendCost();

		this.drawCustomInfo(this, mouseX, mouseY, guiLeft - 16, guiTop + 16, 16, 16, new String[] {
				"§6Flight Statistics",
				"  Mass: §f" + mass,
				"  Cost: §b" + cost + "mB"
		});

		String[] efficiencyLines = I18nUtil.resolveKeyArray("desc.gui.rocket.efficiency");
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 32, 16, 16, guiLeft - 8, guiTop + 48, efficiencyLines);

		int warningY = guiTop + 48;

		if(transporter.tanks[8].getFill() < cost || transporter.tanks[9].getFill() < cost) {
			String[] warnFuel = I18nUtil.resolveKeyArray("desc.gui.rocket.warn_fuel");
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, warningY, 16, 16, guiLeft - 8, warningY + 16, warnFuel);
			warningY += 16;
		}

		if(transporter.getLinkedTransporter() == null) {
			String[] warnLink = I18nUtil.resolveKeyArray("desc.gui.rocket.warn_link");
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, warningY, 16, 16, guiLeft - 8, warningY + 16, warnLink);
			warningY += 16;
		}

		if(mass < transporter.getThreshold()) {
			String[] warnMass = I18nUtil.resolveKeyArray("desc.gui.rocket.warn_mass");
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, warningY, 16, 16, guiLeft - 8, warningY + 16, warnMass);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1F, 1F, 1F, 1F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		drawTexturedModalRect(guiLeft + 100 + (int)(transporter.threshold * 6.78), guiTop + 122, xSize, 0, 4, 15);

		int threshold = transporter.getThreshold();
		fontRendererObj.drawStringWithShadow("x" + threshold, guiLeft + 167 - fontRendererObj.getStringWidth("x" + threshold), guiTop + 12, -1);

		for(int i = 0; i < 4; i++) {
			transporter.tanks[i].renderTank(guiLeft + 8 + i * 18, guiTop + 104, zLevel, 16, 34);
			transporter.tanks[i + 4].renderTank(guiLeft + 98 + i * 18, guiTop + 104, zLevel, 16, 34);
		}

		for(int i = 0; i < 2; i++) {
			transporter.tanks[i + 8].renderTank(guiLeft + 188 + i * 18, guiTop + 88, zLevel, 16, 70);
		}

		this.drawInfoPanel(guiLeft - 16, guiTop + 16, 16, 16, 11);
		this.drawInfoPanel(guiLeft - 16, guiTop + 32, 16, 16, 10);

		int warningY = guiTop + 48;
		int cost = transporter.getSendCost();

		if(transporter.tanks[8].getFill() < cost || transporter.tanks[9].getFill() < cost) {
			this.drawInfoPanel(guiLeft - 16, warningY, 16, 16, 6);
			warningY += 16;
		}

		if(transporter.getLinkedTransporter() == null) {
			this.drawInfoPanel(guiLeft - 16, warningY, 16, 16, 6);
			warningY += 16;
		}

		if(transporter.getTotalMass() < transporter.getThreshold()) {
			this.drawInfoPanel(guiLeft - 16, warningY, 16, 16, 6);
		}

		transporterName.drawTextBox();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		transporterName.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int lastButtonClicked, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, lastButtonClicked, timeSinceLastClick);
		if(isInAABB(mouseX, mouseY, guiLeft + 98, guiTop + 120, 74, 20)) {
			int slidPos = MathHelper.clamp_int((int)((mouseX - (guiLeft + 98)) / 6.78), 0, 10);
			NBTTagCompound data = new NBTTagCompound();
			data.setInteger("threshold", slidPos);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, transporter.xCoord, transporter.yCoord, transporter.zCoord));
		}
	}

	@Override
	protected void keyTyped(char charIn, int key) {
		if(transporterName.textboxKeyTyped(charIn, key)) {
			transporter.setTransporterName(transporterName.getText());
		} else {
			super.keyTyped(charIn, key);
		}
		if(key == 1) mc.thePlayer.closeScreen();
	}

	private boolean isInAABB(int mx, int my, int x, int y, int w, int h) {
		return x <= mx && x + w > mx && y <= my && y + h > my;
	}
}
