package com.hbm.tileentity.machine.storage;

import com.hbm.lib.Library;
import com.hbm.util.fauxpointtwelve.DirPos;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityMachineRadTank extends TileEntityBarrel {

	public boolean frame = false;

	public TileEntityMachineRadTank() {
		super(128000);
	}

	@Override
	public boolean isShielded() {
		return true;
	}

	@Override
	public String getName() {
		return "container.machineRadTank";
	}

	@Override
	public void updateEntity() {
		super.updateEntity();

		if(!worldObj.isRemote) {
			if(worldObj.getTotalWorldTime() % 20 == 0) {
				boolean hasBlockAbove = !worldObj.isAirBlock(xCoord, yCoord + 3, zCoord);
				if(hasBlockAbove != this.frame) {
					this.frame = hasBlockAbove;
					this.markChanged();
				}
			}
			
			if(tank.getFill() > 0 && tank.getTankType().isAntimatter()) {
				worldObj.func_147480_a(xCoord, yCoord, zCoord, false);
				worldObj.newExplosion(null, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, 7.0F, true, true);
			}
		}
	}

	@Override
	protected DirPos[] getConPos() {
		return new DirPos[] {
				new DirPos(xCoord, yCoord + 1, zCoord - 2, Library.NEG_Z),
				new DirPos(xCoord, yCoord + 1, zCoord + 2, Library.POS_Z),
				new DirPos(xCoord - 2, yCoord + 1, zCoord, Library.NEG_X),
				new DirPos(xCoord + 2, yCoord + 1, zCoord, Library.POS_X),
				new DirPos(xCoord, yCoord + 3, zCoord, Library.POS_Y),
				new DirPos(xCoord, yCoord - 1, zCoord, Library.NEG_Y)
		};
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(frame);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.frame = buf.readBoolean();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.frame = nbt.getBoolean("frame");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("frame", frame);
	}

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) {
			bb = AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord, zCoord - 1, xCoord + 2, yCoord + 3, zCoord + 2);
		}
		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}