package com.hbm.blocks.machine.elevator;

import com.hbm.blocks.BlockContainerBase;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.machine.elevator.TileEntityElevatorController;

import api.hbm.block.IToolable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockElevatorController extends BlockContainerBase implements IToolable {

	@SideOnly(Side.CLIENT) public IIcon iconFront;
	@SideOnly(Side.CLIENT) public IIcon iconBack;
	@SideOnly(Side.CLIENT) public IIcon iconSide;
	@SideOnly(Side.CLIENT) public IIcon iconTop;

	public BlockElevatorController() {
		super(Material.iron);
		this.setHardness(3.0F);
		this.setResistance(10.0F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.iconTop   = reg.registerIcon("hbm:machine_controller_top");
		this.iconFront = reg.registerIcon("hbm:machine_controller");
		this.iconBack  = reg.registerIcon("hbm:machine_controller_back");
		this.iconSide  = reg.registerIcon("hbm:machine_controller_side");
		this.blockIcon = this.iconSide;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int metadata) {
		if (metadata == 0) metadata = 3;

		if (metadata == side) return iconFront;

		if (side == 0 || side == 1) return iconTop;

		if (metadata == 2 && side == 3 ||
			metadata == 3 && side == 2 ||
			metadata == 4 && side == 5 ||
			metadata == 5 && side == 4)
			return iconBack;

		return iconSide;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityElevatorController();
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float fx, float fy, float fz) {
		if (world.isRemote) return true;
		TileEntity te = world.getTileEntity(x, y, z);
		if (!(te instanceof TileEntityElevatorController)) return false;

		ItemStack held = player.getHeldItem();
		if (held != null) {
			ToolType tool = ToolType.getType(held);
			if (tool != null) {
				return onScrew(world, player, x, y, z, side, fx, fy, fz, tool);
			}
		}

		player.openGui(MainRegistry.instance, 0, world, x, y, z);
		return true;
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
		if (world.isRemote) return true;
		TileEntity te = world.getTileEntity(x, y, z);
		if (!(te instanceof TileEntityElevatorController)) return false;
		TileEntityElevatorController ctrl = (TileEntityElevatorController) te;

		if (tool == ToolType.SCREWDRIVER && player.isSneaking()) {
			ctrl.scanForFloors();
			player.addChatMessage(new net.minecraft.util.ChatComponentText(
				"\u00a7aElevator: scanned " + ctrl.getFloorCount() + " floor(s)."));
			return true;
		}

		if (tool == ToolType.HAND_DRILL && player.isSneaking()) {
			ctrl.cyclePlatformSize();
			player.addChatMessage(new net.minecraft.util.ChatComponentText(
				"\u00a7aPlatform size: " + ctrl.getPlatformSize() + "x" + ctrl.getPlatformSize()));
			return true;
		}

		return false;
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack) {
		int i = MathHelper.floor_double(entity.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;

		if (i == 0) {
			world.setBlockMetadataWithNotify(x, y, z, 2, 2);
		}
		if (i == 1) {
			world.setBlockMetadataWithNotify(x, y, z, 5, 2);
		}
		if (i == 2) {
			world.setBlockMetadataWithNotify(x, y, z, 3, 2);
		}
		if (i == 3) {
			world.setBlockMetadataWithNotify(x, y, z, 4, 2);
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block block, int meta) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof TileEntityElevatorController) {
			((TileEntityElevatorController) te).onControllerDestroyed();
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public boolean isOpaqueCube() { return true; }
	@Override
	public boolean renderAsNormalBlock() { return true; }
}
