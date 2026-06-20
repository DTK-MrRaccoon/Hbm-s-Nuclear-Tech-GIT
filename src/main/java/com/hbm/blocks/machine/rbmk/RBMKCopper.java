package com.hbm.blocks.machine.rbmk;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKCopper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class RBMKCopper extends RBMKBase {

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if (meta >= this.offset)
			return new TileEntityRBMKCopper();
		return null;
	}

	@Override
	public int getRenderType() {
		return this.renderIDPassive;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return openInv(world, x, y, z, player);
	}
}