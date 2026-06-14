package com.hbm.tileentity.machine.elevator;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.TileEntityLoadedBase;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityElevatorFloor extends TileEntityLoadedBase {

	public int doorBottomY = -1;

	@Override
	public void updateEntity() {
		if (!worldObj.isRemote && worldObj.getTotalWorldTime() % 20 == 0) {
			checkPlatformPresence();
		}
	}

	private boolean platformPresent = false;

	public boolean isPlatformPresent() {
		return platformPresent;
	}

	private void checkPlatformPresence() {
		boolean wasPresent = platformPresent;
		platformPresent = false;

		int checkY = yCoord;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (worldObj.getBlock(xCoord + dx, checkY, zCoord + dz) == ModBlocks.elevator_platform) {
					platformPresent = true;
					break;
				}
			}
			if (platformPresent) break;
		}

		if (wasPresent != platformPresent) {
			worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, this.getBlockType());
			markDirty();
		}
	}

	public void spawnDoor() {
		if (worldObj == null || worldObj.isRemote) return;

		int bottom = yCoord + 1;
		int top	= yCoord + 2;
		this.doorBottomY = bottom;

		if (worldObj.isAirBlock(xCoord, bottom, zCoord)) {
			worldObj.setBlock(xCoord, bottom, zCoord, ModBlocks.elevator_door, 0, 3);
		}
		if (worldObj.isAirBlock(xCoord, top, zCoord)) {
			worldObj.setBlock(xCoord, top, zCoord, ModBlocks.elevator_door, 0, 3);
		}

		markDirty();
	}

	public void removeDoor() {
		if (worldObj == null || worldObj.isRemote) return;
		if (doorBottomY < 0) return;

		for (int dy = 0; dy <= 1; dy++) {
			int by = doorBottomY + dy;
			if (worldObj.getBlock(xCoord, by, zCoord) == ModBlocks.elevator_door) {
				worldObj.setBlockToAir(xCoord, by, zCoord);
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("doorBottomY", doorBottomY);
		nbt.setBoolean("platformPresent", platformPresent);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		doorBottomY = nbt.hasKey("doorBottomY") ? nbt.getInteger("doorBottomY") : -1;
		platformPresent = nbt.getBoolean("platformPresent");
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(doorBottomY);
		buf.writeBoolean(platformPresent);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		doorBottomY = buf.readInt();
		platformPresent = buf.readBoolean();
	}
}
