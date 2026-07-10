package com.hbm.tileentity.machine.steam;

import com.hbm.inventory.container.ContainerSteamShredder;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.gui.GUISteamShredder;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.machine.ItemBlades;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntitySteamShredder extends TileEntitySteamMachineBase {

	public int speed = 0;
	public int maxSpeed = 100;
	public boolean bronze = false;

	public TileEntitySteamShredder() {
		this(false);
	}

	public TileEntitySteamShredder(boolean bronze) {
		super(4, bronze ? 8000 : 16000, bronze ? 50 : 100);
		this.bronze = bronze;
		this.maxSpeed = bronze ? 50 : 100;
	}

	public boolean isBronze() {
		return this.bronze;
	}

	@Override
	public String getName() {
		return this.bronze ? "container.steamShredderBronze" : "container.steamShredder";
	}

	@Override
	protected int getRequiredSteamPerTick() {
		return this.bronze ? 333 : 166;
	}

	@Override
	protected void updateMachineMetrics(boolean isProcessing, int steamAvailable) {
		if(isProcessing && steamAvailable > 0) {
			int requiredSteam = this.getRequiredSteamPerTick();
			int targetSpeed = (int) (maxSpeed * ((double) steamAvailable / (double) requiredSteam));

			if(speed < targetSpeed) {
				speed++;
			} else if(speed > targetSpeed) {
				speed--;
			}

			if(speed > maxSpeed) speed = maxSpeed;
			if(speed < 0) speed = 0;
		} else {
			if(speed > 0) speed -= 2;
			if(speed < 0) speed = 0;
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
		buf.writeBoolean(this.bronze);
		buf.writeInt(speed);
	}

	@Override
	protected void deserializeMachine(ByteBuf buf) {
		this.bronze = buf.readBoolean();
		this.speed = buf.readInt();
	}

	@Override
	protected void readMachineNBT(NBTTagCompound nbt) {
		this.bronze = nbt.getBoolean("bronze");
		this.speed = nbt.getInteger("speed");
	}

	@Override
	protected void writeMachineNBT(NBTTagCompound nbt) {
		nbt.setInteger("speed", speed);
		nbt.setBoolean("bronze", bronze);
	}

	@Override
	protected boolean canProcess() {
		if(slots[0] == null) return false;

		if(slots[2] == null || !(slots[2].getItem() instanceof ItemBlades)) return false;
		if(slots[3] == null || !(slots[3].getItem() instanceof ItemBlades)) return false;
		if(slots[2].getItemDamage() >= slots[2].getMaxDamage() && slots[2].getMaxDamage() > 0) return false;
		if(slots[3].getItemDamage() >= slots[3].getMaxDamage() && slots[3].getMaxDamage() > 0) return false;

		ItemStack result = ShredderRecipes.getShredderResult(slots[0]);
		if(result == null) return false;
		if(slots[1] == null) return true;
		if(!slots[1].isItemEqual(result)) return false;
		return slots[1].stackSize + result.stackSize <= slots[1].getMaxStackSize();
	}

	@Override
	protected void processItem() {
		if(!canProcess()) return;
		ItemStack result = ShredderRecipes.getShredderResult(slots[0]).copy();
		if(slots[1] == null) {
			slots[1] = result;
		} else if(slots[1].isItemEqual(result)) {
			slots[1].stackSize += result.stackSize;
		}

		if(worldObj.rand.nextInt(2) == 0) {
			if(slots[2].getMaxDamage() > 0) slots[2].setItemDamage(slots[2].getItemDamage() + 1);
			if(slots[3].getMaxDamage() > 0) slots[3].setItemDamage(slots[3].getItemDamage() + 1);
		}

		this.decrStackSize(0, 1);
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0) return ShredderRecipes.getShredderResult(stack) != null;
		if(slot == 2 || slot == 3) return stack != null && stack.getItem() instanceof ItemBlades;
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		if(side == ForgeDirection.UP.ordinal()) return new int[] { 0 };
		if(side == ForgeDirection.DOWN.ordinal()) return new int[] { 1 };
		return new int[] { 0, 1, 2, 3 };
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		if(slot == 1) return true;
		return false;
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
		return new ContainerSteamShredder(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUISteamShredder(player.inventory, this);
	}
}
