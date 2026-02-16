package com.hbm.tileentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.ModItems;
import com.hbm.tileentity.IRepairable;
import com.hbm.util.ParticleUtil;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk2.Nodespace;
import api.hbm.energymk2.Nodespace.PowerNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityTransformerMedium extends TileEntityPylonBase implements IRepairable {

	public float temperature = 20F;
	public boolean isBroken = false;
	public int burnTime = 0;
	public int brokenTime = 0;
	public long powerTransfer = 0;

	public static final long MAX_POWER = 2_500_000L;
	public static final float MAX_TEMP = 500F;

	@Override
	public void updateEntity() {
		super.updateEntity();

		if (!worldObj.isRemote) {
			if (isBroken) {

				powerTransfer = 0;

				if(this.node != null) {
					Nodespace.destroyNode(worldObj, xCoord, yCoord, zCoord);
					this.node = null;
				}

				if (burnTime > 0) {
					burnTime--;
					if (worldObj.getTotalWorldTime() % 20 == 0) {
						List<Entity> affected = worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(xCoord - 1.5, yCoord, zCoord - 1.5, xCoord + 2.5, yCoord + 3.5, zCoord + 2.5));
						for (Entity e : affected) {
							e.setFire(5);
						}
					}
				}
				brokenTime++;

			} else {
				if (this.node != null && this.node.net != null) {
					powerTransfer = this.node.net.energyTracker;
				} else {
					powerTransfer = 0;
				}

				if (powerTransfer > MAX_POWER) {
					temperature += (powerTransfer - MAX_POWER) / (float)MAX_POWER / 12.5;
					temperature -= 0.15F;
				} else {
					temperature -= 0.5F;
				}

				if (temperature < 20F) temperature = 20F;

				if (temperature >= MAX_TEMP) {
					isBroken = true;
					burnTime = 2400;
					brokenTime = 0;
					temperature = MAX_TEMP;
					worldObj.newExplosion(null, xCoord + 0.5, yCoord + 1.5, zCoord + 0.5, 2.5F, true, false);

					if (this.node != null) {
						Nodespace.destroyNode(worldObj, xCoord, yCoord, zCoord);
						this.node = null;
					}
					this.markDirty();
				}
			}

			if (worldObj.getTotalWorldTime() % 20 == 0) {
				this.networkPackNT(50);
			}

		} else {
			if (isBroken) {
				if (burnTime > 0) {
					ParticleUtil.spawnGasFlame(worldObj, xCoord + 0.5F + (worldObj.rand.nextFloat() - 0.5F), yCoord + 1.5F, zCoord + 0.5F + (worldObj.rand.nextFloat() - 0.5F), 0, 0.05, 0);
				}
				if (worldObj.rand.nextInt(4) == 0) {
					worldObj.spawnParticle("largesmoke", xCoord + 0.5, yCoord + 1.5 + worldObj.rand.nextFloat(), zCoord + 0.5, 0, 0.1, 0);
				}
			} else {
				if (temperature > 150F) {
					if (worldObj.rand.nextInt(10) == 0) {
						worldObj.spawnParticle("smoke", xCoord + 0.5 + (worldObj.rand.nextFloat() - 0.5F), yCoord + 2.0, zCoord + 0.5 + (worldObj.rand.nextFloat() - 0.5F), 0, 0.05, 0);
					}
				}
			}
		}
	}

	@Override
	public boolean shouldCreateNode() {
		return !this.isBroken;
	}

	@Override
	public PowerNode createNode() {
		TileEntity tile = (TileEntity) this;
		BlockPos corePos = new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord);
		PowerNode node = new PowerNode(corePos);

		node.setConnections(new DirPos(xCoord, yCoord, zCoord, ForgeDirection.UNKNOWN));

		for(int[] pos : this.connected) {
			node.addConnection(new DirPos(pos[0], pos[1], pos[2], ForgeDirection.UNKNOWN));
		}
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
			if(side != ForgeDirection.UP && side != ForgeDirection.DOWN) {
				node.addConnection(new DirPos(xCoord + side.offsetX, yCoord, zCoord + side.offsetZ, side));
			}
		}
		return node;
	}

	@Override
	public boolean isDamaged() {
		return this.isBroken;
	}

	List<AStack> repair = new ArrayList<>();
	@Override
	public List<AStack> getRepairMaterials() {
		if(!repair.isEmpty()) return repair;

		repair.add(new OreDictStack("plateSteel", 4));
		repair.add(new ComparableStack(ModItems.coil_copper, 4));
		return repair;
	}

	@Override
	public void repair() {
		this.isBroken = false;
		this.temperature = 20F;
		this.burnTime = 0;
		this.brokenTime = 0;

		if (this.node != null) {
			Nodespace.destroyNode(worldObj, xCoord, yCoord, zCoord);
			this.node = null;
		}

		this.markDirty();
	}

	@Override
	public void tryExtinguish(World world, int x, int y, int z, EnumExtinguishType type) {
		if (this.isBroken && this.burnTime > 0) {
			if (type == EnumExtinguishType.WATER || type == EnumExtinguishType.CO2 || type == EnumExtinguishType.FOAM) {
				this.burnTime = 0;
				this.markDirty();
				this.networkPackNT(50);
			}
		}
	}

	@Override public ConnectionType getConnectionType() { return ConnectionType.TRIPLE; }
	@Override public boolean isUniversal() { return true; }
	@Override public double getMaxWireLength() { return 35D; }

	@Override
	public Vec3 getConnectionPoint() {
		return Vec3.createVectorHelper(xCoord + 0.5, yCoord + 2.75, zCoord + 0.5);
	}

	@Override
	public Vec3[] getMountPos() {
		double height = 2.75;
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
		if(dir == ForgeDirection.UNKNOWN) dir = ForgeDirection.NORTH;
		ForgeDirection side = dir.getRotation(ForgeDirection.UP);

		return new Vec3[] {
				Vec3.createVectorHelper(0.5 + side.offsetX, height, 0.5 + side.offsetZ),
				Vec3.createVectorHelper(0.5, height, 0.5),
				Vec3.createVectorHelper(0.5 - side.offsetX, height, 0.5 - side.offsetZ)
		};
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		temperature = nbt.getFloat("temperature");
		isBroken = nbt.getBoolean("isBroken");
		burnTime = nbt.getInteger("burnTime");
		brokenTime = nbt.getInteger("brokenTime");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setFloat("temperature", temperature);
		nbt.setBoolean("isBroken", isBroken);
		nbt.setInteger("burnTime", burnTime);
		nbt.setInteger("brokenTime", brokenTime);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeFloat(temperature);
		buf.writeBoolean(isBroken);
		buf.writeInt(burnTime);
		buf.writeInt(brokenTime);
		buf.writeLong(powerTransfer);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		temperature = buf.readFloat();
		isBroken = buf.readBoolean();
		burnTime = buf.readInt();
		brokenTime = buf.readInt();
		powerTransfer = buf.readLong();
	}
}
