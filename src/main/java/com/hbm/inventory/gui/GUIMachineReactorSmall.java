package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerMachineReactorSmall;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.lib.RefStrings;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.machine.TileEntityMachineReactorSmall;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
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
				"fluid gauges." };
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 52, 16, 16, guiLeft - 8, guiTop + 52 + 16, text1);

		// Reactor stats (NO runtime display)
		int activeRods = 0;
		for(int i = 0; i < 12; i++) {
			ItemStack stack = reactor.slots[i];
			if(stack != null && stack.getItem() instanceof ItemBreedingRod) {
				activeRods++;
			}
		}
		
		String[] reactorStats = new String[] { 
			"Reactor Statistics:",
			"Active Rods: " + activeRods + "/12",
			"Core Heat: " + reactor.coreHeat + "/" + reactor.maxCoreHeat,
			"Hull Heat: " + reactor.hullHeat + "/" + reactor.maxHullHeat,
			"Fuel: " + reactor.getFuelPercent() + "%"
		};
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 68, 16, 16, guiLeft - 8, guiTop + 68 + 16, reactorStats);

		int warningY = 84;
		if(reactor.tanks[0].getFill() <= 0 && reactor.coreHeat > 0) {
			String[] text2 = new String[] { "Warning: Water depleted!",
					"Reactor is using emergency coolant." };
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + warningY, 16, 16, guiLeft - 8, guiTop + warningY + 16, text2);
			warningY += 16;
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
		
		if(guiLeft + 52 <= x && guiLeft + 52 + 16 > x && guiTop + 53 < y && guiTop + 53 + 16 >= y) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
			NBTTagCompound control = new NBTTagCompound();
			control.setBoolean("rods", true);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(control, reactor.xCoord, reactor.yCoord, reactor.zCoord));
		}
		
		if(guiLeft + 63 <= x && guiLeft + 63 + 14 > x && guiTop + 107 < y && guiTop + 107 + 18 >= y) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
			int c = 0;
			FluidType type = reactor.tanks[2].getTankType();
			if(type == Fluids.STEAM) c = 0;
			else if(type == Fluids.HOTSTEAM) c = 1;
			else if(type == Fluids.SUPERHOTSTEAM) c = 2;
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
		
		if(reactor.tanks[2].getFill() > 0) {
			int i = reactor.getSteamScaled(88);
			int offset = 234;
			FluidType type = reactor.tanks[2].getTankType();
			if(type == Fluids.HOTSTEAM) offset += 4;
			else if(type == Fluids.SUPERHOTSTEAM) offset += 8;
			drawTexturedModalRect(guiLeft + 80, guiTop + 108, 0, offset, i, 4);
		}
		
		if(reactor.hasHullHeat()) {
			int i = reactor.getHullHeatScaled(88);
			i = (int) Math.min(i, 160);
			drawTexturedModalRect(guiLeft + 80, guiTop + 114, 0, 226, i, 4);
		}
		
		if(reactor.hasCoreHeat()) {
			int i = reactor.getCoreHeatScaled(88);
			i = (int) Math.min(i, 160);
			drawTexturedModalRect(guiLeft + 80, guiTop + 120, 0, 230, i, 4);
		}

		if(!reactor.retracting)
			drawTexturedModalRect(guiLeft + 52, guiTop + 53, 212, 0, 18, 18);
		
		if(reactor.rods >= reactor.rodsMax) {
			for(int x = 0; x < 3; x++)
				for(int y = 0; y < 3; y++)
					drawTexturedModalRect(guiLeft + 79 + 36 * x, guiTop + 17 + 36 * y, 176, 0, 18, 18);
		} else if(reactor.rods > 0) {
			for(int x = 0; x < 3; x++)
				for(int y = 0; y < 3; y++)
					drawTexturedModalRect(guiLeft + 79 + 36 * x, guiTop + 17 + 36 * y, 194, 0, 18, 18);
		}
		
		FluidType type = reactor.tanks[2].getTankType();
		if(type == Fluids.STEAM) drawTexturedModalRect(guiLeft + 63, guiTop + 107, 176, 18, 14, 18);
		else if(type == Fluids.HOTSTEAM) drawTexturedModalRect(guiLeft + 63, guiTop + 107, 190, 18, 14, 18);
		else if(type == Fluids.SUPERHOTSTEAM) drawTexturedModalRect(guiLeft + 63, guiTop + 107, 204, 18, 14, 18);
		
		this.drawInfoPanel(guiLeft - 16, guiTop + 36, 16, 16, 2);
		this.drawInfoPanel(guiLeft - 16, guiTop + 52, 16, 16, 3);
		this.drawInfoPanel(guiLeft - 16, guiTop + 68, 16, 16, 7);
		
		int warningPanelY = 84;
		if(reactor.tanks[0].getFill() <= 0 && reactor.coreHeat > 0) {
			this.drawInfoPanel(guiLeft - 16, guiTop + warningPanelY, 16, 16, 6);
			warningPanelY += 16;
		}
		
		if(reactor.coreHeat > reactor.maxCoreHeat * 0.50)
			this.drawInfoPanel(guiLeft - 16, guiTop + warningPanelY, 16, 16, 7);

		reactor.tanks[0].renderTank(guiLeft + 8, guiTop + 88, this.zLevel, 16, 52);
		reactor.tanks[1].renderTank(guiLeft + 26, guiTop + 88, this.zLevel, 16, 52);
	}
}
