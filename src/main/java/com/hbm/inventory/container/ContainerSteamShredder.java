package com.hbm.inventory.container;

import com.hbm.inventory.SlotCraftingOutput;
import com.hbm.items.machine.ItemBlades;
import com.hbm.tileentity.machine.steam.TileEntitySteamShredder;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerSteamShredder extends Container {

	private final TileEntitySteamShredder shredder;

	public ContainerSteamShredder(InventoryPlayer invPlayer, TileEntitySteamShredder te) {
		this.shredder = te;

		this.addSlotToContainer(new Slot(te, 0, 80, 17));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, te, 1, 80, 48));
		this.addSlotToContainer(new Slot(te, 2, 60, 17));
		this.addSlotToContainer(new Slot(te, 3, 100, 17));

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
		Slot slot = (Slot) this.inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			result = stack.copy();

			if(index <= 3) {
				if(!this.mergeItemStack(stack, 4, this.inventorySlots.size(), true)) return null;
			} else {
				if(stack.getItem() instanceof ItemBlades) {
					if(!this.mergeItemStack(stack, 2, 4, false)) return null;
				} else {
					if(!this.mergeItemStack(stack, 0, 1, false)) return null;
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
		return shredder.isUseableByPlayer(player);
	}
}
