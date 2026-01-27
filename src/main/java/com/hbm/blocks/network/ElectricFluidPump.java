package com.hbm.blocks.network;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.network.TileEntityElectricFluidPump;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ElectricFluidPump extends BlockDummyable {

	public ElectricFluidPump(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityElectricFluidPump();
//		if(meta >= 6) return new TileEntityProxyCombo(false, true, true);
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {1, 0, 0, 0, 0, 0};
	}

	@Override
	public int getOffset() {
		return 0;
	}

}
