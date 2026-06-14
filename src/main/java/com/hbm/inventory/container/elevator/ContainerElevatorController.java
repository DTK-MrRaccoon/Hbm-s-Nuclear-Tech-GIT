package com.hbm.inventory.container.elevator;

import com.hbm.tileentity.machine.elevator.TileEntityElevatorController;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

public class ContainerElevatorController extends Container {

	public TileEntityElevatorController controller;

	public ContainerElevatorController(InventoryPlayer invPlayer, TileEntityElevatorController te) {
		this.controller = te;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		double distToController = player.getDistanceSq(
			controller.xCoord + 0.5,
			controller.yCoord + 0.5,
			controller.zCoord + 0.5);

		if (distToController <= 64) {
			return true;
		}

		double dx = player.posX - (controller.xCoord + 0.5);
		double dz = player.posZ - (controller.zCoord + 0.5);
		double horizontalDistSq = dx * dx + dz * dz;

		return horizontalDistSq <= 100;
	}
}
