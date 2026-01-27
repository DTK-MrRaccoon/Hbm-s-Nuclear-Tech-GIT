package com.hbm.tileentity.network;

import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityElectricFluidPump extends TileEntityMachineBase {

	public TileEntityElectricFluidPump() {
		super(0); 
	}

	public float rot;
	public float prevRot;
	private float rotSpeed = 30.0F; // Set a fixed speed for the test

	@Override
	public void updateEntity() {
		// We skip all server-side logic (fluid/power) and just run the animation
		if(worldObj.isRemote) {
			prevRot = rot;
			rot += rotSpeed;

			// Reset rotation once it hits a full circle to prevent float overflow
			if(rot >= 360) {
				rot -= 360;
				prevRot -= 360;
			}
		}
	}

	// This ensures the model doesn't "disappear" when looking at the base
	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) {
			bb = AxisAlignedBB.getBoundingBox(
				xCoord,
				yCoord,
				zCoord,
				xCoord + 1,
				yCoord + 2,
				zCoord + 1
			);
		}
		return bb;
	}

	/* * Stripping out technical overrides to keep the class clean for testing.
	 * If your base class requires these to be implemented, keep them empty.
	 */
	@Override
	public String getName() {
		return "container.electricFluidPumpTest";
	}
}