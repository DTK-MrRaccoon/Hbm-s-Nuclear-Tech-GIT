package com.hbm.inventory.container;

import com.hbm.inventory.SlotCraftingOutput;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.TileEntityMachineAdvancedCentrifuge;
import api.hbm.energymk2.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMachineAdvancedCentrifuge extends Container {

    private TileEntityMachineAdvancedCentrifuge centrifuge;

    public ContainerMachineAdvancedCentrifuge(InventoryPlayer invPlayer, TileEntityMachineAdvancedCentrifuge tedf) {
        centrifuge = tedf;

        this.addSlotToContainer(new Slot(tedf, 0, 36, 50));
        this.addSlotToContainer(new Slot(tedf, 1, 36, 69));
        this.addSlotToContainer(new Slot(tedf, 2, 36, 88));
        this.addSlotToContainer(new Slot(tedf, 3, 36, 107));
        this.addSlotToContainer(new Slot(tedf, 4, 9, 50));
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
        this.addSlotToContainer(new SlotUpgrade(tedf, 21, 149, 22));
        this.addSlotToContainer(new SlotUpgrade(tedf, 22, 149, 40));

        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 161 + i * 18));
            }
        }

        for(int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 219));
        }
    }

    @Override
    public ItemStack transferStackInSlot(net.minecraft.entity.player.EntityPlayer player, int index) {
        ItemStack rStack = null;
        net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) this.inventorySlots.get(index);

        if(slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            rStack = stack.copy();

            if(index <= 22) {
                if(!this.mergeItemStack(stack, 23, this.inventorySlots.size(), true)) {
                    return null;
                }
                slot.onSlotChange(stack, rStack);
            } else {
                if(rStack.getItem() instanceof IBatteryItem || rStack.getItem() == ModItems.battery_creative) {
                    if(!this.mergeItemStack(stack, 4, 5, false))
                        return null;
                } else if(rStack.getItem() instanceof ItemMachineUpgrade) {
                    if(!this.mergeItemStack(stack, 21, 23, false))
                        return null;
                } else {
                    boolean placed = false;
                    for(int i = 0; i < 4; i++) {
                        if(this.mergeItemStack(stack, i, i + 1, false)) {
                            placed = true;
                            break;
                        }
                    }
                    if(!placed) return null;
                }
            }

            if(stack.stackSize == 0) {
                slot.putStack((ItemStack) null);
            } else {
                slot.onSlotChanged();
            }
        }

        return rStack;
    }

    @Override
    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
        return centrifuge.isUseableByPlayer(player);
    }
}
