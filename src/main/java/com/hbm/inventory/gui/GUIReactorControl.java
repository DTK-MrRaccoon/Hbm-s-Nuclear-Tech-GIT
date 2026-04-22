package com.hbm.inventory.gui;

import org.apache.commons.lang3.math.NumberUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerReactorControl;
import com.hbm.lib.RefStrings;
import com.hbm.module.NumberDisplay;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityReactorControl;
import com.hbm.tileentity.machine.TileEntityReactorControl.ReactorType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class GUIReactorControl extends GuiInfoContainer {

	private static ResourceLocation textureDefault = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_reactor_control.png");
	private static ResourceLocation textureSmall = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_reactor_control_small_reactor.png");
	private TileEntityReactorControl control;

	private final NumberDisplay[] displays = new NumberDisplay[3];
	private GuiTextField[] fields;
	private boolean fieldsInitialized = false;

	public GUIReactorControl(InventoryPlayer invPlayer, TileEntityReactorControl tedf) {
		super(new ContainerReactorControl(invPlayer, tedf));
		control = tedf;
		displays[0] = new NumberDisplay(this, 6, 20, 0x08FF00).setDigitLength(3);
		displays[1] = new NumberDisplay(this, 66, 20, 0x08FF00).setDigitLength(4);
		displays[2] = new NumberDisplay(this, 126, 20, 0x08FF00).setDigitLength(3);

		fields = new GuiTextField[4];
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);

		if(!fieldsInitialized) {
			for(int i = 0; i < 4; i++) {
				fields[i] = new GuiTextField(this.fontRendererObj, 0, 0, 26, 7);
				fields[i].setTextColor(0x08FF00);
				fields[i].setDisabledTextColour(-1);
				fields[i].setEnableBackgroundDrawing(false);
				fields[i].setMaxStringLength(i < 2 ? 3 : 4);
			}
			fieldsInitialized = true;
		}
		updateFieldPositionsAndValues();
	}

	private void updateFieldPositionsAndValues() {
		if(control.reactorType == ReactorType.RESEARCH) {
			fields[0].xPosition = guiLeft + 35;
			fields[0].yPosition = guiTop + 38;
			fields[1].xPosition = guiLeft + 35 + 30;
			fields[1].yPosition = guiTop + 38;
			fields[2].xPosition = guiLeft + 35;
			fields[2].yPosition = guiTop + 49;
			fields[3].xPosition = guiLeft + 35 + 30;
			fields[3].yPosition = guiTop + 49;

			// Only update text if the field is not currently focused to avoid overwriting user input
			if(!fields[0].isFocused()) fields[0].setText(String.valueOf((int) control.levelUpper));
			if(!fields[1].isFocused()) fields[1].setText(String.valueOf((int) control.levelLower));
			if(!fields[2].isFocused()) fields[2].setText(String.valueOf((int) control.heatUpper / 50));
			if(!fields[3].isFocused()) fields[3].setText(String.valueOf((int) control.heatLower / 50));
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		updateFieldPositionsAndValues();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		if(control.reactorType == ReactorType.SMALL) {
			this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 34, 88, 4, new String[] { "Fuel: " + control.fuel + "%" });
			this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 40, 88, 4, new String[] { "Water: " + control.water + "/" + control.maxWater + "mB" });
			this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 46, 88, 4, new String[] { "Coolant: " + control.cool + "/" + control.maxCool + "mB" });
			String s = "";
			switch(control.compression) {
				case 0: s = "Steam"; break;
				case 1: s = "Dense Steam"; break;
				case 2: s = "Super Dense Steam"; break;
			}
			this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 52, 88, 4, new String[] { s + ": " + control.steam + "/" + control.maxSteam + "mB" });
			this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 58, 88, 4, new String[] { "Hull Heat: " + Math.round((control.hullHeat) * 0.00001 * 980 + 20) + "°C" });
			this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 64, 88, 4, new String[] { "Core Heat: " + Math.round((control.coreHeat) * 0.00002 * 980 + 20) + "°C" });
			this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 7, guiTop + 16, 18, 18, new String[] { "Reactor Status: " + (control.isOn ? "ON" : "OFF") });
			this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 43, guiTop + 16, 18, 18, new String[] { "Automatic Shutdown: " + (control.auto ? "ENABLED" : "DISABLED") });
			if(!control.isLinked) this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 79, guiTop + 16, 18, 18, new String[] { "Reactor link not found!" });
			if(control.water < control.maxWater * 0.1) this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 79 + 18, guiTop + 16, 18, 18, new String[] { "Water level low!" });
			if(control.cool < control.maxCool * 0.1) this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 79 + 18 * 2, guiTop + 16, 18, 18, new String[] { "Coolant level low!" });
			if(control.steam > control.maxSteam * 0.95) this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 79 + 18 * 3, guiTop + 16, 18, 18, new String[] { "Steam buffer full!" });
			if(control.coreHeat > 85000) this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 79 + 18 * 4, guiTop + 16, 18, 18, new String[] { "CORE TEMPERATURE CRITICAL!!" });
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int i) {
		super.mouseClicked(x, y, i);

		if(control.reactorType == ReactorType.RESEARCH) {
			for(int j = 0; j < 4; j++) fields[j].mouseClicked(x, y, i);
			if(guiLeft + 33 <= x && guiLeft + 33 + 58 > x && guiTop + 59 < y && guiTop + 59 + 10 >= y) {
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				NBTTagCompound data = new NBTTagCompound();
				double[] vals = new double[] { 0D, 0D, 0D, 0D };
				for(int k = 0; k < 4; k++) {
					double clamp = k < 2 ? 100 : 1000;
					int mod = k < 2 ? 1 : 50;
					if(NumberUtils.isDigits(fields[k].getText())) {
						int j = (int) MathHelper.clamp_double(Double.parseDouble(fields[k].getText()), 0, clamp);
						fields[k].setText(j + "");
						vals[k] = j * mod;
					} else {
						fields[k].setText("0");
					}
				}
				data.setDouble("levelUpper", vals[0]);
				data.setDouble("levelLower", vals[1]);
				data.setDouble("heatUpper", vals[2]);
				data.setDouble("heatLower", vals[3]);
				PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, control.xCoord, control.yCoord, control.zCoord));
			}
			for(int k = 0; k < 3; k++) {
				if(guiLeft + 7 <= x && guiLeft + 7 + 22 > x && guiTop + 37 + k * 11 < y && guiTop + 37 + 10 + k * 11 >= y) {
					mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
					NBTTagCompound data = new NBTTagCompound();
					data.setInteger("function", k);
					PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, control.xCoord, control.yCoord, control.zCoord));
				}
			}
		} else if(control.reactorType == ReactorType.SMALL) {
			if(guiLeft + 7 <= x && guiLeft + 7 + 18 > x && guiTop + 16 < y && guiTop + 16 + 18 >= y) {
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				NBTTagCompound data = new NBTTagCompound();
				data.setBoolean("active", !control.isOn);
				PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, control.xCoord, control.yCoord, control.zCoord));
			}
			if(guiLeft + 43 <= x && guiLeft + 43 + 18 > x && guiTop + 16 < y && guiTop + 16 + 18 >= y) {
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				NBTTagCompound data = new NBTTagCompound();
				data.setBoolean("auto", !control.auto);
				PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, control.xCoord, control.yCoord, control.zCoord));
			}
			if(guiLeft + 63 <= x && guiLeft + 63 + 14 > x && guiTop + 52 < y && guiTop + 52 + 18 >= y) {
				mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
				NBTTagCompound data = new NBTTagCompound();
				data.setInteger("compression", (control.compression + 1) % 3);
				PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, control.xCoord, control.yCoord, control.zCoord));
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.control.hasCustomInventoryName() ? this.control.getInventoryName() : I18n.format(this.control.getInventoryName());
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
		if(control.reactorType == ReactorType.SMALL) {
			this.fontRendererObj.drawString("Rods: " + control.rods + "%", 8, 40, 4210752);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		ResourceLocation tex = (control.reactorType == ReactorType.SMALL) ? textureSmall : textureDefault;
		Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(control.reactorType == ReactorType.RESEARCH) {
			GL11.glPushMatrix();
			Tessellator tess = Tessellator.instance;
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glLineWidth(3F);
			tess.startDrawing(3);
			tess.setColorOpaque_I(0x08FF00);
			for(int i = 0; i < 40; i++) {
				tess.addVertex(guiLeft + 128 + i, guiTop + 39 + MathHelper.clamp_double(control.getTargetLevel(control.function, i * 1250) / 100 * 28, 0, 28), this.zLevel);
			}
			tess.draw();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glPopMatrix();
			for(byte i = 0; i < 3; i++) displays[i].drawNumber(control.getDisplayData()[i]);
			for(int i = 0; i < 4; i++) fields[i].drawTextBox();
		} else if(control.reactorType == ReactorType.SMALL) {
			if(control.fuel > 0) {
				int bar = (control.fuel * 88 / 100);
				drawTexturedModalRect(guiLeft + 80, guiTop + 35, 0, 186, bar, 4);
			}
			if(control.water > 0) {
				int bar = (control.water * 88 / control.maxWater);
				drawTexturedModalRect(guiLeft + 80, guiTop + 41, 0, 190, bar, 4);
			}
			if(control.cool > 0) {
				int bar = (control.cool * 88 / control.maxCool);
				drawTexturedModalRect(guiLeft + 80, guiTop + 47, 0, 194, bar, 4);
			}
			if(control.steam > 0) {
				int bar = (control.steam * 88 / control.maxSteam);
				drawTexturedModalRect(guiLeft + 80, guiTop + 53, 0, 174 + 4 * control.compression, bar, 4);
			}
			if(control.hullHeat > 0) {
				int bar = (control.hullHeat * 88 / 100000);
				drawTexturedModalRect(guiLeft + 80, guiTop + 59, 0, 166, bar, 4);
			}
			if(control.coreHeat > 0) {
				int bar = (control.coreHeat * 88 / 100000);
				drawTexturedModalRect(guiLeft + 80, guiTop + 65, 0, 170, bar, 4);
			}
			if(control.isOn) drawTexturedModalRect(guiLeft + 7, guiTop + 16, 218, 0, 18, 18);
			if(control.auto) drawTexturedModalRect(guiLeft + 43, guiTop + 16, 236, 0, 18, 18);
			drawTexturedModalRect(guiLeft + 63, guiTop + 52, 176 + 14 * control.compression, 0, 14, 18);
			if(!control.isLinked) drawTexturedModalRect(guiLeft + 79, guiTop + 16, 88, 166, 18, 18);
			if(control.water < control.maxWater * 0.1) drawTexturedModalRect(guiLeft + 79 + 18, guiTop + 16, 88 + 18, 166, 18, 18);
			if(control.cool < control.maxCool * 0.1) drawTexturedModalRect(guiLeft + 79 + 18 * 2, guiTop + 16, 88 + 18 * 2, 166, 18, 18);
			if(control.steam > control.maxSteam * 0.95) drawTexturedModalRect(guiLeft + 79 + 18 * 3, guiTop + 16, 88 + 18 * 3, 166, 18, 18);
			if(control.coreHeat > 85000) drawTexturedModalRect(guiLeft + 79 + 18 * 4, guiTop + 16, 88 + 18 * 4, 166, 18, 18);
			if(control.rods == control.maxRods && control.rods != 0) {
				drawTexturedModalRect(guiLeft + 25, guiTop + 16, 176, 18, 18, 18);
			} else if(control.rods > 0) {
				drawTexturedModalRect(guiLeft + 25, guiTop + 16, 194, 18, 18, 18);
			}
		}
	}

	@Override
	protected void keyTyped(char c, int i) {
		boolean fieldFocused = false;
		if(control.reactorType == ReactorType.RESEARCH) {
			for(int j = 0; j < 4; j++) {
				if(fields[j].textboxKeyTyped(c, i)) {
					fieldFocused = true;
				}
			}
		}
		if(!fieldFocused) {
			if(control.reactorType == ReactorType.SMALL) {
				if(c >= '0' && c <= '9') {
					int percent = (c - '0') * 10;
					NBTTagCompound data = new NBTTagCompound();
					data.setInteger("rods", percent);
					PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, control.xCoord, control.yCoord, control.zCoord));
					mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
					return;
				}
			}
			if(i == 1 || i == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
				this.mc.thePlayer.closeScreen();
				return;
			}
			super.keyTyped(c, i);
		}
	}

	@Override
	public void handleMouseInput() {
		super.handleMouseInput();
		if(control.reactorType == ReactorType.SMALL) {
			int wheel = Mouse.getEventDWheel();
			if(wheel != 0) {
				int delta = wheel > 0 ? 1 : -1;
				int newRods = MathHelper.clamp_int(control.rods + delta, 0, control.maxRods);
				NBTTagCompound data = new NBTTagCompound();
				data.setInteger("rods", newRods);
				PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, control.xCoord, control.yCoord, control.zCoord));
			}
		}
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}
}
