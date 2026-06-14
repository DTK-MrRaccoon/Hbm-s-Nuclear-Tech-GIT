package com.hbm.tileentity.machine.elevator;

import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.IRORValueProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityElevatorDoor extends TileEntityLoadedBase implements IRORInteractive, IRORValueProvider {

	public int doorState = 1;

	public int autoCloseTicks = 0;

	public static final int AUTO_CLOSE_TICKS = 60; // 3 seconds

	public int floorY = -1;

	@Override
	public void updateEntity() {
		if (worldObj.isRemote) return;

		if (doorState == 0 && autoCloseTicks > 0) {
			autoCloseTicks--;
			if (autoCloseTicks <= 0) {
				closeDoor();
			}
		}

		if (worldObj.getTotalWorldTime() % 10 == 0) {
			networkPackNT(32);
		}
	}

	public void openDoor(boolean platformAtFloor) {
		if (!platformAtFloor) return;
		if (doorState == 1) {
			doorState = 0;
			autoCloseTicks = AUTO_CLOSE_TICKS;
			updateBlockMeta();
			networkPackNT(32);
		}
	}

	public void closeDoor() {
		if (doorState == 0) {
			doorState = 1;
			autoCloseTicks = 0;
			updateBlockMeta();
			networkPackNT(32);
		}
	}

	private void updateBlockMeta() {
		if (worldObj == null) return;
		int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);

		if (doorState == 0) {
			meta |= 4;
		} else {
			meta &= ~4;
		}
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, meta, 3);
	}

	@Override
	public String[] getFunctionInfo() {
		return new String[] {
			PREFIX_FUNCTION + "open",
			PREFIX_FUNCTION + "close",
			PREFIX_VALUE + "doorState",
		};
	}

	@Override
	public String runRORFunction(String name, String[] params) {
		switch (name) {
			case "open":
				boolean platformHere = isPlatformAtThisFloor();
				openDoor(platformHere);
				return "doorState:" + doorState;
			case "close":
				closeDoor();
				return "doorState:" + doorState;
		}
		return null;
	}

	@Override
	public String provideRORValue(String name) {
		if ((PREFIX_VALUE + "doorState").equals(name)) {
			return String.valueOf(doorState);
		}
		return null;
	}

	private boolean isPlatformAtThisFloor() {
		int checkY = yCoord; // platform posY ≈ yCoord when at this floor
		AxisAlignedBB searchBox = AxisAlignedBB.getBoundingBox(
			xCoord - 8, checkY - 1, zCoord - 8,
			xCoord + 8, checkY + 2, zCoord + 8
		);
		@SuppressWarnings("unchecked")
		java.util.List<EntityElevatorPlatform> platforms = worldObj.getEntitiesWithinAABB(
			EntityElevatorPlatform.class, searchBox
		);
		return !platforms.isEmpty();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("doorState", doorState);
		nbt.setInteger("autoCloseTicks", autoCloseTicks);
		nbt.setInteger("floorY", floorY);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		doorState = nbt.getInteger("doorState");
		if (!nbt.hasKey("doorState")) doorState = 1;
		autoCloseTicks = nbt.getInteger("autoCloseTicks");
		floorY = nbt.getInteger("floorY");
		if (!nbt.hasKey("floorY")) floorY = -1;
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(doorState);
		buf.writeInt(autoCloseTicks);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		doorState = buf.readInt();
		autoCloseTicks = buf.readInt();
	}
}
