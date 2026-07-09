package com.hbm.tileentity.machine.steam;

import com.hbm.inventory.container.ContainerSteamFurnace;
import com.hbm.inventory.gui.GUISteamFurnace;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntitySteamFurnace extends TileEntitySteamMachineBase {

	public int temperature = 20;
	public final int maxTemperature = 500;

	public TileEntitySteamFurnace() {
		super(2, 16000, 160);
	}

	@Override
	public String getName() {
		return "container.steamFurnace";
	}

	@Override
	protected int getRequiredSteamPerTick() {
		return 500;
	}

	@Override
	protected void updateMachineMetrics(boolean isProcessing, int steamAvailable) {
		if(steamAvailable > 0) {
			int targetTemperature = 20 + (int) ((maxTemperature - 20) * ((double) steamAvailable / 500.0D));

			if(temperature < targetTemperature) {
				temperature += 2;
				if(temperature > targetTemperature) temperature = targetTemperature;
			} else if(temperature > targetTemperature) {
				temperature -= 2;
				if(temperature < targetTemperature) temperature = targetTemperature;
			}
		} else {
			if(temperature > 20) temperature -= 2;
		}

		if(temperature < 20) temperature = 20;
		if(temperature > maxTemperature) temperature = maxTemperature;
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
		buf.writeInt(temperature);
	}

	@Override
	protected void deserializeMachine(ByteBuf buf) {
		this.temperature = buf.readInt();
	}

	@Override
	protected void readMachineNBT(NBTTagCompound nbt) {
		this.temperature = nbt.getInteger("temperature");
	}

	@Override
	protected void writeMachineNBT(NBTTagCompound nbt) {
		nbt.setInteger("temperature", temperature);
	}

	@Override
	protected boolean canProcess() {
		if(slots[0] == null) return false;
		ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(slots[0]);
		if(result == null) return false;
		if(slots[1] == null) return true;
		if(!slots[1].isItemEqual(result)) return false;
		return slots[1].stackSize + result.stackSize <= slots[1].getMaxStackSize();
	}

	@Override
	protected void processItem() {
		if(!canProcess()) return;
		ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(slots[0]).copy();
		if(slots[1] == null) {
			slots[1] = result;
		} else if(slots[1].isItemEqual(result)) {
			slots[1].stackSize += result.stackSize;
		}
		decrStackSize(0, 1);
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0) return stack != null && FurnaceRecipes.smelting().getSmeltingResult(stack) != null;
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		if(side == ForgeDirection.UP.ordinal()) return new int[] { 0 };
		if(side == ForgeDirection.DOWN.ordinal()) return new int[] { 1 };
		return new int[] { 0 };
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot == 1;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerSteamFurnace(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUISteamFurnace(player.inventory, this);
	}
}
