package com.hbm.inventory.container;

import com.hbm.inventory.SlotTakeOnly;
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
		
		// Fuel rod slots (0-11) - 3x4 grid (max stack size 1)
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
		
		// Fluid IO
		this.addSlotToContainer(new Slot(tedf, 12, 8, 90));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 13, 8, 108));
		this.addSlotToContainer(new Slot(tedf, 14, 26, 90));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 15, 26, 108));
		
		// Player inventory
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 56));
			}
		}
		
		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142 + 56));
		}
	}
	
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int par2) {
		ItemStack var3 = null;
		Slot var4 = (Slot) this.inventorySlots.get(par2);
		
		if (var4 != null && var4.getHasStack()) {
			ItemStack var5 = var4.getStack();
			var3 = var5.copy();
			
			if (par2 <= 15) {
				// Prevent shift-click extraction of locked or partially used rods
				if(par2 >= 0 && par2 <= 11) {
					if(reactor.rodLocked[par2]) {
						return null; // Rod is locked
					}
					if(reactor.rodMaxDuration[par2] > 0 && reactor.rodDuration[par2] < reactor.rodMaxDuration[par2] && reactor.rodDuration[par2] > 0) {
						return null; // Rod is partially used
					}
				}
				
				if (!this.mergeItemStack(var5, 16, this.inventorySlots.size(), true)) {
					return null;
				}
			} else {
				if (!this.mergeItemStack(var5, 0, 12, false))
					if (!this.mergeItemStack(var5, 12, 13, false))
						if (!this.mergeItemStack(var5, 14, 15, false))
							return null;
			}
			
			if (var5.stackSize == 0) {
				var4.putStack((ItemStack) null);
			} else {
				var4.onSlotChanged();
			}
		}
		
		return var3;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return reactor.isUseableByPlayer(player);
	}
}
