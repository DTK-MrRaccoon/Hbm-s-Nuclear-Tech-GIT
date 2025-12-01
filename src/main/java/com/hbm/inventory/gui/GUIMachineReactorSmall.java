package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerMachineReactorSmall;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.RefStrings;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.machine.TileEntityMachineReactorSmall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIMachineReactorSmall extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_reactor_experimental.png");
	private TileEntityMachineReactorSmall reactor;

	public GUIMachineReactorSmall(InventoryPlayer invPlayer, TileEntityMachineReactorSmall tedf) {
		super(new ContainerMachineReactorSmall(invPlayer, tedf));
		reactor = tedf;
		
		this.xSize = 176;
		this.ySize = 222;
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		reactor.tanks[0].renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 36, 16, 52);
		reactor.tanks[1].renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 36, 16, 52);
		reactor.tanks[2].renderTankInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 108, 88, 4);
		this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 114, 88, 4, new String[] { "Hull Temperature:", "   " + Math.round((reactor.hullHeat) * 0.00001 * 980 + 20) + "°C" });
		this.drawCustomInfo(this, mouseX, mouseY, guiLeft + 80, guiTop + 120, 88, 4, new String[] { "Core Temperature:", "   " + Math.round((reactor.coreHeat) * 0.00002 * 980 + 20) + "°C" });
		
		String[] text = new String[] { "Water is the primary coolant.",
				"It absorbs heat from the hull and",
				"generates steam.",
				"",
				"Coolant is for emergency use only.",
				"It will cool the core when water",
				"is depleted.",
				"",
				"Water blocks next to the reactor",
				"help with cooling." };
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 36, 16, 16, guiLeft - 8, guiTop + 36 + 16, text);
		
		String[] text1 = new String[] { "Raise/lower the control rods",
				"using the button next to the",
				"fluid gauges.",
				"",
				"Fuel rods lock when activated",
				"and unlock when depleted." };
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 52, 16, 16, guiLeft - 8, guiTop + 52 + 16, text1);

		// Reactor stats info box (orange/yellow)
		int totalRuntime = 0;
		int activeRods = 0;
		for(int i = 0; i < 12; i++) {
			if(reactor.rodLocked[i]) {
				activeRods++;
				totalRuntime += reactor.rodDuration[i];
			}
		}
		int runtimeSeconds = totalRuntime / 20;
		int runtimeMinutes = runtimeSeconds / 60;
		int displaySeconds = runtimeSeconds % 60;
		
		String[] reactorStats = new String[] { 
			"Reactor Statistics:",
			"Active Rods: " + activeRods + "/12",
			"Total Runtime: " + runtimeMinutes + "m " + displaySeconds + "s",
			"Core Heat: " + reactor.coreHeat + "/" + reactor.maxCoreHeat,
			"Hull Heat: " + reactor.hullHeat + "/" + reactor.maxHullHeat,
			"Fuel: " + reactor.getFuelPercent() + "%"
		};
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 68, 16, 16, guiLeft - 8, guiTop + 68 + 16, reactorStats);

		int warningY = 84; // Start position for warnings
		if(reactor.tanks[0].getFill() <= 0 && reactor.coreHeat > 0) {
			String[] text2 = new String[] { "Warning: Water depleted!",
					"Reactor is using emergency coolant." };
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + warningY, 16, 16, guiLeft - 8, guiTop + warningY + 16, text2);
			warningY += 16; // Move next warning down
		}

		if(reactor.coreHeat > reactor.maxCoreHeat * 0.75) {
			String[] text3 = new String[] { "DANGER: Reactor overheating!",
					"Leaking radiation!" };
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + warningY, 16, 16, guiLeft - 8, guiTop + warningY + 16, text3);
		}
		
		String s = "0";
		FluidType type = reactor.tanks[2].getTankType();
		if(type == Fluids.STEAM) s = "1x";
		else if(type == Fluids.HOTSTEAM) s = "10x";
		else if(type == Fluids.SUPERHOTSTEAM) s = "100x";
		
		String[] text4 = new String[] { "Steam compression switch",
				"Current compression level: " + s};
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 63, guiTop + 107, 14, 18, mouseX, mouseY, text4);
		
		String[] text5 = new String[] { reactor.retracting ? "Raise control rods" : "Lower control rods"};
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 52, guiTop + 53, 18, 18, mouseX, mouseY, text5);
		
		// Show rod status on hover
		for(int i = 0; i < 12; i++) {
			int slotX = getSlotX(i);
			int slotY = getSlotY(i);
			if(mouseX >= guiLeft + slotX && mouseX < guiLeft + slotX + 18 && 
			   mouseY >= guiTop + slotY && mouseY < guiTop + slotY + 18) {
				if(reactor.rodLocked[i] || (reactor.rodMaxDuration[i] > 0 && reactor.rodDuration[i] > 0)) {
					// Calculate remaining time in minutes (20 ticks = 1 second)
					int remainingTicks = reactor.rodDuration[i];
					int remainingSeconds = remainingTicks / 20;
					int remainingMinutes = remainingSeconds / 60;
					int rodDisplaySeconds = remainingSeconds % 60;
					
					String timeStr = remainingMinutes + "m " + rodDisplaySeconds + "s";
					String statusStr = reactor.rodLocked[i] ? "ACTIVE" : "PAUSED";
					
					String[] rodInfo = new String[] {
						"Rod Status: " + statusStr,
						"Time Remaining: " + timeStr,
						"Duration: " + reactor.rodDuration[i] + "/" + reactor.rodMaxDuration[i],
						"Heat: " + reactor.rodHeat[i] + " per tick",
						"Flux: " + reactor.rodFlux[i]
					};
					this.drawCustomInfoStat(mouseX, mouseY, guiLeft + slotX, guiTop + slotY, 18, 18, mouseX, mouseY + 40, rodInfo);
				}
			}
		}
	}
	
	private int getSlotX(int id) {
		switch(id) {
		case 0: return 98;
		case 1: return 134;
		case 2: return 80;
		case 3: return 116;
		case 4: return 152;
		case 5: return 98;
		case 6: return 134;
		case 7: return 80;
		case 8: return 116;
		case 9: return 152;
		case 10: return 98;
		case 11: return 134;
		}
		return 0;
	}
	
	private int getSlotY(int id) {
		switch(id) {
		case 0: case 1: return 18;
		case 2: case 3: case 4: return 36;
		case 5: case 6: return 54;
		case 7: case 8: case 9: return 72;
		case 10: case 11: return 90;
		}
		return 0;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.reactor.hasCustomInventoryName() ? this.reactor.getInventoryName() : I18n.format(this.reactor.getInventoryName());
		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 6, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@SuppressWarnings("incomplete-switch")
	protected void mouseClicked(int x, int y, int i) {
		super.mouseClicked(x, y, i);
		
		// Control rod button
		if(guiLeft + 52 <= x && guiLeft + 52 + 16 > x && guiTop + 53 < y && guiTop + 53 + 16 >= y) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
			NBTTagCompound control = new NBTTagCompound();
			control.setBoolean("rods", true);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(control, reactor.xCoord, reactor.yCoord, reactor.zCoord));
		}
		
		// Steam compression button
		if(guiLeft + 63 <= x && guiLeft + 63 + 14 > x && guiTop + 107 < y && guiTop + 107 + 18 >= y) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
			int c = 0;
			FluidType type = reactor.tanks[2].getTankType();
			if(type == Fluids.STEAM) c = 0;
			else if(type == Fluids.HOTSTEAM) c = 1;
			else if(type == Fluids.SUPERHOTSTEAM) c = 2;
			// Cycle to next compression level
			c = (c + 1) % 3;
			NBTTagCompound control = new NBTTagCompound();
			control.setInteger("compression", c);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(control, reactor.xCoord, reactor.yCoord, reactor.zCoord));
		}
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		
		// Steam bar
		if(reactor.tanks[2].getFill() > 0) {
			int i = reactor.getSteamScaled(88);
			int offset = 234;
			FluidType type = reactor.tanks[2].getTankType();
			if(type == Fluids.HOTSTEAM) offset += 4;
			else if(type == Fluids.SUPERHOTSTEAM) offset += 8;
			drawTexturedModalRect(guiLeft + 80, guiTop + 108, 0, offset, i, 4);
		}
		
		// Hull heat bar
		if(reactor.hasHullHeat()) {
			int i = reactor.getHullHeatScaled(88);
			i = (int) Math.min(i, 160);
			drawTexturedModalRect(guiLeft + 80, guiTop + 114, 0, 226, i, 4);
		}
		
		// Core heat bar
		if(reactor.hasCoreHeat()) {
			int i = reactor.getCoreHeatScaled(88);
			i = (int) Math.min(i, 160);
			drawTexturedModalRect(guiLeft + 80, guiTop + 120, 0, 230, i, 4);
		}

		// Control rod button
		if(!reactor.retracting)
			drawTexturedModalRect(guiLeft + 52, guiTop + 53, 212, 0, 18, 18);
		
		// Rod status indicators - ALWAYS SHOWN
		if(reactor.rods >= reactor.rodsMax) {
			for(int x = 0; x < 3; x++)
				for(int y = 0; y < 3; y++)
					drawTexturedModalRect(guiLeft + 79 + 36 * x, guiTop + 17 + 36 * y, 176, 0, 18, 18);
		} else if(reactor.rods > 0) {
			for(int x = 0; x < 3; x++)
				for(int y = 0; y < 3; y++)
					drawTexturedModalRect(guiLeft + 79 + 36 * x, guiTop + 17 + 36 * y, 194, 0, 18, 18);
		}
		
		// Draw locked rod indicators - ALWAYS SHOWN
		for(int i = 0; i < 12; i++) {
			if(reactor.rodLocked[i]) {
				int slotX = getSlotX(i);
				int slotY = getSlotY(i);
				// Draw a semi-transparent red overlay to indicate locked
				drawGradientRect(guiLeft + slotX, guiTop + slotY, guiLeft + slotX + 16, guiTop + slotY + 16, 0x60FF0000, 0x60FF0000);
			}
		}
		
		// Steam compression button
		FluidType type = reactor.tanks[2].getTankType();
		if(type == Fluids.STEAM) drawTexturedModalRect(guiLeft + 63, guiTop + 107, 176, 18, 14, 18);
		else if(type == Fluids.HOTSTEAM) drawTexturedModalRect(guiLeft + 63, guiTop + 107, 190, 18, 14, 18);
		else if(type == Fluids.SUPERHOTSTEAM) drawTexturedModalRect(guiLeft + 63, guiTop + 107, 204, 18, 14, 18);
		
		// Info panels
		this.drawInfoPanel(guiLeft - 16, guiTop + 36, 16, 16, 2);
		this.drawInfoPanel(guiLeft - 16, guiTop + 52, 16, 16, 3);
		this.drawInfoPanel(guiLeft - 16, guiTop + 68, 16, 16, 7); // Orange/yellow reactor stats
		
		int warningPanelY = 84;
		if(reactor.tanks[0].getFill() <= 0 && reactor.coreHeat > 0) {
			this.drawInfoPanel(guiLeft - 16, guiTop + warningPanelY, 16, 16, 6);
			warningPanelY += 16;
		}
		
		if(reactor.coreHeat > reactor.maxCoreHeat * 0.50)
			this.drawInfoPanel(guiLeft - 16, guiTop + warningPanelY, 16, 16, 7);

		// Render fluid tanks
		reactor.tanks[0].renderTank(guiLeft + 8, guiTop + 88, this.zLevel, 16, 52);
		reactor.tanks[1].renderTank(guiLeft + 26, guiTop + 88, this.zLevel, 16, 52);
	}
}