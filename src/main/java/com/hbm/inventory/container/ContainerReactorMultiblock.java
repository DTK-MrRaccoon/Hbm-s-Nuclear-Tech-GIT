package com.hbm.inventory.container;

import com.hbm.inventory.SlotTakeOnly;
import com.hbm.tileentity.machine.TileEntityMachineReactorLarge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerReactorMultiblock extends Container {

	private TileEntityMachineReactorLarge reactor;

	public ContainerReactorMultiblock(InventoryPlayer invPlayer, TileEntityMachineReactorLarge tedf) {
		reactor = tedf;

		this.addSlotToContainer(new Slot(tedf, 0, 8, 90));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 1, 8, 108));
		this.addSlotToContainer(new Slot(tedf, 2, 26, 90));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 3, 26, 108));
		this.addSlotToContainer(new Slot(tedf, 4, 80, 36));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 5, 80, 72));
		this.addSlotToContainer(new Slot(tedf, 6, 152, 36));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 7, 152, 72));

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
			}
		}
		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 198));
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return reactor.isUseableByPlayer(player);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack stack = null;
		Slot slot = (Slot) this.inventorySlots.get(index);

		if (slot != null && slot.getHasStack()) {
			ItemStack stackInSlot = slot.getStack();
			stack = stackInSlot.copy();

			if (index <= 7) {
				if (!this.mergeItemStack(stackInSlot, 8, this.inventorySlots.size(), true)) {
					return null;
				}
			} else {
				if (reactor.isItemValidForSlot(0, stackInSlot)) {
					if (!this.mergeItemStack(stackInSlot, 0, 1, false)) return null;
				} else if (reactor.isItemValidForSlot(2, stackInSlot)) {
					if (!this.mergeItemStack(stackInSlot, 2, 3, false)) return null;
				} else if (reactor.isItemValidForSlot(4, stackInSlot)) {
					if (!this.mergeItemStack(stackInSlot, 4, 5, false)) return null;
				} else if (reactor.isItemValidForSlot(6, stackInSlot)) {
					if (!this.mergeItemStack(stackInSlot, 6, 7, false)) return null;
				} else {
					return null;
				}
			}

			if (stackInSlot.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}
		}
		return stack;
	}
}