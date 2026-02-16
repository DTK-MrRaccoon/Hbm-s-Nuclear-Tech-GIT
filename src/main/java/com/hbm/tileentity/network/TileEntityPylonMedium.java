package com.hbm.tileentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
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
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityPylonMedium extends TileEntityPylonBase implements IRepairable {

	public float temperature = 20F;
	public boolean isBroken = false;
	public int burnTime = 0;
	public int brokenTime = 0;
	public long powerTransfer = 0;

	public static final long MAX_POWER = 750_000L;
	public static final float MAX_TEMP = 500F;

	@Override
	public void updateEntity() {
		super.updateEntity();

		if (!worldObj.isRemote) {
			boolean canOverheat = hasTransformer() || isConnector();

			if (isBroken) {
				powerTransfer = 0;

				if(this.node != null) {
					Nodespace.destroyNode(worldObj, xCoord, yCoord, zCoord);
					this.node = null;
				}

				if (burnTime > 0) {
					burnTime--;
					if (worldObj.getTotalWorldTime() % 20 == 0) {
						List<Entity> affected = worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(xCoord - 1.5, yCoord, zCoord - 1.5, xCoord + 2.5, yCoord + 6.5, zCoord + 2.5));
						for (Entity e : affected) {
							e.setFire(5);
						}
					}
				}
				brokenTime++;

			} else if (canOverheat) {
				if (this.node != null && this.node.net != null) {
					powerTransfer = this.node.net.energyTracker;
				} else {
					powerTransfer = 0;
				}

				// Custom Overload Formula
				if (powerTransfer > MAX_POWER) {
					temperature += (powerTransfer - MAX_POWER) / (float)MAX_POWER / 25;
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
					worldObj.newExplosion(null, xCoord + 0.5, yCoord + 6.5, zCoord + 0.5, 2.5F, true, false);

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
					ParticleUtil.spawnGasFlame(worldObj, xCoord + 0.5F + (worldObj.rand.nextFloat() - 0.5F), yCoord + 6.5F, zCoord + 0.5F + (worldObj.rand.nextFloat() - 0.5F), 0, 0.05, 0);
				}
				if (worldObj.rand.nextInt(4) == 0) {
					worldObj.spawnParticle("largesmoke", xCoord + 0.5, yCoord + 6.5 + worldObj.rand.nextFloat(), zCoord + 0.5, 0, 0.1, 0);
				}
			} else if (hasTransformer() || isConnector()) {
				if (temperature > 150F) {
					if (worldObj.rand.nextInt(10) == 0) {
						worldObj.spawnParticle("smoke", xCoord + 0.5 + (worldObj.rand.nextFloat() - 0.5F), yCoord + 6.5, zCoord + 0.5 + (worldObj.rand.nextFloat() - 0.5F), 0, 0.05, 0);
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
		if(isBroken) {
			PowerNode dummyNode = new PowerNode(new BlockPos(xCoord, -1000, zCoord));
			dummyNode.setConnections(new DirPos[0]);
			return dummyNode;
		}

		TileEntity tile = (TileEntity) this;
		PowerNode node = new PowerNode(new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord)).setConnections(new DirPos(xCoord, yCoord, zCoord, ForgeDirection.UNKNOWN));
		for(int[] pos : this.connected) node.addConnection(new DirPos(pos[0], pos[1], pos[2], ForgeDirection.UNKNOWN));

		if(this.hasTransformer() || this.isConnector()) {
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10).getOpposite();
			node.addConnection(new DirPos(xCoord + dir.offsetX, yCoord, zCoord + dir.offsetZ, dir));
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

	@Override
	public ConnectionType getConnectionType() {
		return ConnectionType.TRIPLE;
	}
	
	@Override
	public boolean isUniversal() {
		return isConnector();
	}

	@Override
	public Vec3[] getMountPos() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
		double height = 7.5D;
		
		return new Vec3[] {
				Vec3.createVectorHelper(0.5, height, 0.5),
				Vec3.createVectorHelper(0.5 + dir.offsetX, height, 0.5 + dir.offsetZ),
				Vec3.createVectorHelper(0.5 + dir.offsetX * 2, height, 0.5 + dir.offsetZ * 2),
		};
	}

	@Override
	public Vec3 getExtraMountPos() {
		if(hasTransformer() || isConnector()) {
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
			ForgeDirection side = dir.getOpposite();
			return Vec3.createVectorHelper(
					xCoord + 0.5 + (side.offsetX * 1),
					yCoord + 6,
					zCoord + 0.5 + (side.offsetZ * 1)
			);
		}
		return super.getExtraMountPos();
	}

	@Override
	public double getMaxWireLength() {
		return isConnector() ? 25D : 45D;
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		if(this.hasTransformer() || this.isConnector()) {
			return ForgeDirection.getOrientation(this.getBlockMetadata() - 10).getOpposite() == dir;
		}
		return false;
	}
	
	public boolean hasTransformer() {
		Block block = this.getBlockType();
		return block == ModBlocks.red_pylon_medium_wood_transformer || block == ModBlocks.red_pylon_medium_steel_transformer || block == ModBlocks.red_pylon_medium_concrete_transformer;
	}

	public boolean isConnector() {
		Block block = this.getBlockType();
		return block == ModBlocks.red_pylon_medium_wood_connector || block == ModBlocks.red_pylon_medium_steel_connector || block == ModBlocks.red_pylon_medium_concrete_connector;
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
