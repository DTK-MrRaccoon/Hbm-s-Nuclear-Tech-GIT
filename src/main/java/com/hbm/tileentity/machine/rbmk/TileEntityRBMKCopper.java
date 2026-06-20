package com.hbm.tileentity.machine.rbmk;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKConsole.ColumnType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import api.hbm.tile.IHeatPipe;
import api.hbm.tile.IHeatSource;
import io.netty.buffer.ByteBuf;

public class TileEntityRBMKCopper extends TileEntityRBMKBase implements IHeatSource {

	private int tuOutput;
	private double conversion;
	private int maxTransfer;

	@Override
	public void updateEntity() {
		super.updateEntity();
		if (!worldObj.isRemote) {
			pushToPipes();
		}
	}

	private void pushToPipes() {
		this.tuOutput = 0;
		this.conversion = RBMKDials.getCopperConversion(worldObj);
		this.maxTransfer = RBMKDials.getCopperMaxTransfer(worldObj);

		if (this.heat <= 750.0) return;

		ForgeDirection dir = ForgeDirection.DOWN;
		int ix = this.xCoord + dir.offsetX;
		int iy = this.yCoord + dir.offsetY;
		int iz = this.zCoord + dir.offsetZ;
		TileEntity te = worldObj.getTileEntity(ix, iy, iz);

		if (te instanceof IHeatPipe) {
			IHeatPipe pipe = (IHeatPipe) te;
			int space = pipe.getMaxHeat() - pipe.getHeatStored();
			if (space <= 0) return;

			double availableHeat = this.heat - 750.0;
			int maxTU = (int) (availableHeat * this.conversion);
			int targetTU = Math.min(maxTU, Math.min(space, this.maxTransfer));
			if (targetTU <= 0) return;

			double heatToConsume = targetTU / this.conversion;
			heatToConsume = Math.min(heatToConsume, availableHeat);

			int actualTU = (int) (heatToConsume * this.conversion);
			if (actualTU <= 0) return;

			pipe.setHeat(pipe.getHeatStored() + actualTU);
			this.heat -= heatToConsume;
			this.tuOutput = actualTU;
		}
	}

	@Override
	public int getHeatStored() {
		return 0;
	}

	@Override
	public void useUpHeat(int heat) {
	}

	@Override
	public ColumnType getConsoleType() {
		return ColumnType.COPPER;
	}

	@Override
	public NBTTagCompound getNBTForConsole() {
		NBTTagCompound data = new NBTTagCompound();
		data.setDouble("heat", this.heat);
		data.setDouble("maxHeat", this.maxHeat());
		data.setInteger("tuOutput", this.tuOutput);
		data.setDouble("conversion", this.conversion);
		data.setInteger("maxTransfer", this.maxTransfer);
		return data;
	}

	@Override
	public void getDiagData(NBTTagCompound nbt) {
		super.getDiagData(nbt);
		nbt.setInteger("tuOutput", this.tuOutput);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(this.tuOutput);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.tuOutput = buf.readInt();
	}
}