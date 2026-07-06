package com.hbm.tileentity.machine.steam;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.fluid.IFluidStandardTransceiver;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class TileEntitySteamMachineBase extends TileEntityMachineBase implements IFluidStandardTransceiver, IGUIProvider {

	public final FluidTank steam;
	public final FluidTank spentSteam;
	public int progress;
	public int maxProgress = 100;

	public int steamConsumedLastTick = 0;
	protected int steamRemainder = 0;

	private boolean wasActiveLastTick = false;

	protected TileEntitySteamMachineBase(int slotCount, int steamCap, int wasteCap) {
		super(slotCount);
		this.steam = new FluidTank(Fluids.STEAM, steamCap);
		this.spentSteam = new FluidTank(Fluids.SPENTSTEAM, wasteCap);
	}

	public abstract String getName();
	protected abstract boolean canProcess();
	protected abstract void processItem();

	protected abstract int getRequiredSteamPerTick();
	protected abstract void updateMachineMetrics(boolean isProcessing, int steamAvailable);
	protected abstract void serializeMachine(ByteBuf buf);
	protected abstract void deserializeMachine(ByteBuf buf);
	protected abstract void readMachineNBT(NBTTagCompound nbt);
	protected abstract void writeMachineNBT(NBTTagCompound nbt);

	public ForgeDirection getFrontDirection() {
		if(this.worldObj == null) return ForgeDirection.NORTH;
		int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
		switch(meta % 4) {
			case 0: return ForgeDirection.NORTH;
			case 1: return ForgeDirection.EAST;
			case 2: return ForgeDirection.SOUTH;
			case 3: return ForgeDirection.WEST;
			default: return ForgeDirection.NORTH;
		}
	}

	public ForgeDirection getBackDirection() {
		return getFrontDirection().getOpposite();
	}

	protected void updateFluidConnections() {
		ForgeDirection back = getBackDirection();
		ForgeDirection up = ForgeDirection.UP;
		ForgeDirection down = ForgeDirection.DOWN;

		this.trySubscribe(steam.getTankType(), worldObj, xCoord + up.offsetX, yCoord + up.offsetY, zCoord + up.offsetZ, up);
		this.trySubscribe(steam.getTankType(), worldObj, xCoord + back.offsetX, yCoord + back.offsetY, zCoord + back.offsetZ, back);
		this.trySubscribe(steam.getTankType(), worldObj, xCoord + down.offsetX, yCoord + down.offsetY, zCoord + down.offsetZ, down);
	}

	protected void outputSpentSteam() {
		if(spentSteam.getFill() <= 0) return;

		ForgeDirection back = getBackDirection();
		ForgeDirection down = ForgeDirection.DOWN;

		int totalOutputDirs = 2;
		int amountPerSide = spentSteam.getFill() / totalOutputDirs;

		if(amountPerSide > 0) {
			int initialFill = spentSteam.getFill();

			this.sendFluid(spentSteam, worldObj, xCoord + back.offsetX, yCoord + back.offsetY, zCoord + back.offsetZ, back);
			int sentToBack = initialFill - spentSteam.getFill();

			if(spentSteam.getFill() > 0) {
				this.sendFluid(spentSteam, worldObj, xCoord + down.offsetX, yCoord + down.offsetY, zCoord + down.offsetZ, down);
			}
		} else {
			this.sendFluid(spentSteam, worldObj, xCoord + back.offsetX, yCoord + back.offsetY, zCoord + back.offsetZ, back);
			if(spentSteam.getFill() > 0) {
				this.sendFluid(spentSteam, worldObj, xCoord + down.offsetX, yCoord + down.offsetY, zCoord + down.offsetZ, down);
			}
		}
	}

	protected void ventSteam(int amount) {
		if(this.worldObj == null || this.worldObj.isRemote || amount <= 0) return;

		ForgeDirection back = getBackDirection();
		double baseX = xCoord + 0.5D + back.offsetX * 0.65D;
		double baseY = yCoord + 0.65D;
		double baseZ = zCoord + 0.5D + back.offsetZ * 0.65D;

		int particles = Math.max(1, Math.min(6, amount / 4 + 1));
		for(int i = 0; i < particles; i++) {
			double x = baseX + (worldObj.rand.nextDouble() - 0.5D) * 0.2D;
			double y = baseY + worldObj.rand.nextDouble() * 0.15D;
			double z = baseZ + (worldObj.rand.nextDouble() - 0.5D) * 0.2D;
			worldObj.spawnParticle("cloud", x, y, z, back.offsetX * 0.02D, 0.02D, back.offsetZ * 0.02D);
		}
	}

	protected void dropInventory() {
		if(worldObj == null || worldObj.isRemote) return;

		java.util.Random rand = this.worldObj.rand;
		for(int i = 0; i < this.getSizeInventory(); i++) {
			ItemStack stack = this.getStackInSlot(i);
			if(stack == null) continue;

			while(stack.stackSize > 0) {
				int amount = rand.nextInt(21) + 10;
				if(amount > stack.stackSize) amount = stack.stackSize;
				stack.stackSize -= amount;

				ItemStack drop = new ItemStack(stack.getItem(), amount, stack.getItemDamage());
				if(stack.hasTagCompound()) drop.setTagCompound((NBTTagCompound) stack.getTagCompound().copy());

				EntityItem entity = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, drop);
				double speed = 0.05D;
				entity.motionX = rand.nextGaussian() * speed;
				entity.motionY = rand.nextGaussian() * speed + 0.2D;
				entity.motionZ = rand.nextGaussian() * speed;
				worldObj.spawnEntityInWorld(entity);
			}
		}
	}

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			if(this.worldObj.getTotalWorldTime() % 20 == 0) {
				this.updateFluidConnections();
			}

			boolean canRun = this.canProcess();
			int targetSteam = canRun ? getRequiredSteamPerTick() : 0;
			int steamAvailable = Math.min(steam.getFill(), targetSteam);

			if(steamAvailable > 0) {
				steam.setFill(steam.getFill() - steamAvailable);
				steamConsumedLastTick = steamAvailable;

				steamRemainder += steamAvailable;
				int wasteToProduce = steamRemainder / 100;
				steamRemainder %= 100;

				if(wasteToProduce > 0) {
					int space = spentSteam.getMaxFill() - spentSteam.getFill();
					if(space >= wasteToProduce) {
						spentSteam.setFill(spentSteam.getFill() + wasteToProduce);
					} else if(space > 0) {
						spentSteam.setFill(spentSteam.getMaxFill());
						ventSteam((wasteToProduce - space) * 100);
					} else {
						ventSteam(wasteToProduce * 100);
					}
				}
			} else {
				steamConsumedLastTick = 0;
			}

			updateMachineMetrics(canRun, steamAvailable);

			this.outputSpentSteam();

			if(spentSteam.getFill() >= spentSteam.getMaxFill()) {
				ventSteam(100);
			}

			boolean isActiveNow = steamConsumedLastTick > 0;
			if(isActiveNow != wasActiveLastTick) {
				wasActiveLastTick = isActiveNow;
				this.markDirty();
				this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			}

			this.networkPackNT(32);
		}
	}

	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);
		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.func_148857_g());
		this.worldObj.markBlockRangeForRenderUpdate(this.xCoord, this.yCoord, this.zCoord, this.xCoord, this.yCoord, this.zCoord);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		steam.serialize(buf);
		spentSteam.serialize(buf);
		buf.writeInt(progress);
		buf.writeInt(maxProgress);
		buf.writeInt(steamConsumedLastTick);
		serializeMachine(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		steam.deserialize(buf);
		spentSteam.deserialize(buf);
		progress = buf.readInt();
		maxProgress = buf.readInt();
		steamConsumedLastTick = buf.readInt();
		deserializeMachine(buf);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		progress = nbt.getInteger("progress");
		steamRemainder = nbt.getInteger("remainder");
		steam.readFromNBT(nbt, "steam");
		spentSteam.readFromNBT(nbt, "spent");
		steamConsumedLastTick = nbt.getInteger("consumedLast");
		wasActiveLastTick = steamConsumedLastTick > 0;
		readMachineNBT(nbt);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("progress", progress);
		nbt.setInteger("remainder", steamRemainder);
		steam.writeToNBT(nbt, "steam");
		spentSteam.writeToNBT(nbt, "spent");
		nbt.setInteger("consumedLast", steamConsumedLastTick);
		writeMachineNBT(nbt);
	}

	@Override
	public FluidTank[] getReceivingTanks() {
		return new FluidTank[] { steam };
	}

	@Override
	public FluidTank[] getSendingTanks() {
		return new FluidTank[] { spentSteam };
	}

	@Override
	public FluidTank[] getAllTanks() {
		return new FluidTank[] { steam, spentSteam };
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection dir) {
		if(type == steam.getTankType()) {
			return dir == ForgeDirection.UP || dir == ForgeDirection.DOWN || dir == getBackDirection();
		}

		if(type == spentSteam.getTankType()) {
			return dir == getBackDirection() || dir == ForgeDirection.DOWN;
		}

		return false;
	}

	@Override
	public long getFluidAvailable(FluidType type, int pressure) {
		long amount = 0;
		for(FluidTank tank : getSendingTanks()) {
			if(tank.getTankType() == type && tank.getPressure() == pressure) amount += tank.getFill();
		}
		return amount;
	}

	@Override
	public void useUpFluid(FluidType type, int pressure, long amount) {
		for(FluidTank tank : getSendingTanks()) {
			if(tank.getTankType() == type && tank.getPressure() == pressure) {
				int use = (int)Math.min(amount, tank.getFill());
				tank.setFill(tank.getFill() - use);
				amount -= use;
				if(amount <= 0) return;
			}
		}
	}

	@Override
	public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	@cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536D;
	}

	@Override
	public abstract Container provideContainer(int ID, EntityPlayer player, net.minecraft.world.World world, int x, int y, int z);

	@Override
	public abstract Object provideGUI(int ID, EntityPlayer player, net.minecraft.world.World world, int x, int y, int z);
}
