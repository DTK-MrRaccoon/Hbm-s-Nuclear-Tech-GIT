package com.hbm.inventory.container;

import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.tileentity.machine.TileEntityMachineKrusty;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.SlotTakeOnly;
import com.hbm.items.machine.IItemFluidIdentifier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMachineKrusty extends Container {

private TileEntityMachineKrusty krusty;
	
	public ContainerMachineKrusty(InventoryPlayer invPlayer, TileEntityMachineKrusty tedf) {
		
		krusty = tedf;
		
		// fuel
		this.addSlotToContainer(new Slot(tedf, 0, 113, 40));
		this.addSlotToContainer(new Slot(tedf, 1, 95, 58));
		this.addSlotToContainer(new Slot(tedf, 2, 131, 58));
		this.addSlotToContainer(new Slot(tedf, 3, 113, 76));
		// fluid
		this.addSlotToContainer(new Slot(tedf, 4, 92, 98));
		this.addSlotToContainer(new SlotTakeOnly(tedf, 5, 134, 98));
		
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
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack var3 = null;
		Slot slot = (Slot) this.inventorySlots.get(index);
		
		if (slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			var3 = stack.copy();
			
            if (index <= 5) {
				if (!this.mergeItemStack(stack, 6, this.inventorySlots.size(), true)){
					return null;
				}
			} else {
				if(FluidContainerRegistry.getFluidContent(stack, krusty.tanks[0].getTankType()) > 0) {
					if(!this.mergeItemStack(stack, 4, 5, false))
						return null;
				} else {
					if(stack.getItem() instanceof ItemPlateFuel) {
						if (!this.mergeItemStack(stack, 0, 4, false))
							return null;
					} else {
						return null;
					}
				}
			}
            
			if (stack.stackSize == 0) {
				slot.putStack((ItemStack) null);
			} else {
				slot.onSlotChanged();
			}
		}
		
		return var3;
    }

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return krusty.isUseableByPlayer(player);
	}
}
