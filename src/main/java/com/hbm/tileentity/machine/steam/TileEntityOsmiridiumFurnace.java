package com.hbm.tileentity.machine.steam;

import com.hbm.inventory.container.ContainerOsmiridiumFurnace;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.gui.GUIOsmiridiumFurnace;

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

public class TileEntityOsmiridiumFurnace extends TileEntitySteamMachineBase {

	public int temperature = 20;
	public final int maxTemperature = 2500;

	public TileEntityOsmiridiumFurnace() {
		super(8, Fluids.ULTRAHOTSTEAM, 1600, 1600);
		this.maxProgress = 100;
	}

	@Override
	public String getName() {
		return "container.osmiridiumFurnace";
	}

	@Override
	protected int getRequiredSteamPerTick() {
		return 750;
	}

	@Override
	protected int getSteamToWasteRatio() {
		return 1;
	}

	@Override
	protected void updateMachineMetrics(boolean isProcessing, int steamAvailable) {
		if(steamAvailable > 0) {
			int targetTemperature = 20 + (int)((maxTemperature - 20) * ((double) steamAvailable / 50.0D));

			if(temperature < targetTemperature) {
				temperature += 10;
				if(temperature > targetTemperature) temperature = targetTemperature;
			} else if(temperature > targetTemperature) {
				temperature -= 10;
				if(temperature < targetTemperature) temperature = targetTemperature;
			}
		} else {
			if(temperature > 20) temperature -= 10;
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

	private boolean canSmeltSlot(int inputSlot, int outputSlot) {
		if(slots[inputSlot] == null) return false;

		ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(slots[inputSlot]);
		if(result == null) return false;

		if(slots[outputSlot] == null) return true;
		if(!slots[outputSlot].isItemEqual(result)) return false;
		return slots[outputSlot].stackSize + result.stackSize <= slots[outputSlot].getMaxStackSize();
	}

	@Override
	protected boolean canProcess() {
		return canSmeltSlot(0, 4) || canSmeltSlot(1, 5) || canSmeltSlot(2, 6) || canSmeltSlot(3, 7);
	}

	@Override
	protected void processItem() {
		boolean didSmelt = false;

		for (int i = 0; i < 4; i++) {
			int inputSlot = i;
			int outputSlot = i + 4;
			if(canSmeltSlot(inputSlot, outputSlot)) {
				ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(slots[inputSlot]).copy();
				if(slots[outputSlot] == null) {
					slots[outputSlot] = result;
				} else {
					slots[outputSlot].stackSize += result.stackSize;
				}
				decrStackSize(inputSlot, 1);
				didSmelt = true;
			}
		}

		if(didSmelt) {
			this.markDirty();
		}
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot >= 0 && slot <= 3) return stack != null && FurnaceRecipes.smelting().getSmeltingResult(stack) != null;
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		if(side == ForgeDirection.UP.ordinal()) return new int[] { 0, 1, 2, 3 };
		if(side == ForgeDirection.DOWN.ordinal()) return new int[] { 4, 5, 6, 7 };
		return new int[] { 0, 1, 2, 3 };
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot >= 4 && slot <= 7;
	}

	@Override
	protected void updateFluidConnections() {
		ForgeDirection back = getBackDirection();
		ForgeDirection down = ForgeDirection.DOWN;

		// Subscribing only to BACK and DOWN (UP removed)
		this.trySubscribe(steam.getTankType(), worldObj, xCoord + back.offsetX, yCoord + back.offsetY, zCoord + back.offsetZ, back);
		this.trySubscribe(steam.getTankType(), worldObj, xCoord + down.offsetX, yCoord + down.offsetY, zCoord + down.offsetZ, down);
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection dir) {
		if(type == steam.getTankType()) {
			// Removed ForgeDirection.UP from the allowed steam inputs
			return dir == ForgeDirection.DOWN || dir == getBackDirection();
		}

		if(type == spentSteam.getTankType()) {
			return dir == getBackDirection() || dir == ForgeDirection.DOWN;
		}

		return false;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerOsmiridiumFurnace(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIOsmiridiumFurnace(player.inventory, this);
	}
}
