package com.hbm.inventory.container;

import com.hbm.inventory.SlotCraftingOutput;
import com.hbm.tileentity.machine.steam.TileEntitySteamFurnace;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

public class ContainerSteamFurnace extends Container {

	private final TileEntitySteamFurnace furnace;

	public ContainerSteamFurnace(InventoryPlayer invPlayer, TileEntitySteamFurnace te) {
		this.furnace = te;

		this.addSlotToContainer(new Slot(te, 0, 56, 17));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, te, 1, 116, 35));

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

			if(index <= 1) {
				if(!this.mergeItemStack(stack, 2, this.inventorySlots.size(), true)) return null;
			} else {
				if(FurnaceRecipes.smelting().getSmeltingResult(stack) != null) {
					if(!this.mergeItemStack(stack, 0, 1, false)) return null;
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
		return furnace.isUseableByPlayer(player);
	}
}
