package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.machine.TileEntityMachineAdvancedCentrifuge;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class MachineAdvancedCentrifuge extends BlockDummyable {

	public MachineAdvancedCentrifuge() {
		super(Material.iron);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityMachineAdvancedCentrifuge();
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {3, 0, 1, 0, 0, 1};
	}

	@Override
	public int getOffset() {
		return 0;
	}
}
