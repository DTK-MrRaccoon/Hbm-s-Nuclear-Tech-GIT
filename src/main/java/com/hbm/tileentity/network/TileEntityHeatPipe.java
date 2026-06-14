package com.hbm.tileentity.network;

import com.hbm.tileentity.TileEntityLoadedBase;
import api.hbm.tile.IHeatPipe;
import api.hbm.tile.IHeatSource;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityHeatPipe extends TileEntityLoadedBase implements IHeatPipe {

	public int heat;
	public int maxHeat = 500000;
	public boolean[] connections = new boolean[6];
	public static int transferRate = 250000;
	public static float baseLoss = 0.035F;
	public static float asbestosLoss = 0.001F;
	public static float polymerLoss = 0.015F;
	public byte insulation = 0; // 0=none, 1=asbestos, 2=polymer

	public TileEntityHeatPipe() {
		super();
	}

	@Override
	public void updateEntity() {
		if(worldObj.isRemote) return;

		this.updateConnections();
		this.transferHeat();
		this.applyLoss();

		if(this.heat < 0) this.heat = 0;
		if(this.heat > this.maxHeat) this.heat = this.maxHeat;

		this.networkPackNT(10);
	}

	private void transferHeat() {
		for(int i = 0; i < 6; i++) {
			if(!this.connections[i]) continue;

			ForgeDirection dir = ForgeDirection.getOrientation(i);
			int ix = this.xCoord + dir.offsetX;
			int iy = this.yCoord + dir.offsetY;
			int iz = this.zCoord + dir.offsetZ;

			TileEntity te = worldObj.getTileEntity(ix, iy, iz);
			if(!(te instanceof TileEntityHeatPipe)) continue;

			TileEntityHeatPipe other = (TileEntityHeatPipe) te;
			if(!shouldProcessPair(ix, iy, iz)) continue;

			int diff = this.heat - other.heat;
			if(diff == 0) continue;

			int move = Math.min(Math.abs(diff) / 2, transferRate);
			if(move <= 0) continue;

			if(diff > 0) {
				this.heat -= move;
				other.heat += move;
			} else {
				this.heat += move;
				other.heat -= move;
			}
		}

		for(int i = 0; i < 6; i++) {
			if(!this.connections[i]) continue;

			ForgeDirection dir = ForgeDirection.getOrientation(i);
			int ix = this.xCoord + dir.offsetX;
			int iy = this.yCoord + dir.offsetY;
			int iz = this.zCoord + dir.offsetZ;

			TileEntity te = worldObj.getTileEntity(ix, iy, iz);
			if(!(te instanceof IHeatSource) || te instanceof TileEntityHeatPipe) continue;

			IHeatSource source = (IHeatSource) te;
			int sourceHeat = source.getHeatStored();
			int diff = sourceHeat - this.heat;
			if(diff <= 0) continue;

			int move = Math.min(Math.min(diff / 2, transferRate), this.maxHeat - this.heat);
			if(move <= 0) continue;

			source.useUpHeat(move);
			this.heat += move;
		}
	}

	private boolean shouldProcessPair(int ix, int iy, int iz) {
		if(this.xCoord != ix) return this.xCoord < ix;
		if(this.yCoord != iy) return this.yCoord < iy;
		return this.zCoord < iz;
	}

	private void applyLoss() {
		if(this.heat <= 0) return;

		float lossRate;
		if(this.insulation == 1) lossRate = asbestosLoss;
		else if(this.insulation == 2) lossRate = polymerLoss;
		else lossRate = baseLoss;

		int loss = (int) (this.heat * lossRate);
		if(loss < 1 && this.heat > 0) loss = 1;
		this.heat -= loss;
	}

	public float getCurrentLoss() {
		if(this.insulation == 1) return asbestosLoss;
		if(this.insulation == 2) return polymerLoss;
		return baseLoss;
	}

	public boolean isInsulated() {
		return this.insulation > 0;
	}

	public void updateConnections() {
		boolean changed = false;
		for(int i = 0; i < 6; i++) {
			ForgeDirection dir = ForgeDirection.getOrientation(i);
			int ix = this.xCoord + dir.offsetX;
			int iy = this.yCoord + dir.offsetY;
			int iz = this.zCoord + dir.offsetZ;
			boolean canConnect = false;
			TileEntity te = worldObj.getTileEntity(ix, iy, iz);
			if(te instanceof TileEntityHeatPipe) canConnect = true;
			if(te instanceof IHeatSource) canConnect = true;
			if(this.connections[i] != canConnect) {
				this.connections[i] = canConnect;
				changed = true;
			}
		}
		if(changed) {
			this.markDirty();
			if(this.worldObj instanceof WorldServer) {
				WorldServer world = (WorldServer) this.worldObj;
				world.getPlayerManager().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			}
		}
	}

	@Override
	public int getHeatStored() {
		return this.heat;
	}

	@Override
	public void useUpHeat(int heat) {
		this.heat = Math.max(0, this.heat - heat);
	}

	@Override
	public int getMaxHeat() {
		return this.maxHeat;
	}

	@Override
	public void setHeat(int heat) {
		this.heat = Math.min(this.maxHeat, heat);
	}

	@Override
	public boolean canConnectOnSide(ForgeDirection dir) {
		return dir != null && this.connections[dir.ordinal()];
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.heat = nbt.getInteger("heat");
		for(int i = 0; i < 6; i++) this.connections[i] = nbt.getBoolean("conn_" + i);
		this.insulation = nbt.getByte("insulation");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("heat", this.heat);
		for(int i = 0; i < 6; i++) nbt.setBoolean("conn_" + i, this.connections[i]);
		nbt.setByte("insulation", this.insulation);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(this.heat);
		byte connMask = 0;
		for(int i = 0; i < 6; i++) if(this.connections[i]) connMask |= (1 << i);
		buf.writeByte(connMask);
		buf.writeByte(this.insulation);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.heat = buf.readInt();
		byte connMask = buf.readByte();
		for(int i = 0; i < 6; i++) this.connections[i] = (connMask & (1 << i)) != 0;
		this.insulation = buf.readByte();

		if(this.worldObj != null && this.worldObj.isRemote) {
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
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
		if(this.worldObj != null && this.worldObj.isRemote) {
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		}
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord - 1, zCoord - 1, xCoord + 2, yCoord + 2, zCoord + 2);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}