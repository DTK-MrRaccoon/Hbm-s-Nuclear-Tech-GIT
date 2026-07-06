package com.hbm.tileentity.machine.steam;

import com.hbm.inventory.container.ContainerSteamShredder;
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
	public final int maxSpeed = 100;

	public TileEntitySteamShredder() {
		super(4, 16000, 160);
	}

	@Override
	public String getName() {
		return "container.steamShredder";
	}

	@Override
	protected int getRequiredSteamPerTick() {
		return 333;
	}

	@Override
	protected void updateMachineMetrics(boolean isProcessing, int steamAvailable) {
		if(isProcessing && steamAvailable > 0) {
			int targetSpeed = (int) (maxSpeed * ((double) steamAvailable / 333.0D));

			if(speed < targetSpeed) {
				speed++;
			} else if(speed > targetSpeed) {
				speed--;
			}

			if(speed > maxSpeed) speed = maxSpeed;
			if(speed < 0) speed = 0;

			progress += Math.max(1, speed / 20);
			if(progress >= maxProgress) {
				progress = 0;
				processItem();
				this.markDirty();
			}
		} else {
			if(speed > 0) speed -= 2;
			if(speed < 0) speed = 0;
			if(progress > 0) progress--;
		}
	}

	@Override
	protected void serializeMachine(ByteBuf buf) {
		buf.writeInt(speed);
	}

	@Override
	protected void deserializeMachine(ByteBuf buf) {
		this.speed = buf.readInt();
	}

	@Override
	protected void readMachineNBT(NBTTagCompound nbt) {
		this.speed = nbt.getInteger("speed");
	}

	@Override
	protected void writeMachineNBT(NBTTagCompound nbt) {
		nbt.setInteger("pressure", speed);
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
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerSteamShredder(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUISteamShredder(player.inventory, this);
	}
}
