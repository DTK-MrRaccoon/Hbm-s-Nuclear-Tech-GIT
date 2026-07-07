package com.hbm.inventory.container;

import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.SlotTakeOnly;
import com.hbm.tileentity.machine.steam.TileEntitySteamBoiler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;

public class ContainerSteamBoiler extends Container {

	private final TileEntitySteamBoiler boiler;

	public ContainerSteamBoiler(InventoryPlayer invPlayer, TileEntitySteamBoiler tile) {
		this.boiler = tile;

		this.addSlotToContainer(new Slot(tile, 0, 62, 53));
		this.addSlotToContainer(new SlotTakeOnly(tile, 1, 134, 53));
		this.addSlotToContainer(new Slot(tile, 2, 8, 17));
		this.addSlotToContainer(new SlotTakeOnly(tile, 3, 8, 53));

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142));
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack result = null;
		Slot slot = (Slot)this.inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			result = stack.copy();

			if(index <= 3) {
				if(!this.mergeItemStack(stack, 4, this.inventorySlots.size(), true)) return null;
			} else {
				if(TileEntityFurnace.getItemBurnTime(stack) > 0) {
					if(!this.mergeItemStack(stack, 0, 1, false)) return null;
				} else if(FluidContainerRegistry.getFluidContent(stack, boiler.water.getTankType()) > 0) {
					if(!this.mergeItemStack(stack, 2, 3, false)) return null;
				} else {
					return null;
				}
			}

			if(stack.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}
		}

		return result;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return this.boiler.isUseableByPlayer(player);
	}
}
