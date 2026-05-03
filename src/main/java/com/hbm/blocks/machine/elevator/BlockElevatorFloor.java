package com.hbm.blocks.machine.elevator;

import com.hbm.blocks.BlockContainerBase;
import com.hbm.tileentity.machine.elevator.TileEntityElevatorFloor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class BlockElevatorFloor extends BlockContainerBase {

	@SideOnly(Side.CLIENT) public IIcon iconTop;
	@SideOnly(Side.CLIENT) public IIcon iconSide;

	public BlockElevatorFloor() {
		super(Material.iron);
		this.setHardness(2.0F);
		this.setResistance(8.0F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.iconTop  = reg.registerIcon("hbm:deco_steel");
		this.iconSide = reg.registerIcon("hbm:deco_steel");
		this.blockIcon = this.iconSide;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		if (side == 1 || side == 0) return iconTop;
		return iconSide;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityElevatorFloor();
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		if (!world.isRemote) {
			TileEntity te = world.getTileEntity(x, y, z);
			if (te instanceof TileEntityElevatorFloor) {
				((TileEntityElevatorFloor) te).spawnDoor();
			}
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof TileEntityElevatorFloor) {
			((TileEntityElevatorFloor) te).removeDoor();
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float fx, float fy, float fz) {
		return false;
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public int isProvidingWeakPower(net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof TileEntityElevatorFloor) {
			return ((TileEntityElevatorFloor) te).isPlatformPresent() ? 15 : 0;
		}
		return 0;
	}

	@Override
	public int isProvidingStrongPower(net.minecraft.world.IBlockAccess world, int x, int y, int z, int side) {
		return isProvidingWeakPower(world, x, y, z, side);
	}

	@Override
	public boolean isOpaqueCube() { return true; }
	@Override
	public boolean renderAsNormalBlock() { return true; }
}
