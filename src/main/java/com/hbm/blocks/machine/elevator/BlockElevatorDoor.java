package com.hbm.blocks.machine.elevator;

import com.hbm.blocks.BlockContainerBase;
import com.hbm.tileentity.machine.elevator.TileEntityElevatorDoor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockElevatorDoor extends BlockContainerBase {

	@SideOnly(Side.CLIENT) public IIcon iconDoorClosed;
	@SideOnly(Side.CLIENT) public IIcon iconDoorOpen;
	@SideOnly(Side.CLIENT) public IIcon iconDoorSide;

	public BlockElevatorDoor() {
		super(Material.iron);
		this.setHardness(2.0F);
		this.setResistance(8.0F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.iconDoorClosed = reg.registerIcon("hbm:block_steel");
		this.iconDoorOpen   = reg.registerIcon("hbm:block_steel");
		this.iconDoorSide   = reg.registerIcon("hbm:block_steel");
		this.blockIcon = this.iconDoorClosed;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		// meta bit 2 = open state
		boolean open = (meta & 4) != 0;
		if (side == 2 || side == 3) return open ? iconDoorOpen : iconDoorClosed;
		return iconDoorSide;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityElevatorDoor();
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float fx, float fy, float fz) {
		return false;
	}

	@Override
	public boolean isOpaqueCube() { return false; }
	@Override
	public boolean renderAsNormalBlock() { return false; }

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof TileEntityElevatorDoor) {
			TileEntityElevatorDoor door = (TileEntityElevatorDoor) te;
			if (door.doorState == 0) {
				return null;
			}
		}
		return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof TileEntityElevatorDoor && ((TileEntityElevatorDoor) te).doorState == 0) {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.125F);
		} else {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public int quantityDropped(java.util.Random rand) {
		return 0;
	}
}
