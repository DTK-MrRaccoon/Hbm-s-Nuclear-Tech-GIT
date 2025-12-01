package com.hbm.tileentity.machine;

import com.hbm.interfaces.IMultiblock;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTank;

import api.hbm.fluidmk2.IFluidConnectorMK2;
import api.hbm.fluidmk2.IFluidStandardReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardSenderMK2;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityDummy extends TileEntity implements IFluidConnectorMK2, IFluidStandardReceiverMK2, IFluidStandardSenderMK2 {

	public int targetX;
	public int targetY;
	public int targetZ;
	private boolean isLoaded = true;
	
    @Override
	public void updateEntity() {
    	if(!this.worldObj.isRemote) {
    		if(!(this.worldObj.getBlock(targetX, targetY, targetZ) instanceof IMultiblock)) {
    			worldObj.func_147480_a(xCoord, yCoord, zCoord, false);
    		}
    	}
    }
    
    @Override
    public void onChunkUnload() {
    	super.onChunkUnload();
    	this.isLoaded = false;
    }
    
    @Override
    public boolean isLoaded() {
    	return isLoaded;
    }

    @Override
	public void readFromNBT(NBTTagCompound nbt)
    {
    	super.readFromNBT(nbt);
        this.targetX = nbt.getInteger("tx");
        this.targetY = nbt.getInteger("ty");
        this.targetZ = nbt.getInteger("tz");
    }

    @Override
	public void writeToNBT(NBTTagCompound nbt)
    {
    	super.writeToNBT(nbt);
    	nbt.setInteger("tx", this.targetX);
    	nbt.setInteger("ty", this.targetY);
    	nbt.setInteger("tz", this.targetZ);
    }
    
    // Delegate fluid operations to target tile entity
    private TileEntity getTarget() {
    	if(worldObj == null) return null;
    	return worldObj.getTileEntity(targetX, targetY, targetZ);
    }
    
    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
    	TileEntity target = getTarget();
    	if(target instanceof IFluidConnectorMK2) {
    		return ((IFluidConnectorMK2) target).canConnect(type, dir);
    	}
    	return false;
    }
    
    @Override
    public FluidTank[] getReceivingTanks() {
    	TileEntity target = getTarget();
    	if(target instanceof IFluidStandardReceiverMK2) {
    		return ((IFluidStandardReceiverMK2) target).getReceivingTanks();
    	}
    	return new FluidTank[0];
    }
    
    @Override
    public FluidTank[] getSendingTanks() {
    	TileEntity target = getTarget();
    	if(target instanceof IFluidStandardSenderMK2) {
    		return ((IFluidStandardSenderMK2) target).getSendingTanks();
    	}
    	return new FluidTank[0];
    }
    
    @Override
    public FluidTank[] getAllTanks() {
    	TileEntity target = getTarget();
    	if(target instanceof IFluidStandardReceiverMK2) {
    		return ((IFluidStandardReceiverMK2) target).getAllTanks();
    	}
    	if(target instanceof IFluidStandardSenderMK2) {
    		return ((IFluidStandardSenderMK2) target).getAllTanks();
    	}
    	return new FluidTank[0];
    }
}
