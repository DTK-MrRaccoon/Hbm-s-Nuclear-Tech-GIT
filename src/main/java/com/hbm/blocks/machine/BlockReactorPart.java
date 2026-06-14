package com.hbm.blocks.machine;

import com.hbm.blocks.BlockEnumMulti;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.machine.TileEntityMachineReactorLarge;

import api.hbm.fluid.IFluidConnectorBlock;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockReactorPart extends BlockEnumMulti implements IFluidConnectorBlock {

	public static enum ReactorPart {
		ELEMENT, CONTROL, HATCH, EJECTOR, INSERTER, CONDUCTOR, COMPUTER
	}

	private static final ForgeDirection[] ROTATION_DIRECTIONS = {
		ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST
	};

	@SideOnly(Side.CLIENT)
	private IIcon[] iconsSide;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsTop;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsFront;

	public BlockReactorPart() {
		super(Material.iron, ReactorPart.class, true, true);
	}

	public static int packMeta(ReactorPart subtype, int rotation) {
		if (subtype == ReactorPart.HATCH) {
			if (rotation == 0) return 8;
			if (rotation == 1) return 9;
			if (rotation == 2) return 2;
			if (rotation == 3) return 11;
		}
		if (subtype == ReactorPart.EJECTOR) {
			if (rotation == 0) return 12;
			if (rotation == 1) return 13;
			if (rotation == 2) return 3;
			if (rotation == 3) return 15;
		}
		if (subtype == ReactorPart.INSERTER) {
			if (rotation == 0) return 14;
			if (rotation == 1) return 7;
			if (rotation == 2) return 4;
			if (rotation == 3) return 10;
		}
		return subtype.ordinal();
	}

	public static ReactorPart getSubtype(int meta) {
		switch (meta) {
			case 0: return ReactorPart.ELEMENT;
			case 1: return ReactorPart.CONTROL;
			case 5: return ReactorPart.CONDUCTOR;
			case 6: return ReactorPart.COMPUTER;
			case 8: case 9: case 2: case 11: return ReactorPart.HATCH;
			case 12: case 13: case 3: case 15: return ReactorPart.EJECTOR;
			case 14: case 7: case 4: case 10: return ReactorPart.INSERTER;
			default: return ReactorPart.ELEMENT;
		}
	}

	public static int getRotation(int meta) {
		switch (meta) {
			case 8: case 12: case 14: return 0;
			case 9: case 13: case 7: return 1;
			case 2: case 3: case 4: return 2;
			case 11: case 15: case 10: return 3;
			default: return 0;
		}
	}

	public static ForgeDirection getDirection(int meta) {
		return ROTATION_DIRECTIONS[getRotation(meta)];
	}

	@Override
	public int damageDropped(int meta) {
		return getSubtype(meta).ordinal();
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
		int subtypeOrdinal = stack.getItemDamage();
		if (subtypeOrdinal < 0 || subtypeOrdinal >= ReactorPart.values().length) {
			subtypeOrdinal = 0;
		}
		ReactorPart subtype = ReactorPart.values()[subtypeOrdinal];

		if (subtype == ReactorPart.HATCH || subtype == ReactorPart.EJECTOR || subtype == ReactorPart.INSERTER) {
			int l = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
			int rot;
			if (l == 0) {
				rot = 0;
			} else if (l == 1) {
				rot = 3;
			} else if (l == 2) {
				rot = 1;
			} else {
				rot = 2;
			}
			world.setBlockMetadataWithNotify(x, y, z, packMeta(subtype, rot), 2);
		} else {
			world.setBlockMetadataWithNotify(x, y, z, subtypeOrdinal, 2);
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		int meta = world.getBlockMetadata(x, y, z);
		ReactorPart part = getSubtype(meta);

		if (player.getHeldItem() != null && player.getHeldItem().getItem() == net.minecraft.init.Items.stick) {
			debugBlock(world, x, y, z, player);
			return true;
		}

		if (part == ReactorPart.HATCH || part == ReactorPart.EJECTOR || part == ReactorPart.INSERTER) {
			if (world.isRemote) return true;
			if (!player.isSneaking()) {
				ForgeDirection front = getDirection(meta);
				ForgeDirection toCore = front.getOpposite();
				int coreX = x + toCore.offsetX * 2;
				int coreZ = z + toCore.offsetZ * 2;
				TileEntity te = world.getTileEntity(coreX, y, coreZ);
				if (te instanceof TileEntityMachineReactorLarge) {
					TileEntityMachineReactorLarge reactor = (TileEntityMachineReactorLarge) te;
					if (reactor.checkBody()) {
						reactor.updateReactorSize();
						FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, coreX, y, coreZ);
					} else {
						player.addChatMessage(new ChatComponentText("Error: Reactor structure invalid!"));
					}
				} else {
					player.addChatMessage(new ChatComponentText("Error: Reactor core not found!"));
				}
				return true;
			}
		}
		return false;
	}

	public static void debugBlock(World world, int x, int y, int z, EntityPlayer player) {
		int meta = world.getBlockMetadata(x, y, z);
		player.addChatMessage(new ChatComponentText("Subtype: " + getSubtype(meta).name() + ", Front: " + getDirection(meta)));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		String base = RefStrings.MODID + ":";
		iconsSide = new IIcon[7];
		iconsTop = new IIcon[7];
		iconsFront = new IIcon[7];

		iconsSide[0] = reg.registerIcon(base + "reactor_element_side");
		iconsTop[0] = reg.registerIcon(base + "reactor_element_top");
		iconsFront[0] = iconsSide[0];

		iconsSide[1] = reg.registerIcon(base + "reactor_control_side");
		iconsTop[1] = reg.registerIcon(base + "reactor_control_top");
		iconsFront[1] = iconsSide[1];

		iconsSide[2] = reg.registerIcon(base + "brick_concrete");
		iconsTop[2] = iconsSide[2];
		iconsFront[2] = reg.registerIcon(base + "reactor_hatch");

		iconsSide[3] = reg.registerIcon(base + "brick_concrete");
		iconsTop[3] = iconsSide[3];
		iconsFront[3] = reg.registerIcon(base + "reactor_ejector");

		iconsSide[4] = reg.registerIcon(base + "brick_concrete");
		iconsTop[4] = iconsSide[4];
		iconsFront[4] = reg.registerIcon(base + "reactor_inserter");

		iconsSide[5] = reg.registerIcon(base + "reactor_conductor_side");
		iconsTop[5] = reg.registerIcon(base + "reactor_conductor_top");
		iconsFront[5] = iconsSide[5];

		iconsSide[6] = reg.registerIcon(base + "reactor_computer");
		iconsTop[6] = iconsSide[6];
		iconsFront[6] = iconsSide[6];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		ReactorPart part = getSubtype(meta);
		int idx = part.ordinal();

		if (side == 0 || side == 1) {
			return iconsTop[idx];
		}

		if ((part == ReactorPart.HATCH || part == ReactorPart.EJECTOR || part == ReactorPart.INSERTER)
				&& side == getDirection(meta).ordinal()) {
			return iconsFront[idx];
		}

		return iconsSide[idx];
	}

	@Override
	public boolean hasTileEntity(int metadata) {
		return getSubtype(metadata) == ReactorPart.COMPUTER;
	}

	@Override
	public TileEntity createTileEntity(World world, int metadata) {
		return getSubtype(metadata) == ReactorPart.COMPUTER ? new TileEntityMachineReactorLarge() : null;
	}

	@Override
	public boolean canConnect(FluidType type, IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
		int meta = world.getBlockMetadata(x, y, z);
		ReactorPart part = getSubtype(meta);
		if (part == ReactorPart.HATCH || part == ReactorPart.CONDUCTOR) {
			return type == Fluids.WATER || type == Fluids.COOLANT || type == Fluids.STEAM
					|| type == Fluids.HOTSTEAM || type == Fluids.SUPERHOTSTEAM || type == Fluids.ULTRAHOTSTEAM;
		}
		return false;
	}
}