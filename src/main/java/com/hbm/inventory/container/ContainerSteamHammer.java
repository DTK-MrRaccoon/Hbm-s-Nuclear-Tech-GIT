package com.hbm.inventory.container;

import com.hbm.inventory.SlotCraftingOutput;
import com.hbm.items.tool.ItemTieredHammer;
import com.hbm.tileentity.machine.steam.TileEntitySteamHammer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerSteamHammer extends Container {

	private final TileEntitySteamHammer hammer;

	public ContainerSteamHammer(InventoryPlayer invPlayer, TileEntitySteamHammer te) {
		this.hammer = te;

		this.addSlotToContainer(new Slot(te, 0, 84, 17));
		this.addSlotToContainer(new Slot(te, 1, 56, 47));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, te, 2, 116, 48));

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

			if(index <= 2) {
				if(!this.mergeItemStack(stack, 3, this.inventorySlots.size(), true)) return null;
			} else {
				if(stack.getItem() instanceof ItemTieredHammer) {
					if(!this.mergeItemStack(stack, 0, 1, false)) return null;
				} else {
					if(!this.mergeItemStack(stack, 1, 2, false)) return null;
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
		return hammer.isUseableByPlayer(player);
	}
}
