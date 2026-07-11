package com.hbm.tileentity.machine.steam;

import com.hbm.inventory.container.ContainerSteamPress;
import com.hbm.inventory.gui.GUISteamPress;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntitySteamPress extends TileEntitySteamMachineBase {

	public int pressure = 0;
	public final int maxPressure = 100;

	public TileEntitySteamPress() {
		super(3, 16000, 160);
	}

	@Override
	public String getName() {
		return "container.steamPress";
	}

	@Override
	protected int getRequiredSteamPerTick() {
		return 50 + (int) (200.0D * ((double) progress / (double) Math.max(1, maxProgress)));
	}

	@Override
	protected void updateMachineMetrics(boolean isProcessing, int steamAvailable) {
		int targetSteam = getRequiredSteamPerTick();

		if(isProcessing && steamAvailable > 0) {
			double steamRatio = (double) steamAvailable / (double) targetSteam;
			int targetPressure = (int) (maxPressure * steamRatio);

			if(pressure < targetPressure) {
				pressure++;
			} else if(pressure > targetPressure) {
				pressure--;
			}

			if(pressure > maxPressure) pressure = maxPressure;
			if(pressure < 0) pressure = 0;
		} else {
			if(pressure > 0) pressure--;
		}
	}

	@Override
	protected float getProgressIncrement(boolean isProcessing, int steamAvailable) {
		if (isProcessing && steamAvailable > 0) {
			return (float) steamAvailable / (float) getRequiredSteamPerTick();
		}
		return 0.0F;
	}

	@Override
	protected void serializeMachine(ByteBuf buf) {
		buf.writeInt(pressure);
	}

	@Override
	protected void deserializeMachine(ByteBuf buf) {
		this.pressure = buf.readInt();
	}

	@Override
	protected void readMachineNBT(NBTTagCompound nbt) {
		this.pressure = nbt.getInteger("pressure");
	}

	@Override
	protected void writeMachineNBT(NBTTagCompound nbt) {
		nbt.setInteger("pressure", pressure);
	}

	@Override
	protected boolean canProcess() {
		if(slots[0] == null || slots[1] == null) return false;
		if(!(slots[0].getItem() instanceof ItemStamp)) return false;
		if(slots[0].getItemDamage() >= slots[0].getMaxDamage() && slots[0].getMaxDamage() > 0) return false;

		ItemStack result = PressRecipes.getOutput(slots[1], slots[0]);
		if(result == null) return false;
		if(slots[2] == null) return true;
		if(!slots[2].isItemEqual(result)) return false;
		return slots[2].stackSize + result.stackSize <= slots[2].getMaxStackSize();
	}

	@Override
	protected void processItem() {
		if(!canProcess()) return;
		ItemStack result = PressRecipes.getOutput(slots[1], slots[0]).copy();
		if(slots[2] == null) {
			slots[2] = result;
		} else if(slots[2].isItemEqual(result)) {
			slots[2].stackSize += result.stackSize;
		}

		if(slots[0].getMaxDamage() > 0) {
			slots[0].setItemDamage(slots[0].getItemDamage() + 1);
			if(slots[0].getItemDamage() >= slots[0].getMaxDamage()) {
				slots[0] = null;
			}
		}

		decrStackSize(1, PressRecipes.getInputAmount(slots[1], slots[0]));
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0) return stack != null && stack.getItem() instanceof ItemStamp;
		if(slot == 1) return stack != null;
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		if(side == ForgeDirection.UP.ordinal()) return new int[] { 0, 1 };
		if(side == ForgeDirection.DOWN.ordinal()) return new int[] { 2 };
		return new int[] { 1 };
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot == 2;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerSteamPress(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUISteamPress(player.inventory, this);
	}
}
