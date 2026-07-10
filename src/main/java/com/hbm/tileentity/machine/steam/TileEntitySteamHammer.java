package com.hbm.tileentity.machine.steam;

import com.hbm.inventory.container.ContainerSteamHammer;
import com.hbm.inventory.gui.GUISteamHammer;
import com.hbm.inventory.recipes.HammerRecipes;
import com.hbm.items.tool.ItemTieredHammer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntitySteamHammer extends TileEntitySteamMachineBase {

	public boolean bronze = false;
	public int pressure = 0;
	public int maxPressure;

	public TileEntitySteamHammer() {
		this(false);
	}

	public TileEntitySteamHammer(boolean bronze) {
		super(3, bronze ? 8000 : 16000, bronze ? 80 : 160);
		this.bronze = bronze;
		this.applyBronzeStats();
	}

	public boolean isBronze() {
		return this.bronze;
	}

	private void applyBronzeStats() {
		this.maxPressure = this.bronze ? 50 : 100;
		this.maxProgress = this.bronze ? 200 : 100;
		if(this.pressure > this.maxPressure) this.pressure = this.maxPressure;
		if(this.pressure < 0) this.pressure = 0;
	}

	@Override
	public String getName() {
		return this.bronze ? "container.steamHammerBronze" : "container.steamHammer";
	}

	@Override
	protected int getRequiredSteamPerTick() {
		return this.bronze ? 100 : 50;
	}

	@Override
	protected void updateMachineMetrics(boolean isProcessing, int steamAvailable) {
		if(isProcessing && steamAvailable > 0) {
			int targetPressure = Math.max(1, (steamAvailable * this.maxPressure) / Math.max(1, getRequiredSteamPerTick()));
			if(this.pressure < targetPressure) {
				this.pressure += 2;
			} else if(this.pressure > targetPressure) {
				this.pressure--;
			}
			if(this.pressure > this.maxPressure) this.pressure = this.maxPressure;
			if(this.pressure < 0) this.pressure = 0;
		} else if(this.pressure > 0) {
			this.pressure--;
		}
	}

	@Override
	protected float getProgressIncrement(boolean isProcessing, int steamAvailable) {
		if(isProcessing && steamAvailable > 0) {
			float steamFactor = (float) steamAvailable / (float) getRequiredSteamPerTick();
			float pressureFactor = (float) this.pressure / (float) Math.max(1, this.maxPressure);
			return steamFactor * pressureFactor;
		}
		return 0.0F;
	}

	@Override
	protected void serializeMachine(ByteBuf buf) {
		buf.writeBoolean(this.bronze);
		buf.writeInt(this.pressure);
	}

	@Override
	protected void deserializeMachine(ByteBuf buf) {
		this.bronze = buf.readBoolean();
		this.pressure = buf.readInt();
		this.applyBronzeStats();
	}

	@Override
	protected void readMachineNBT(NBTTagCompound nbt) {
		this.bronze = nbt.getBoolean("bronze");
		this.pressure = nbt.getInteger("pressure");
		this.applyBronzeStats();
	}

	@Override
	protected void writeMachineNBT(NBTTagCompound nbt) {
		nbt.setBoolean("bronze", this.bronze);
		nbt.setInteger("pressure", this.pressure);
	}

	@Override
	protected boolean canProcess() {
		if(slots[0] == null || slots[1] == null) return false;
		if(!(slots[0].getItem() instanceof ItemTieredHammer)) return false;

		ItemStack result = HammerRecipes.getOutput(slots[1], slots[0]);
		if(result == null) return false;

		if(slots[2] == null) return true;
		if(!slots[2].isItemEqual(result)) return false;
		return slots[2].stackSize + result.stackSize <= slots[2].getMaxStackSize();
	}

	@Override
	protected void processItem() {
		if(!canProcess()) return;

		ItemStack result = HammerRecipes.getOutput(slots[1], slots[0]).copy();
		if(slots[2] == null) {
			slots[2] = result;
		} else if(slots[2].isItemEqual(result)) {
			slots[2].stackSize += result.stackSize;
		}

		if(slots[0] != null) {
			ItemStack damaged = slots[0].getItem().getContainerItem(slots[0]);
			slots[0] = damaged;
		}

		decrStackSize(1, 1);
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(slot == 0) return stack != null && stack.getItem() instanceof ItemTieredHammer;
		if(slot == 1) return stack != null;
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		if(side == ForgeDirection.UP.ordinal()) return new int[] { 0 };
		if(side == ForgeDirection.DOWN.ordinal()) return new int[] { 2 };
		return new int[] { 1 };
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot == 2;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerSteamHammer(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUISteamHammer(player.inventory, this);
	}
}
