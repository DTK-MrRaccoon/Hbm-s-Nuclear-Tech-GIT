package com.hbm.tileentity.machine.elevator;

import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityElevatorPlatform extends Entity {

	public int controllerX, controllerY, controllerZ;
	public int platformSize = 1;

	public double targetY = 0;

	public double highestFloorY = 0;

	public static final double MAX_SPEED = 0.2;
	public static final double ACCELERATION = 0.03;
	public static final long   POWER_PER_TICK = 25000L;

	public boolean isMoving = false;
	public double currentSpeed = 0.0;

	public int parkedAtFloor = -1;

	private ElevatorSolidEntity solidEntity = null;
	private boolean solidSpawned = false;

	private java.util.Set<Long> platformBlockPositions = new java.util.HashSet<>();

	public EntityElevatorPlatform(World world) {
		super(world);
		this.noClip = true;
		this.isImmuneToFire = true;
		this.setSize(1.0F, 0.25F);
	}

	@Override
	protected void entityInit() {
		this.dataWatcher.addObject(20, 1);
		this.dataWatcher.addObject(21, 0.0F);
		this.dataWatcher.addObject(22, 0);
		this.dataWatcher.addObject(23, 0);
		this.dataWatcher.addObject(24, 0);
		this.dataWatcher.addObject(25, 0.0F);
	}

	public void setControllerPos(int x, int y, int z) {
		this.controllerX = x;
		this.controllerY = y;
		this.controllerZ = z;
		this.dataWatcher.updateObject(22, x);
		this.dataWatcher.updateObject(23, y);
		this.dataWatcher.updateObject(24, z);
	}

	public void setPlatformSize(int size) {
		this.platformSize = size;
		this.dataWatcher.updateObject(20, size);
		this.setSize(size, 0.25F);
		if (solidEntity != null && !solidEntity.isDead) {
			solidEntity.setSize(size, 0.25F);
		}
	}

	public void setTargetY(double y) {
		this.targetY = y;
		this.dataWatcher.updateObject(21, (float) y);
		this.isMoving = true;
	}

	public void setHighestFloorY(double y) {
		this.highestFloorY = y;
		this.dataWatcher.updateObject(25, (float) y);
	}

	@Override
	public boolean interactFirst(EntityPlayer player) {
		if (worldObj.isRemote) return true;

		TileEntity te = worldObj.getTileEntity(controllerX, controllerY, controllerZ);
		if (!(te instanceof TileEntityElevatorController)) {
			return false;
		}

		player.openGui(MainRegistry.instance, 0, worldObj, controllerX, controllerY, controllerZ);
		return true;
	}

	@Override
	public void onEntityUpdate() {
		if (!worldObj.isRemote) {
			updateServerSide();
		} else {
			this.platformSize  = this.dataWatcher.getWatchableObjectInt(20);
			this.targetY	   = this.dataWatcher.getWatchableObjectFloat(21);
			this.controllerX   = this.dataWatcher.getWatchableObjectInt(22);
			this.controllerY   = this.dataWatcher.getWatchableObjectInt(23);
			this.controllerZ   = this.dataWatcher.getWatchableObjectInt(24);
			this.highestFloorY = this.dataWatcher.getWatchableObjectFloat(25);
		}
		updateBoundingBox();
	}

	private void updateBoundingBox() {
		float half = platformSize / 2.0F;
		this.boundingBox.setBounds(
			this.posX - half, this.posY,
			this.posZ - half,
			this.posX + half, this.posY + 0.25,
			this.posZ + half
		);
	}

	private void updateServerSide() {
		TileEntity te = worldObj.getTileEntity(controllerX, controllerY, controllerZ);
		if (!(te instanceof TileEntityElevatorController)) {
			killSolid();
			this.setDead();
			return;
		}
		TileEntityElevatorController ctrl = (TileEntityElevatorController) te;

		if (!ctrl.floors.isEmpty()) {
			double hfy = ctrl.floors.get(ctrl.floors.size() - 1);
			if (hfy != highestFloorY) setHighestFloorY(hfy);
		}

		double dy = targetY - this.posY;

		if (Math.abs(dy) > 0.02) {
			if (parkedAtFloor >= 0) {
				removePlatformBlocks();
				teleportRidersUp();
				respawnDoorsAtFloor(ctrl, parkedAtFloor);
				parkedAtFloor = -1;
			}

			ensureSolidEntity();

			if (ctrl.power < 200L) {
				isMoving = false;
				currentSpeed = 0.0;
				return;
			}
			ctrl.setPower(ctrl.power - 200L);
			isMoving = true;

			double distanceLeft = Math.abs(dy);
			double brakingDistance = (currentSpeed * currentSpeed) / (2.0 * 0.05);

			if (distanceLeft < brakingDistance + 0.5) {
				currentSpeed = Math.max(0.01, currentSpeed - 0.05);
			} else {
				currentSpeed = Math.min(0.2, currentSpeed + 0.03);
			}

			double move = Math.min(currentSpeed, distanceLeft) * Math.signum(dy);

			moveRidersWithPlatform(move);

			this.setPosition(this.posX, this.posY + move, this.posZ);
			moveSolidEntity();

		} else {
			if (isMoving) {
				this.setPosition(this.posX, targetY, this.posZ);
				moveSolidEntity();
				isMoving = false;
				currentSpeed = 0.0;
				handleArrival(ctrl);
			}
		}
	}

	private void ensureSolidEntity() {
		if (!solidSpawned || solidEntity == null || solidEntity.isDead) {
			solidEntity = new ElevatorSolidEntity(worldObj, this);
			solidEntity.setSize(platformSize, 0.25F);
			solidEntity.setPosition(this.posX, this.posY, this.posZ);
			float halfWidth = platformSize / 2.0F;
			solidEntity.boundingBox.setBounds(
				this.posX - halfWidth, this.posY,
				this.posZ - halfWidth,
				this.posX + halfWidth, this.posY + 0.25,
				this.posZ + halfWidth
			);
			worldObj.spawnEntityInWorld(solidEntity);
			solidSpawned = true;
		}
	}

	private void moveSolidEntity() {
		if (solidEntity != null && !solidEntity.isDead) {
			solidEntity.setPosition(this.posX, this.posY, this.posZ);
			float halfWidth = platformSize / 2.0F;
			solidEntity.boundingBox.setBounds(
				this.posX - halfWidth, this.posY,
				this.posZ - halfWidth,
				this.posX + halfWidth, this.posY + 0.25,
				this.posZ + halfWidth
			);
		}
	}

	private void killSolid() {
		if (solidEntity != null && !solidEntity.isDead) {
			solidEntity.setDead();
		}
		solidEntity = null;
		solidSpawned = false;
	}

	@Override
	public void setDead() {
		removePlatformBlocks();
		killSolid();
		super.setDead();
	}

	@SuppressWarnings("unchecked")
	private void moveRidersWithPlatform(double dy) {
		if (solidEntity == null || solidEntity.isDead) return;
		float half = platformSize / 2.0F;
		AxisAlignedBB riderBox = AxisAlignedBB.getBoundingBox(
			this.posX - half, this.posY + 0.2, this.posZ - half,
			this.posX + half, this.posY + 0.7, this.posZ + half
		);
		List<Entity> riders = worldObj.getEntitiesWithinAABBExcludingEntity(solidEntity, riderBox);
		for (Entity e : riders) {
			if (e == this) continue;
			e.setPosition(e.posX, e.posY + dy, e.posZ);
		}
	}

	private void handleArrival(TileEntityElevatorController ctrl) {
		killSolid();

		for (int i = 0; i < ctrl.floors.size(); i++) {
			int floorY = ctrl.floors.get(i);
			boolean atThisFloor = Math.abs(this.posY - (floorY + 1)) < 0.5;
			if (atThisFloor) {
				parkedAtFloor = i;
				removeDoorBlocksAtFloor(floorY);
				spawnPlatformBlocks();
			} else {
				ensureDoorsClosedAtFloor(ctrl, floorY);
			}
		}
	}

	public void spawnPlatformBlocks() {
		removePlatformBlocks();

		int half = platformSize / 2;
		int px = (int) Math.floor(this.posX);
		int py = (int) Math.floor(this.posY) - 1;
		int pz = (int) Math.floor(this.posZ);

		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				int bx = px + dx;
				int bz = pz + dz;

				if (worldObj.isAirBlock(bx, py, bz)) {
					worldObj.setBlock(bx, py, bz, ModBlocks.elevator_platform, 0, 2);
					platformBlockPositions.add(packPosition(bx, py, bz));
				}
			}
		}
	}

	private void removePlatformBlocks() {
		for (Long packed : platformBlockPositions) {
			int[] pos = unpackPosition(packed);
			if (worldObj.getBlock(pos[0], pos[1], pos[2]) == ModBlocks.elevator_platform) {
				worldObj.setBlockToAir(pos[0], pos[1], pos[2]);
			}
		}
		platformBlockPositions.clear();
	}

	private long packPosition(int x, int y, int z) {
		return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | ((long) z & 0x3FFFFFF);
	}

	private int[] unpackPosition(long packed) {
		int x = (int) (packed >> 38);
		int y = (int) ((packed >> 26) & 0xFFF);
		int z = (int) (packed << 38 >> 38);
		return new int[] { x, y, z };
	}

	@SuppressWarnings("unchecked")
	private void teleportRidersUp() {
		AxisAlignedBB checkBox = AxisAlignedBB.getBoundingBox(
			this.posX - 2.5, this.posY - 2.0, this.posZ - 2.5,
			this.posX + 2.5, this.posY + 2.0, this.posZ + 2.5
		);
		List<Entity> entities = worldObj.getEntitiesWithinAABBExcludingEntity(this, checkBox);
		for (Entity e : entities) {
			if (e == solidEntity) continue;

			if (e instanceof EntityPlayer || e instanceof net.minecraft.entity.EntityLiving) {
				e.setPosition(e.posX, e.posY + 1.0, e.posZ);
				e.motionY = 0;
				e.fallDistance = 0;
			}
		}
	}

	public void removeDoorBlocksAtFloor(int floorY) {
		int scanR = (platformSize / 2) + 2;
		int px = (int) Math.floor(this.posX);
		int pz = (int) Math.floor(this.posZ);
		for (int doorY = floorY + 1; doorY <= floorY + 2; doorY++) {
			for (int dx = -scanR; dx <= scanR; dx++) {
				for (int dz = -scanR; dz <= scanR; dz++) {
					int bx = px + dx, bz = pz + dz;
					if (worldObj.getBlock(bx, doorY, bz) == ModBlocks.elevator_door) {
						worldObj.setBlockToAir(bx, doorY, bz);
					}
				}
			}
		}
	}

	private void respawnDoorsAtFloor(TileEntityElevatorController ctrl, int floorIndex) {
		if (floorIndex < 0 || floorIndex >= ctrl.floors.size()) return;
		int floorY = ctrl.floors.get(floorIndex);
		int scanR = (platformSize / 2) + 2;
		int px = (int) Math.floor(this.posX);
		int pz = (int) Math.floor(this.posZ);
		for (int dx = -scanR; dx <= scanR; dx++) {
			for (int dz = -scanR; dz <= scanR; dz++) {
				int bx = px + dx, bz = pz + dz;
				if (worldObj.getBlock(bx, floorY, bz) == ModBlocks.elevator_floor) {
					TileEntity fte = worldObj.getTileEntity(bx, floorY, bz);
					if (fte instanceof TileEntityElevatorFloor) {
						((TileEntityElevatorFloor) fte).spawnDoor();
					}
				}
			}
		}
	}

	private void ensureDoorsClosedAtFloor(TileEntityElevatorController ctrl, int floorY) {
		int scanR = (platformSize / 2) + 2;
		int px = (int) Math.floor(this.posX);
		int pz = (int) Math.floor(this.posZ);
		for (int dx = -scanR; dx <= scanR; dx++) {
			for (int dz = -scanR; dz <= scanR; dz++) {
				int bx = px + dx, bz = pz + dz;
				if (worldObj.getBlock(bx, floorY, bz) == ModBlocks.elevator_floor) {
					TileEntity fte = worldObj.getTileEntity(bx, floorY, bz);
					if (fte instanceof TileEntityElevatorFloor) {
						TileEntityElevatorFloor floorTE = (TileEntityElevatorFloor) fte;
						if (worldObj.getBlock(bx, floorY + 1, bz) != ModBlocks.elevator_door) {
							floorTE.spawnDoor();
						}
					}
				}
			}
		}
	}

	@Override public boolean canBePushed()	  { return false; }
	@Override public boolean canBeCollidedWith() { return true; }

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		controllerX   = nbt.getInteger("ctrlX");
		controllerY   = nbt.getInteger("ctrlY");
		controllerZ   = nbt.getInteger("ctrlZ");
		platformSize  = nbt.getInteger("platformSize");
		if (platformSize == 0) platformSize = 1;
		targetY	   = nbt.getDouble("targetY");
		highestFloorY = nbt.getDouble("highestFloorY");
		isMoving	  = nbt.getBoolean("isMoving");
		currentSpeed  = nbt.getDouble("currentSpeed");
		parkedAtFloor = nbt.hasKey("parkedAtFloor") ? nbt.getInteger("parkedAtFloor") : -1;
		setControllerPos(controllerX, controllerY, controllerZ);
		setPlatformSize(platformSize);
		setHighestFloorY(highestFloorY);

		if (nbt.hasKey("platformBlocks")) {
			NBTTagCompound blocksTag = nbt.getCompoundTag("platformBlocks");
			int count = blocksTag.getInteger("count");
			for (int i = 0; i < count; i++) {
				platformBlockPositions.add(blocksTag.getLong("pos" + i));
			}
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		nbt.setInteger("ctrlX",		 controllerX);
		nbt.setInteger("ctrlY",		 controllerY);
		nbt.setInteger("ctrlZ",		 controllerZ);
		nbt.setInteger("platformSize",  platformSize);
		nbt.setDouble("targetY",		targetY);
		nbt.setDouble("highestFloorY",  highestFloorY);
		nbt.setBoolean("isMoving",	  isMoving);
		nbt.setDouble("currentSpeed",   currentSpeed);
		nbt.setInteger("parkedAtFloor", parkedAtFloor);

		NBTTagCompound blocksTag = new NBTTagCompound();
		blocksTag.setInteger("count", platformBlockPositions.size());
		int i = 0;
		for (Long pos : platformBlockPositions) {
			blocksTag.setLong("pos" + i, pos);
			i++;
		}
		nbt.setTag("platformBlocks", blocksTag);
	}

	public static class ElevatorSolidEntity extends Entity {

		public EntityElevatorPlatform platform;

		public ElevatorSolidEntity(World world) {
			super(world);
			this.noClip = false;
			this.isImmuneToFire = true;
			this.setSize(1.0F, 0.25F);
		}

		public ElevatorSolidEntity(World world, EntityElevatorPlatform platform) {
			this(world);
			this.platform = platform;
		}

		@Override
		protected void entityInit() { }

		@Override
		public void setSize(float width, float height) {
			super.setSize(width, height);
			updateBoundingBox();
		}

		@Override
		public void setPosition(double x, double y, double z) {
			super.setPosition(x, y, z);
			updateBoundingBox();
		}

		private void updateBoundingBox() {
			float halfWidth = this.width / 2.0F;
			this.boundingBox.setBounds(
				this.posX - halfWidth, this.posY,
				this.posZ - halfWidth,
				this.posX + halfWidth, this.posY + this.height,
				this.posZ + halfWidth
			);
		}

		@Override
		public void onEntityUpdate() {
			if (!worldObj.isRemote && (platform == null || platform.isDead)) {
				this.setDead();
			}
			updateBoundingBox();
		}

		@Override
		public boolean canBeCollidedWith() { return !this.isDead; }

		@Override
		public AxisAlignedBB getCollisionBox(Entity entity) {
			return this.boundingBox;
		}

		@Override
		public AxisAlignedBB getBoundingBox() {
			return this.boundingBox;
		}

		@Override
		public boolean canBePushed() { return false; }

		@Override
		public boolean interactFirst(EntityPlayer player) {
			if (platform != null && !platform.isDead) {
				return platform.interactFirst(player);
			}
			return false;
		}

		@Override
		public boolean attackEntityFrom(DamageSource source, float amount) {
			return false;
		}

		@Override
		public boolean writeToNBTOptional(NBTTagCompound nbt) { return false; }

		@Override
		protected void readEntityFromNBT(NBTTagCompound nbt) { this.setDead(); }

		@Override
		protected void writeEntityToNBT(NBTTagCompound nbt) { }
	}
}
