package com.hbm.blocks.machine.elevator;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockElevatorPlatform extends Block {

	public BlockElevatorPlatform() {
		super(Material.iron);
		this.setHardness(-1.0F);
		this.setResistance(6000000.0F);
		this.setBlockUnbreakable();
		this.setBlockTextureName("stone");
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public int getRenderType() {
		return -1;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public boolean canCollideCheck(int meta, boolean holdingItem) {
		return true;
	}

	@Override
	public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
		return false;
	}

	@Override
	public int quantityDropped(java.util.Random rand) {
		return 0;
	}

	@Override
	public boolean canSilkHarvest() {
		return false;
	}
}
