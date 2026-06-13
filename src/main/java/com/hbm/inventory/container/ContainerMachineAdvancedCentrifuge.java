package com.hbm.inventory.container;

import com.hbm.inventory.SlotCraftingOutput;
import com.hbm.inventory.SlotNonRetarded;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.TileEntityMachineAdvancedCentrifuge;
import com.hbm.util.InventoryUtil;
import api.hbm.energymk2.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMachineAdvancedCentrifuge extends ContainerBase {

	private TileEntityMachineAdvancedCentrifuge centrifuge;

	public ContainerMachineAdvancedCentrifuge(InventoryPlayer invPlayer, TileEntityMachineAdvancedCentrifuge tedf) {
		super(invPlayer, tedf);
		centrifuge = tedf;

		// Input slots 0-3
		this.addSlotToContainer(new SlotNonRetarded(tedf, 0, 36, 50));
		this.addSlotToContainer(new SlotNonRetarded(tedf, 1, 36, 69));
		this.addSlotToContainer(new SlotNonRetarded(tedf, 2, 36, 88));
		this.addSlotToContainer(new SlotNonRetarded(tedf, 3, 36, 107));

		// Battery slot
		this.addSlotToContainer(new Slot(tedf, 4, 9, 50));

		// Output slots 5-20
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 5, 63, 50));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 6, 83, 50));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 7, 103, 50));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 8, 123, 50));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 9, 63, 69));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 10, 83, 69));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 11, 103, 69));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 12, 123, 69));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 13, 63, 88));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 14, 83, 88));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 15, 103, 88));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 16, 123, 88));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 17, 63, 107));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 18, 83, 107));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 19, 103, 107));
		this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, tedf, 20, 123, 107));

		// Upgrade slots
		this.addSlotToContainer(new SlotUpgrade(tedf, 21, 149, 22));
		this.addSlotToContainer(new SlotUpgrade(tedf, 22, 149, 40));

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 161 + i * 18));
			}
		}
		for (int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 219));
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack slotOriginal = null;
		Slot slot = (Slot) this.inventorySlots.get(index);

		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			slotOriginal = slotStack.copy();

			if (index <= tile.getSizeInventory() - 1) {
				SlotCraftingOutput.checkAchievements(player, slotStack);
				if (!this.mergeItemStack(slotStack, tile.getSizeInventory(), this.inventorySlots.size(), true)) {
					return null;
				}
			} else {
				if (slotOriginal.getItem() instanceof IBatteryItem || slotOriginal.getItem() == ModItems.battery_creative) {
					if (!this.mergeItemStack(slotStack, 4, 5, false)) return null;
				} else if (slotOriginal.getItem() instanceof ItemMachineUpgrade) {
					if (!this.mergeItemStack(slotStack, 21, 23, false)) return null;
				} else {
					if (!InventoryUtil.mergeItemStack(this.inventorySlots, slotStack, 0, 4, false)) return null;
				}
			}

			if (slotStack.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}

			slot.onPickupFromSlot(player, slotStack);
		}

		return slotOriginal;
	}
}