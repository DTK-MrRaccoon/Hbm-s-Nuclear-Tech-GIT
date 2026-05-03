package com.hbm.inventory.gui.elevator;

import java.util.List;

import com.hbm.inventory.container.elevator.ContainerElevatorController;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.elevator.TileEntityElevatorController;

import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class GUIElevatorController extends GuiContainer {

	private TileEntityElevatorController controller;

	private static final int BTN_SPAWN	  = 100;
	private static final int BTN_DESPAWN	= 101;
	private static final int BTN_FLOOR_BASE = 200;

	private static final int COL_BG		 = 0xFF2B2B2B;
	private static final int COL_PANEL	  = 0xFF3A3A3A;
	private static final int COL_BORDER	 = 0xFF555555;
	private static final int COL_TITLE	  = 0xFFFFFFFF;
	private static final int COL_TEXT	   = 0xFFCCCCCC;
	private static final int COL_TEXT_DIM   = 0xFF888888;
	private static final int COL_POWER_BG   = 0xFF1A1A1A;
	private static final int COL_POWER_HI   = 0xFF22BB22;
	private static final int COL_POWER_LO   = 0xFFBB6600;
	private static final int COL_ACTIVE	 = 0xFF005500;

	public GUIElevatorController(InventoryPlayer inv, TileEntityElevatorController te) {
		super(new ContainerElevatorController(inv, te));
		this.controller = te;
		this.xSize = 180;
		this.ySize = 220;
	}

	@Override
	public void initGui() {
		super.initGui();
		rebuildButtons();
	}

	private void rebuildButtons() {
		this.buttonList.clear();
		int bx = guiLeft + 8;
		int by = guiTop + 60;

		this.buttonList.add(new GuiButton(BTN_SPAWN,   bx,	  by, 80, 16, "Spawn"));
		this.buttonList.add(new GuiButton(BTN_DESPAWN, bx + 84, by, 80, 16, "Despawn"));

		List<Integer> floors = controller.floors;
		for (int i = 0; i < floors.size() && i < 10; i++) {
			int fy = floors.get(i);
			boolean isCurrent = (i == controller.targetFloor && controller.hasPlatform);
			String label = (isCurrent ? "> " : "") + "Floor " + (i + 1) + "  Y=" + fy;
			this.buttonList.add(new GuiButton(BTN_FLOOR_BASE + i,
				bx, by + 22 + i * 14, 164, 12, label));
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		NBTTagCompound nbt = new NBTTagCompound();
		if (button.id == BTN_SPAWN) {
			nbt.setBoolean("spawn", true);
		} else if (button.id == BTN_DESPAWN) {
			nbt.setBoolean("despawn", true);
		} else if (button.id >= BTN_FLOOR_BASE) {
			nbt.setInteger("goFloor", button.id - BTN_FLOOR_BASE);
		}
		PacketDispatcher.wrapper.sendToServer(
			new NBTControlPacket(nbt, controller.xCoord, controller.yCoord, controller.zCoord));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
		GL11.glDisable(GL11.GL_LIGHTING);

		int x = guiLeft;
		int y = guiTop;
		int w = xSize;
		int h = ySize;

		drawRect(x - 1, y - 1, x + w + 1, y + h + 1, COL_BORDER);
		drawRect(x, y, x + w, y + h, COL_BG);
		drawRect(x, y, x + w, y + 16, COL_PANEL);
		drawRect(x, y + 16, x + w, y + 17, COL_BORDER);

		int barX = x + 8;
		int barY = y + 30;
		int barW = w - 16;
		int barH = 10;
		drawRect(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, COL_BORDER);
		drawRect(barX, barY, barX + barW, barY + barH, COL_POWER_BG);

		long maxP = controller.getMaxPower();
		if (maxP > 0) {
			int filled = (int) (barW * controller.power / maxP);
			if (filled > 0) {
				int col = controller.power > maxP / 2 ? COL_POWER_HI : COL_POWER_LO;
				drawRect(barX, barY, barX + filled, barY + barH, col);
			}
		}

		if (controller.hasPlatform && !controller.floors.isEmpty()) {
			int fi = controller.targetFloor;
			if (fi >= 0 && fi < controller.floors.size() && fi < 10) {
				int btnY = guiTop + 60 + 22 + fi * 14;
				drawRect(guiLeft + 8, btnY, guiLeft + 8 + 164, btnY + 12, COL_ACTIVE);
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		String title = "Elevator Controller";
		fontRendererObj.drawString(title,
			xSize / 2 - fontRendererObj.getStringWidth(title) / 2, 4, COL_TITLE);

		long pct = controller.getMaxPower() > 0
			? controller.power * 100 / controller.getMaxPower() : 0;
		fontRendererObj.drawString(
			"HE: " + controller.power + " / " + controller.getMaxPower() + "  (" + pct + "%)",
			8, 20, COL_TEXT);

		fontRendererObj.drawString(
			"Size: " + controller.platformSize + "x" + controller.platformSize
			+ "   Floors: " + controller.floors.size()
			+ "   " + (controller.hasPlatform ? "Active" : "No platform"),
			8, 44, COL_TEXT_DIM);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		rebuildButtons();
	}

	@Override
	public void drawScreen(int mx, int my, float partial) {
		this.drawDefaultBackground();
		super.drawScreen(mx, my, partial);
	}
}
