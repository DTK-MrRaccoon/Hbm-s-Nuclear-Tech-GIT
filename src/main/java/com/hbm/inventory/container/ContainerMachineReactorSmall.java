package com.hbm.inventory.container;

import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.SlotTakeOnly;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntityMachineReactorSmall;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMachineReactorSmall extends Container {

	private TileEntityMachineReactorSmall reactor;

	public ContainerMachineReactorSmall(InventoryPlayer invPlayer, TileEntityMachineReactorSmall tedf) {
		reactor = tedf;

		this.addSlotToContainer(new Slot(tedf, 0, 98, 18) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 1, 134, 18) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 2, 80, 36) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 3, 116, 36) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 4, 152, 36) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 5, 98, 54) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 6, 134, 54) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 7, 80, 72) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 8, 116, 72) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 9, 152, 72) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 10, 98, 90) { @Override public int getSlotStackLimit() { return 1; } });
		this.addSlotToContainer(new Slot(tedf, 11, 134, 90) { @Override public int getSlotStackLimit() { return 1; } });

		this.addSlotToContainer(new Slot(tedf, 12, 8, 90));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 13, 8, 108));
		this.addSlotToContainer(new Slot(tedf, 14, 26, 90));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 15, 26, 108));

		for(int i = 0; i < 3; i++)
			for(int j = 0; j < 9; j++)
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
		for(int i = 0; i < 9; i++)
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 198));
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
		ItemStack stack = null;
		Slot slotObj = (Slot)inventorySlots.get(slot);
		if(slotObj != null && slotObj.getHasStack()) {
			ItemStack stackInSlot = slotObj.getStack();
			stack = stackInSlot.copy();

			if(slot <= 15) {
				if(!mergeItemStack(stackInSlot, 16, inventorySlots.size(), true)) return null;
			} else {
				if(stackInSlot.getItem() == ModItems.neutron_reflector ||
					stackInSlot.getItem() instanceof com.hbm.items.machine.ItemBreedingRod) {
					if(!mergeItemStack(stackInSlot, 0, 12, false)) return null;
				} else if(FluidContainerRegistry.getFluidContent(stackInSlot, reactor.tanks[0].getTankType()) > 0) {
					if(!mergeItemStack(stackInSlot, 12, 13, false)) return null;
				} else if(FluidContainerRegistry.getFluidContent(stackInSlot, reactor.tanks[1].getTankType()) > 0) {
					if(!mergeItemStack(stackInSlot, 14, 15, false)) return null;
				} else {
					return null;
				}
			}

			if(stackInSlot.stackSize == 0) slotObj.putStack(null);
			else slotObj.onSlotChanged();
		}
		return stack;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return reactor.isUseableByPlayer(player);
	}
}
