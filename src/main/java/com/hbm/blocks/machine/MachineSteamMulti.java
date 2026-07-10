package com.hbm.blocks.machine;

import java.util.List;
import java.util.Random;

import com.hbm.blocks.BlockEnumMulti;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.steam.*;

import api.hbm.block.IToolable;
import api.hbm.block.IToolable.ToolType;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineSteamMulti extends BlockEnumMulti implements ITileEntityProvider {

	public static enum SteamMachineType {
		FURNACE,
		SHREDDER,
		PRESS,
		HAMMER,
		BOILER,
		BRONZE_SHREDDER,
		BRONZE_HAMMER,
		BRONZE_BOILER,
		OSMIRIDIUM_FURNACE
	}

	private static final ForgeDirection[] FACING_DIR = new ForgeDirection[] {
			ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST
	};

	@SideOnly(Side.CLIENT)
	private IIcon[] iconsFront;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsFrontOn;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsTop;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsTopOn;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsBack;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsBackOn;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsSide;
	@SideOnly(Side.CLIENT)
	private IIcon[] iconsSideOn;
	@SideOnly(Side.CLIENT)
	private IIcon iconSide;

	public MachineSteamMulti() {
		super(Material.iron, SteamMachineType.class, true, false);
	}

	public static int getTypeIndex(int meta) {
		int type = meta & 15;
		if(type >= SteamMachineType.values().length) type = SteamMachineType.values().length - 1;
		return type;
	}

	public static int getRotationIndex(int meta) {
		return 0;
	}

	public static ForgeDirection getFacing(int meta) {
		return FACING_DIR[getRotationIndex(meta)];
	}

	public static int packMeta(int type, int rotation) {
		if(type < 0 || type >= SteamMachineType.values().length) type = 0;
		return type;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		switch(getTypeIndex(meta)) {
			case 0: return new TileEntitySteamFurnace();
			case 1: return new TileEntitySteamShredder(false);
			case 2: return new TileEntitySteamPress();
			case 3: return new TileEntitySteamHammer(false);
			case 4: return new TileEntitySteamBoiler(false);
			case 5: return new TileEntitySteamShredder(true);
			case 6: return new TileEntitySteamHammer(true);
			case 7: return new TileEntitySteamBoiler(true);
			case 8: return new TileEntityOsmiridiumFurnace();
			default: return new TileEntitySteamFurnace();
		}
	}

	@Override
	public int damageDropped(int meta) {
		return getTypeIndex(meta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		String base = RefStrings.MODID + ":";
		this.iconsFront = new IIcon[SteamMachineType.values().length];
		this.iconsFrontOn = new IIcon[SteamMachineType.values().length];
		this.iconsTop = new IIcon[SteamMachineType.values().length];
		this.iconsTopOn = new IIcon[SteamMachineType.values().length];
		this.iconsBack = new IIcon[SteamMachineType.values().length];
		this.iconsBackOn = new IIcon[SteamMachineType.values().length];
		this.iconsSide = new IIcon[SteamMachineType.values().length];
		this.iconsSideOn = new IIcon[SteamMachineType.values().length];

		this.iconsFront[0] = reg.registerIcon(base + "steam_furnace_front");
		this.iconsFrontOn[0] = reg.registerIcon(base + "steam_furnace_front_on");
		this.iconsFront[1] = reg.registerIcon(base + "steam_shredder_front");
		this.iconsFrontOn[1] = this.iconsFront[1];
		this.iconsFront[2] = reg.registerIcon(base + "steam_press_front");
		this.iconsFrontOn[2] = reg.registerIcon(base + "steam_press_front_on");
		this.iconsFront[3] = reg.registerIcon(base + "steam_hammer_front");
		this.iconsFrontOn[3] = reg.registerIcon(base + "steam_hammer_front_on");
		this.iconsFront[4] = reg.registerIcon(base + "machine_boiler_front");
		this.iconsFrontOn[4] = reg.registerIcon(base + "machine_boiler_front_lit");
		this.iconsFront[5] = reg.registerIcon(base + "steam_shredder_front_bronze");
		this.iconsFrontOn[5] = this.iconsFront[5];
		this.iconsFront[6] = reg.registerIcon(base + "steam_hammer_front_bronze");
		this.iconsFrontOn[6] = reg.registerIcon(base + "steam_hammer_front_bronze_on");
		this.iconsFront[7] = reg.registerIcon(base + "machine_boiler_front_bronze");
		this.iconsFrontOn[7] = reg.registerIcon(base + "machine_boiler_front_bronze_lit");
		this.iconsFront[8] = reg.registerIcon(base + "osmiridium_furnace_front");
		this.iconsFrontOn[8] = reg.registerIcon(base + "osmiridium_furnace_front_on");

		this.iconsTop[0] = reg.registerIcon(base + "steam_machine_pipe");
		this.iconsTopOn[0] = this.iconsTop[0];
		this.iconsTop[1] = reg.registerIcon(base + "steam_shredder_top");
		this.iconsTopOn[1] = reg.registerIcon(base + "steam_shredder_top_on");
		this.iconsTop[2] = reg.registerIcon(base + "steam_machine_pipe");
		this.iconsTopOn[2] = this.iconsTop[2];
		this.iconsTop[3] = reg.registerIcon(base + "steam_machine_pipe");
		this.iconsTopOn[3] = this.iconsTop[3];
		this.iconsTop[4] = reg.registerIcon(base + "steam_machine_pipe");
		this.iconsTopOn[4] = this.iconsTop[4];
		this.iconsTop[5] = reg.registerIcon(base + "steam_shredder_top_bronze");
		this.iconsTopOn[5] = reg.registerIcon(base + "steam_shredder_top_bronze_on");
		this.iconsTop[6] = reg.registerIcon(base + "steam_machine_pipe_bronze");
		this.iconsTopOn[6] = this.iconsTop[6];
		this.iconsTop[7] = reg.registerIcon(base + "steam_machine_pipe_bronze");
		this.iconsTopOn[7] = this.iconsTop[7];
		this.iconsTop[8] = reg.registerIcon(base + "osmiridium_furance_top");
		this.iconsTopOn[8] = this.iconsTop[8];

		this.iconsBack[0] = this.iconsTop[0];
		this.iconsBackOn[0] = this.iconsTopOn[0];
		this.iconsBack[1] = this.iconsTop[0];
		this.iconsBackOn[1] = this.iconsTopOn[0];
		this.iconsBack[2] = this.iconsTop[2];
		this.iconsBackOn[2] = this.iconsTopOn[2];
		this.iconsBack[3] = this.iconsTop[3];
		this.iconsBackOn[3] = this.iconsTopOn[3];
		this.iconsBack[4] = this.iconsTop[4];
		this.iconsBackOn[4] = this.iconsTopOn[4];
		this.iconsBack[5] = reg.registerIcon(base + "steam_machine_pipe_bronze");
		this.iconsBackOn[5] = this.iconsBack[5];
		this.iconsBack[6] = this.iconsTop[6];
		this.iconsBackOn[6] = this.iconsTopOn[6];
		this.iconsBack[7] = this.iconsTop[7];
		this.iconsBackOn[7] = this.iconsTopOn[7];
		this.iconsBack[8] = this.iconsTop[8];
		this.iconsBackOn[8] = this.iconsTopOn[8];

		this.iconSide = reg.registerIcon(base + "steam_machine_base");
		this.iconsSide[0] = this.iconSide;
		this.iconsSideOn[0] = this.iconSide;
		this.iconsSide[1] = this.iconSide;
		this.iconsSideOn[1] = this.iconSide;
		this.iconsSide[2] = this.iconSide;
		this.iconsSideOn[2] = this.iconSide;
		this.iconsSide[3] = reg.registerIcon(base + "steam_machine_base");
		this.iconsSideOn[3] = this.iconsSide[3];
		this.iconsSide[4] = reg.registerIcon(base + "machine_boiler_side");
		this.iconsSideOn[4] = this.iconsSide[4];
		this.iconsSide[5] = reg.registerIcon(base + "steam_machine_base_bronze");
		this.iconsSideOn[5] = this.iconsSide[5];
		this.iconsSide[6] = reg.registerIcon(base + "steam_machine_base_bronze");
		this.iconsSideOn[6] = this.iconsSide[6];
		this.iconsSide[7] = reg.registerIcon(base + "machine_boiler_side_bronze");
		this.iconsSideOn[7] = this.iconsSide[7];
		this.iconsSide[8] = reg.registerIcon(base + "osmiridium_furnace_base");
		this.iconsSideOn[8] = this.iconsSide[8];
		this.blockIcon = this.iconSide;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		int type = getTypeIndex(meta);

		if(type < 0 || type >= SteamMachineType.values().length) type = 0;

		if(side == 0) {
			return iconsBack[type];
		} else if(side == 1) {
			return iconsTop[type];
		} else if(side == 3) {
			return iconsFront[type];
		} else if(side == 2) {
			return iconsBack[type];
		} else {
			return iconsSide[type];
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
		int meta = world.getBlockMetadata(x, y, z);
		int type = getTypeIndex(meta);
		ForgeDirection front = ForgeDirection.NORTH;
		boolean isOn = false;
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntitySteamMachineBase) {
			front = ((TileEntitySteamMachineBase) te).getFrontDirection();
			isOn = ((TileEntitySteamMachineBase) te).steamConsumedLastTick > 0;
		} else if(te instanceof TileEntitySteamBoiler) {
			front = ((TileEntitySteamBoiler) te).getFrontDirection();
			isOn = ((TileEntitySteamBoiler) te).burnTime > 0;
		}

		if(type < 0 || type >= SteamMachineType.values().length) type = 0;

		if(side == 0) {
			return isOn ? iconsBackOn[type] : iconsBack[type];
		} else if(side == front.ordinal()) {
			return isOn ? iconsFrontOn[type] : iconsFront[type];
		} else if(side == front.getOpposite().ordinal()) {
			return isOn ? iconsBackOn[type] : iconsBack[type];
		} else if(side == 1) {
			return isOn ? iconsTopOn[type] : iconsTop[type];
		} else {
			return isOn ? iconsSideOn[type] : iconsSide[type];
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
		int meta = world.getBlockMetadata(x, y, z);
		int type = getTypeIndex(meta);
		TileEntity te = world.getTileEntity(x, y, z);

		boolean active = false;
		ForgeDirection dir = ForgeDirection.NORTH;

		if(te instanceof TileEntitySteamMachineBase) {
			active = ((TileEntitySteamMachineBase) te).steamConsumedLastTick > 0;
			dir = ((TileEntitySteamMachineBase) te).getFrontDirection();
		} else if(te instanceof TileEntitySteamBoiler) {
			active = ((TileEntitySteamBoiler) te).burnTime > 0;
			dir = ((TileEntitySteamBoiler) te).getFrontDirection();
		}

		if(active) {
			float cX = x + 0.5F;
			float cY = y + rand.nextFloat() * 0.375F;
			float cZ = z + 0.5F;
			float off = 0.52F;
			float var = rand.nextFloat() * 0.6F - 0.3F;

			if(type == 0 || type == 4 || type == 7 || type == 8) {
				if(dir == ForgeDirection.WEST) {
					world.spawnParticle("smoke", cX - off, cY, cZ + var, 0.0D, 0.0D, 0.0D);
					world.spawnParticle("flame", cX - off, cY, cZ + var, 0.0D, 0.0D, 0.0D);
				} else if(dir == ForgeDirection.EAST) {
					world.spawnParticle("smoke", cX + off, cY, cZ + var, 0.0D, 0.0D, 0.0D);
					world.spawnParticle("flame", cX + off, cY, cZ + var, 0.0D, 0.0D, 0.0D);
				} else if(dir == ForgeDirection.NORTH) {
					world.spawnParticle("smoke", cX + var, cY, cZ - off, 0.0D, 0.0D, 0.0D);
					world.spawnParticle("flame", cX + var, cY, cZ - off, 0.0D, 0.0D, 0.0D);
				} else if(dir == ForgeDirection.SOUTH) {
					world.spawnParticle("smoke", cX + var, cY, cZ + off, 0.0D, 0.0D, 0.0D);
					world.spawnParticle("flame", cX + var, cY, cZ + off, 0.0D, 0.0D, 0.0D);
				}
			}
		}
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
		int type = stack != null ? stack.getItemDamage() : 0;
		if(type < 0 || type >= SteamMachineType.values().length) type = 0;

		int rot = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		ForgeDirection dir = FACING_DIR[rot];

		int meta = packMeta(type, rot);
		world.setBlockMetadataWithNotify(x, y, z, meta, 2);

		world.removeTileEntity(x, y, z);
		TileEntity te = createNewTileEntity(world, meta);
		if(te instanceof TileEntitySteamMachineBase) {
			((TileEntitySteamMachineBase) te).setFrontDirection(dir);
		} else if(te instanceof TileEntitySteamBoiler) {
			((TileEntitySteamBoiler) te).setFrontDirection(dir);
		}
		world.setTileEntity(x, y, z, te);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(player.getHeldItem() != null && IToolable.ToolType.getType(player.getHeldItem()) == ToolType.WRENCH) {
			return false;
		}

		if(world.isRemote) return true;
		if(player.isSneaking()) return false;

		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof com.hbm.tileentity.IGUIProvider) {
			FMLNetworkHandler.openGui(player, MainRegistry.instance, getTypeIndex(world.getBlockMetadata(x, y, z)), world, x, y, z);
			return true;
		}
		return false;
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, List list) {
		for(int i = 0; i < SteamMachineType.values().length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		int type = stack != null ? stack.getItemDamage() : 0;
		if(type < 0 || type >= SteamMachineType.values().length) type = 0;
		return super.getUnlocalizedName() + "." + SteamMachineType.values()[type].name().toLowerCase();
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntityMachineBase) {
			dropInventory(world, x, y, z, (TileEntityMachineBase) te);
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	private void dropInventory(World world, int x, int y, int z, TileEntityMachineBase te) {
		for(int i = 0; i < te.getSizeInventory(); i++) {
			ItemStack stack = te.getStackInSlot(i);
			if(stack == null) continue;

			while(stack.stackSize > 0) {
				int amount = world.rand.nextInt(21) + 10;
				if(amount > stack.stackSize) amount = stack.stackSize;
				stack.stackSize -= amount;

				ItemStack drop = new ItemStack(stack.getItem(), amount, stack.getItemDamage());
				if(stack.hasTagCompound()) {
					drop.setTagCompound((NBTTagCompound) stack.getTagCompound().copy());
				}

				EntityItem entity = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, drop);
				double speed = 0.05D;
				entity.motionX = world.rand.nextGaussian() * speed;
				entity.motionY = world.rand.nextGaussian() * speed + 0.2D;
				entity.motionZ = world.rand.nextGaussian() * speed;
				world.spawnEntityInWorld(entity);
			}
		}
	}
}
