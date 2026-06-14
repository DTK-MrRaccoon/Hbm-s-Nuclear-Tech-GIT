package com.hbm.tileentity.machine.elevator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.inventory.container.elevator.ContainerElevatorController;
import com.hbm.inventory.gui.elevator.GUIElevatorController;

import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.IRORValueProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityElevatorController extends TileEntityMachineBase implements IEnergyReceiverMK2, IRORInteractive, IRORValueProvider, IGUIProvider, IControlReceiver {

	public long power = 0;
	public static final long MAX_POWER = 100_000L;

	public int platformSize = 1;

	public List<Integer> floors = new ArrayList<>();

	public int targetFloor = 0;

	public boolean hasPlatform = false;

	private transient EntityElevatorPlatform platformEntity = null;

	public TileEntityElevatorController() {
		super(0);
	}

	@Override
	public String getName() { return "Elevator Controller"; }

	@Override
	public void updateEntity() {
		if (worldObj.isRemote) return;

		maintainPlatform();

		if (worldObj.getTotalWorldTime() % 10 == 0) {
			networkPackNT(64);
		}
	}

	private void maintainPlatform() {
		if (!hasPlatform) return;

		if (platformEntity == null || platformEntity.isDead) {
			platformEntity = findPlatformEntity();
		}

		if (platformEntity == null || platformEntity.isDead) {
			spawnPlatform();
		}
	}

	@SuppressWarnings("unchecked")
	private EntityElevatorPlatform findPlatformEntity() {
		AxisAlignedBB searchBox = AxisAlignedBB.getBoundingBox(
			xCoord - 1, 0, zCoord - 1,
			xCoord + 2, 256, zCoord + 2
		);
		List<EntityElevatorPlatform> entities = worldObj.getEntitiesWithinAABB(
			EntityElevatorPlatform.class, searchBox
		);
		for (EntityElevatorPlatform e : entities) {
			if (e.controllerX == xCoord && e.controllerY == yCoord && e.controllerZ == zCoord) {
				return e;
			}
		}
		return null;
	}

	public void spawnPlatform() {
		if (worldObj.isRemote) return;
		double spawnY = yCoord + 1;
		if (!floors.isEmpty()) spawnY = floors.get(0) + 1;

		EntityElevatorPlatform platform = new EntityElevatorPlatform(worldObj);
		platform.setControllerPos(xCoord, yCoord, zCoord);
		platform.setPlatformSize(platformSize);
		platform.setPosition(xCoord + 0.5, spawnY, zCoord + 0.5);
		platform.targetY = spawnY;
		if (!floors.isEmpty()) {
			platform.setHighestFloorY(floors.get(floors.size() - 1));
		}
		worldObj.spawnEntityInWorld(platform);
		platformEntity = platform;
		hasPlatform = true;

		if (!floors.isEmpty()) {
			for (int i = 0; i < floors.size(); i++) {
				if (Math.abs(spawnY - (floors.get(i) + 1)) < 0.5) {
					platform.parkedAtFloor = i;
					platform.removeDoorBlocksAtFloor(floors.get(i));
					platform.spawnPlatformBlocks();
					break;
				}
			}
		}

		networkPackNT(64);
	}

	public void despawnPlatform() {
		if (platformEntity != null && !platformEntity.isDead) {
			platformEntity.setDead();
		}
		platformEntity = null;
		hasPlatform = false;
		networkPackNT(64);
	}

	public void onControllerDestroyed() {
		despawnPlatform();
	}

	public void scanForFloors() {
		floors.clear();
		int scanRadius = 5;
		for (int dy = -128; dy <= 128; dy++) {
			int fy = yCoord + dy;
			if (fy < 0 || fy > 255) continue;
			for (int dx = -scanRadius; dx <= scanRadius; dx++) {
				for (int dz = -scanRadius; dz <= scanRadius; dz++) {
					if (worldObj.getBlock(xCoord + dx, fy, zCoord + dz) == ModBlocks.elevator_floor) {
						if (!floors.contains(fy)) floors.add(fy);
					}
				}
			}
		}
		Collections.sort(floors);
		markDirty();
		networkPackNT(64);
	}

	public int getFloorCount() { return floors.size(); }

	public void cyclePlatformSize() {
		if (platformSize == 1) platformSize = 3;
		else if (platformSize == 3) platformSize = 5;
		else if (platformSize == 5) platformSize = 7;
		else if (platformSize == 7) platformSize = 9;
		else platformSize = 1;

		if (platformEntity != null && !platformEntity.isDead) {
			platformEntity.setPlatformSize(platformSize);
		}
		markDirty();
		networkPackNT(64);
	}

	public int getPlatformSize() { return platformSize; }

	public void goToFloor(int floorIndex) {
		if (floorIndex < 0 || floorIndex >= floors.size()) return;
		if (power <= 0) return;
		targetFloor = floorIndex;
		if (platformEntity != null && !platformEntity.isDead) {
			// Platform surface stops 1 block ABOVE the floor block
			platformEntity.setTargetY(floors.get(floorIndex) + 1);
		}
		networkPackNT(64);
	}

	@Override public long getPower() { return power; }
	@Override public void setPower(long p) { this.power = Math.max(0, Math.min(p, MAX_POWER)); }
	@Override public long getMaxPower() { return MAX_POWER; }
	@Override public boolean canConnect(ForgeDirection dir) { return true; }

	@Override
	public String[] getFunctionInfo() {
		return new String[] {
			PREFIX_FUNCTION + "goto!floor",
			PREFIX_FUNCTION + "spawn",
			PREFIX_FUNCTION + "despawn",
			PREFIX_FUNCTION + "stop",
			PREFIX_VALUE + "floor",
			PREFIX_VALUE + "floors",
			PREFIX_VALUE + "power",
		};
	}

	@Override
	public String runRORFunction(String name, String[] params) {
		if ((PREFIX_FUNCTION + "goto").equals(name) && params.length >= 1) {
			try {
				int floor = Integer.parseInt(params[0]);
				if (floor < 0 || floor >= floors.size()) {
					return "Floor " + floor + " out of range (0-" + (floors.size() - 1) + ")";
				}
				if (!hasPlatform || platformEntity == null || platformEntity.isDead) {
					return "No platform";
				}
				goToFloor(floor);
				return "Going to floor " + floor;
			} catch (Exception e) {
				return "Invalid floor: " + params[0];
			}
		}

		if ((PREFIX_FUNCTION + "spawn").equals(name)) {
			spawnPlatform();
			return "Platform spawned";
		}

		if ((PREFIX_FUNCTION + "despawn").equals(name)) {
			despawnPlatform();
			return "Platform despawned";
		}

		if ((PREFIX_FUNCTION + "stop").equals(name)) {
			if (platformEntity != null && !platformEntity.isDead) {
				platformEntity.targetY = platformEntity.posY;
				platformEntity.isMoving = false;
				platformEntity.currentSpeed = 0.0;
				return "Emergency stop";
			}
			return "No platform";
		}

		return null;
	}

	@Override
	public String provideRORValue(String name) {
		if ((PREFIX_VALUE + "floor").equals(name))  return String.valueOf(targetFloor);
		if ((PREFIX_VALUE + "floors").equals(name)) return String.valueOf(floors.size());
		if ((PREFIX_VALUE + "power").equals(name))  return String.valueOf(power);
		return null;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerElevatorController(player.inventory, this);
	}

	@Override
	@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIElevatorController(player.inventory, this);
	}

	@Override
	public boolean hasPermission(EntityPlayer player) { return true; }

	@Override
	public void receiveControl(NBTTagCompound data) {
		if (data.hasKey("goFloor"))  goToFloor(data.getInteger("goFloor"));
		if (data.hasKey("spawn")   && data.getBoolean("spawn"))   spawnPlatform();
		if (data.hasKey("despawn") && data.getBoolean("despawn")) despawnPlatform();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		nbt.setInteger("platformSize", platformSize);
		nbt.setInteger("targetFloor", targetFloor);
		nbt.setBoolean("hasPlatform", hasPlatform);

		NBTTagList floorList = new NBTTagList();
		for (int f : floors) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("y", f);
			floorList.appendTag(tag);
		}
		nbt.setTag("floors", floorList);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		power = nbt.getLong("power");
		platformSize = nbt.getInteger("platformSize");
		if (platformSize == 0) platformSize = 1;
		targetFloor = nbt.getInteger("targetFloor");
		hasPlatform = nbt.getBoolean("hasPlatform");

		floors.clear();
		NBTTagList floorList = nbt.getTagList("floors", 10);
		for (int i = 0; i < floorList.tagCount(); i++) {
			floors.add(floorList.getCompoundTagAt(i).getInteger("y"));
		}
		Collections.sort(floors);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeInt(platformSize);
		buf.writeInt(targetFloor);
		buf.writeBoolean(hasPlatform);
		buf.writeInt(floors.size());
		for (int f : floors) buf.writeInt(f);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
		platformSize = buf.readInt();
		targetFloor = buf.readInt();
		hasPlatform = buf.readBoolean();
		int count = buf.readInt();
		floors.clear();
		for (int i = 0; i < count; i++) floors.add(buf.readInt());
	}

	@Override public int[] getAccessibleSlotsFromSide(int side) { return new int[0]; }
	@Override public boolean canInsertItem(int slot, net.minecraft.item.ItemStack stack, int side) { return false; }
	@Override public boolean canExtractItem(int slot, net.minecraft.item.ItemStack stack, int side) { return false; }
}
