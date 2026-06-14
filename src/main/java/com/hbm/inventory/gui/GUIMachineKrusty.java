package com.hbm.inventory.gui;

import java.util.Locale;

import org.apache.commons.lang3.math.NumberUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerMachineKrusty;
import com.hbm.lib.RefStrings;
import com.hbm.module.NumberDisplay;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityMachineKrusty;
import com.hbm.render.util.GaugeUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class GUIMachineKrusty extends GuiInfoContainer {
	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/reactors/gui_machine_krusty.png");
	private TileEntityMachineKrusty krusty;
	private final NumberDisplay[] displays = new NumberDisplay[3];
	byte timer;
	
	private GuiTextField field;
	
	public GUIMachineKrusty(InventoryPlayer invPlayer, TileEntityMachineKrusty te) {
		super(new ContainerMachineKrusty(invPlayer, te));
		krusty = te;
		this.xSize = 176;
		this.ySize = 222;
		displays[0] = new NumberDisplay(this, 14, 25, 0x08FF00).setDigitLength(4);
		displays[1] = new NumberDisplay(this, 12, 63, 0x08FF00).setDigitLength(3);
		displays[2] = new NumberDisplay(this, 5, 101, 0x08FF00).setDigitLength(3);
	}
	
	@Override
	public void initGui() {
		super.initGui();
		
		this.guiLeft = (this.width - this.xSize) / 2;
		this.guiTop = (this.height - this.ySize) / 2;
		
		Keyboard.enableRepeatEvents(true);
		
		this.field = new GuiTextField(this.fontRendererObj, guiLeft + 8, guiTop + 99, 33, 16);
		this.field.setEnableBackgroundDrawing(false);
		this.field.setMaxStringLength(3);
		
		this.field.setText(String.valueOf((int)(krusty.level * 100)));
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		
		String[] text2 = new String[] {
				"This reactor is fueled with plate fuel.",
				"The reaction needs a neutron source to start."
		};
		
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 14, guiTop + 61, 16, 16, guiLeft - 6, guiTop + 61 + 16, text2);
		
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 111, guiTop + 96, 18, 18, mouseX, mouseY, new String[] { String.format(Locale.US, "%d", krusty.tanks[0].getFill()) + " / " + String.format(Locale.US, "%d", krusty.tanks[0].getMaxFill()) + " mB" });
	}
	
	protected void mouseClicked(int mouseX, int mouseY, int i) {
		super.mouseClicked(mouseX, mouseY, i);
		this.field.mouseClicked(mouseX, mouseY, i);
    	
    	if(guiLeft + 8 <= mouseX && guiLeft + 8 + 33 > mouseX && guiTop + 99 < mouseY && guiTop + 99 + 16 >= mouseY)
    		displays[2].setBlinks(true);
    	else
    		displays[2].setBlinks(false);
    	
    	if(guiLeft + 44 <= mouseX && guiLeft + 44 + 11 > mouseX && guiTop + 97 < mouseY && guiTop + 97 + 20 >= mouseY) {
			
    		double level;
    		
			if(NumberUtils.isNumber(field.getText())) {
				int j = (int)MathHelper.clamp_double(Double.parseDouble(field.getText()), 0, 100);
				field.setText(j + "");
				level = j * 0.01D;
			} else {
				return;
			}
			
			NBTTagCompound control = new NBTTagCompound();
			control.setDouble("level", level);
			timer = 15;
			
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(control, krusty.xCoord, krusty.yCoord, krusty.zCoord));
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("hbm:block.rbmk_az5_cover"), 0.5F));
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.krusty.hasCustomInventoryName() ? this.krusty.getInventoryName() : I18n.format(this.krusty.getInventoryName());
		final String[] labels = { "Flux", "Heat", "Control" };
		
		this.fontRendererObj.drawString(name, 121 - this.fontRendererObj.getStringWidth(name) / 2, 6, 15066597);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
		this.fontRendererObj.drawString(labels[0], 6, 13, 15066597);
		this.fontRendererObj.drawString(labels[1], 6, 51, 15066597);
		this.fontRendererObj.drawString(labels[2], 6, 89, 15066597);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		
		if(krusty.level <= 0.5D)
			drawTexturedModalRect(guiLeft + 81 + 36, guiTop + 26 + 36, 176, 0, 8, 8);
		
		if(timer > 0) {
			drawTexturedModalRect(guiLeft + 44, guiTop + 97, 176, 8, 11, 20);
			timer--;
		}
		
		for(byte i = 0; i < 2; i++)
			displays[i].drawNumber(krusty.getDisplayData()[i]);
		
		if(NumberUtils.isDigits(field.getText())) {
			int level = (int)MathHelper.clamp_double(Double.parseDouble(field.getText()), 0, 100);
			field.setText(level + "");
			displays[2].drawNumber(level);
		} else {
			field.setText(0 + "");
			displays[2].drawNumber(0);
		}

		this.drawInfoPanel(guiLeft - 14, guiTop + 61, 16, 16, 2);
		
		GaugeUtil.drawSmoothGauge(guiLeft + 121, guiTop + 106, this.zLevel, (double) krusty.tanks[0].getFill() / (double) krusty.tanks[0].getMaxFill(), 5, 2, 1, 0x7F0000);
		
		int powerHeight = (int) (krusty.power * 53 / krusty.maxPower);
		drawTexturedModalRect(guiLeft + 143, guiTop + 69 - powerHeight, 176, 52 - powerHeight, 16, powerHeight);
	}
	
	@Override
	protected void keyTyped(char c, int i) {

		if(this.field.textboxKeyTyped(c, i))
			return;
		
		if(i == 1 || i == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
			this.mc.thePlayer.closeScreen();
			return;
		}
		
		super.keyTyped(c, i);
	}
	
	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}
}