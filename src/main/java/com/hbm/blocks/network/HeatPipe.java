package com.hbm.blocks.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.network.TileEntityHeatPipe;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class HeatPipe extends FluidDuctBase implements ILookOverlay, ITooltipProvider {

	@SideOnly(Side.CLIENT) public IIcon iconStraight;
	@SideOnly(Side.CLIENT) public IIcon iconEnd;
	@SideOnly(Side.CLIENT) public IIcon iconJunction;
	@SideOnly(Side.CLIENT) public IIcon iconCurveTL;
	@SideOnly(Side.CLIENT) public IIcon iconCurveTR;
	@SideOnly(Side.CLIENT) public IIcon iconCurveBL;
	@SideOnly(Side.CLIENT) public IIcon iconCurveBR;

	@SideOnly(Side.CLIENT) public IIcon iconStraightAsbestos;
	@SideOnly(Side.CLIENT) public IIcon iconEndAsbestos;
	@SideOnly(Side.CLIENT) public IIcon iconJunctionAsbestos;
	@SideOnly(Side.CLIENT) public IIcon iconCurveTLAsbestos;
	@SideOnly(Side.CLIENT) public IIcon iconCurveTRAsbestos;
	@SideOnly(Side.CLIENT) public IIcon iconCurveBLAsbestos;
	@SideOnly(Side.CLIENT) public IIcon iconCurveBRAsbestos;

	@SideOnly(Side.CLIENT) public IIcon iconStraightPolymer;
	@SideOnly(Side.CLIENT) public IIcon iconEndPolymer;
	@SideOnly(Side.CLIENT) public IIcon iconJunctionPolymer;
	@SideOnly(Side.CLIENT) public IIcon iconCurveTLPolymer;
	@SideOnly(Side.CLIENT) public IIcon iconCurveTRPolymer;
	@SideOnly(Side.CLIENT) public IIcon iconCurveBLPolymer;
	@SideOnly(Side.CLIENT) public IIcon iconCurveBRPolymer;

	public static int renderID = -1;

	public HeatPipe() {
		super(Material.iron);
		this.setBlockName("heat_pipe");
		this.setCreativeTab(MainRegistry.machineTab);
		this.setHardness(3.0F);
		this.setResistance(15.0F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		String base = "boxduct_copper";

		this.iconStraight = reg.registerIcon(RefStrings.MODID + ":" + base + "_straight");
		this.iconEnd = reg.registerIcon(RefStrings.MODID + ":" + base + "_end");
		this.iconJunction = reg.registerIcon(RefStrings.MODID + ":" + base + "_junction_0");
		this.iconCurveTL = reg.registerIcon(RefStrings.MODID + ":" + base + "_curve_tl");
		this.iconCurveTR = reg.registerIcon(RefStrings.MODID + ":" + base + "_curve_tr");
		this.iconCurveBL = reg.registerIcon(RefStrings.MODID + ":" + base + "_curve_bl");
		this.iconCurveBR = reg.registerIcon(RefStrings.MODID + ":" + base + "_curve_br");

		this.iconStraightAsbestos = reg.registerIcon(RefStrings.MODID + ":" + base + "_asbestos_straight");
		this.iconEndAsbestos = reg.registerIcon(RefStrings.MODID + ":" + base + "_asbestos_end");
		this.iconJunctionAsbestos = reg.registerIcon(RefStrings.MODID + ":" + base + "_asbestos_junction_0");
		this.iconCurveTLAsbestos = reg.registerIcon(RefStrings.MODID + ":" + base + "_asbestos_curve_tl");
		this.iconCurveTRAsbestos = reg.registerIcon(RefStrings.MODID + ":" + base + "_asbestos_curve_tr");
		this.iconCurveBLAsbestos = reg.registerIcon(RefStrings.MODID + ":" + base + "_asbestos_curve_bl");
		this.iconCurveBRAsbestos = reg.registerIcon(RefStrings.MODID + ":" + base + "_asbestos_curve_br");

		this.iconStraightPolymer = reg.registerIcon(RefStrings.MODID + ":" + base + "_polymer_straight");
		this.iconEndPolymer = reg.registerIcon(RefStrings.MODID + ":" + base + "_polymer_end");
		this.iconJunctionPolymer = reg.registerIcon(RefStrings.MODID + ":" + base + "_polymer_junction_0");
		this.iconCurveTLPolymer = reg.registerIcon(RefStrings.MODID + ":" + base + "_polymer_curve_tl");
		this.iconCurveTRPolymer = reg.registerIcon(RefStrings.MODID + ":" + base + "_polymer_curve_tr");
		this.iconCurveBLPolymer = reg.registerIcon(RefStrings.MODID + ":" + base + "_polymer_curve_bl");
		this.iconCurveBRPolymer = reg.registerIcon(RefStrings.MODID + ":" + base + "_polymer_curve_br");

		this.blockIcon = this.iconStraight;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityHeatPipe();
	}

	@Override
	public int getRenderType() {
		return renderID;
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
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		return true;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float fX, float fY, float fZ) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityHeatPipe)) return false;

		TileEntityHeatPipe pipe = (TileEntityHeatPipe) te;
		ItemStack held = player.getHeldItem();

		if(held != null) {
			if(held.getItem() == ModItems.ingot_asbestos && pipe.insulation == 0) {
				if(!world.isRemote) {
					pipe.insulation = 1;
					held.stackSize--;
					world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "dig.cloth", 1.0F, 1.0F);
					pipe.markDirty();
					world.markBlockForUpdate(x, y, z);
				}
				return true;
			}
			if(held.getItem() == ModItems.plate_polymer && pipe.insulation == 0) {
				if(!world.isRemote) {
					pipe.insulation = 2;
					held.stackSize--;
					world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "dig.cloth", 1.0F, 1.2F);
					pipe.markDirty();
					world.markBlockForUpdate(x, y, z);
				}
				return true;
			}
			if(held.getItem() == ModItems.screwdriver && pipe.insulation > 0) {
				if(!world.isRemote) {
					Item dropItem = pipe.insulation == 1 ? ModItems.ingot_asbestos : ModItems.plate_polymer;
					world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, new ItemStack(dropItem)));
					pipe.insulation = 0;
					pipe.markDirty();
					world.markBlockForUpdate(x, y, z);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntityHeatPipe) {
			((TileEntityHeatPipe) te).updateConnections();
		}
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
		super.onBlockPlacedBy(world, x, y, z, player, stack);
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntityHeatPipe) {
			((TileEntityHeatPipe) te).updateConnections();
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
		TileEntityHeatPipe pipe = (TileEntityHeatPipe) world.getTileEntity(x, y, z);
		if(pipe != null) {
			if(pipe.heat > 0) {
				int heatToDrop = pipe.heat / 1000;
				if(heatToDrop > 0) {
					world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.fizz", 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
				}
			}
			if(pipe.insulation > 0) {
				Item dropItem = pipe.insulation == 1 ? ModItems.ingot_asbestos : ModItems.plate_polymer;
				world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, new ItemStack(dropItem)));
			}
		}
		super.breakBlock(world, x, y, z, block, meta);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if(!world.isRemote && entity instanceof EntityLivingBase) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof TileEntityHeatPipe) {
				TileEntityHeatPipe pipe = (TileEntityHeatPipe) te;
				if(pipe.insulation > 0) {
					if(pipe.heat > 10000) {
						int baseDamage = 1 + pipe.heat / 10000;
						int damage = Math.max(1, (int)(baseDamage * 0.25F));
						entity.attackEntityFrom(DamageSource.inFire, damage);
					}
				} else {
					if(pipe.heat > 500) {
						int damage = 1 + pipe.heat / 10000;
						entity.attackEntityFrom(DamageSource.inFire, damage);
					}
				}
			}
		}
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		this.addStandardInfo(stack, player, list, ext);
	}

	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB entityBounding, List list, Entity entity) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityHeatPipe)) {
			super.addCollisionBoxesToList(world, x, y, z, entityBounding, list, entity);
			return;
		}
		TileEntityHeatPipe pipe = (TileEntityHeatPipe) te;

		double lower = 0.125D;
		double upper = 0.875D;

		boolean pX = pipe.connections[Library.POS_X.ordinal()];
		boolean nX = pipe.connections[Library.NEG_X.ordinal()];
		boolean pY = pipe.connections[Library.POS_Y.ordinal()];
		boolean nY = pipe.connections[Library.NEG_Y.ordinal()];
		boolean pZ = pipe.connections[Library.POS_Z.ordinal()];
		boolean nZ = pipe.connections[Library.NEG_Z.ordinal()];
		int mask = (pX ? 32 : 0) | (nX ? 16 : 0) | (pY ? 8 : 0) | (nY ? 4 : 0) | (pZ ? 2 : 0) | (nZ ? 1 : 0);

		List<AxisAlignedBB> bbs = new ArrayList<>();

		if(mask == 0) {
			bbs.add(AxisAlignedBB.getBoundingBox(x + lower, y + lower, z + lower, x + upper, y + upper, z + upper));
		} else if(mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
			bbs.add(AxisAlignedBB.getBoundingBox(x + 0.0D, y + lower, z + lower, x + 1.0D, y + upper, z + upper));
		} else if(mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
			bbs.add(AxisAlignedBB.getBoundingBox(x + lower, y + 0.0D, z + lower, x + upper, y + 1.0D, z + upper));
		} else if(mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
			bbs.add(AxisAlignedBB.getBoundingBox(x + lower, y + lower, z + 0.0D, x + upper, y + upper, z + 1.0D));
		} else {
			bbs.add(AxisAlignedBB.getBoundingBox(x + lower, y + lower, z + lower, x + upper, y + upper, z + upper));

			if(pX) bbs.add(AxisAlignedBB.getBoundingBox(x + upper, y + lower, z + lower, x + 1.0D, y + upper, z + upper));
			if(nX) bbs.add(AxisAlignedBB.getBoundingBox(x + 0.0D, y + lower, z + lower, x + lower, y + upper, z + upper));
			if(pY) bbs.add(AxisAlignedBB.getBoundingBox(x + lower, y + upper, z + lower, x + upper, y + 1.0D, z + upper));
			if(nY) bbs.add(AxisAlignedBB.getBoundingBox(x + lower, y + 0.0D, z + lower, x + upper, y + lower, z + upper));
			if(pZ) bbs.add(AxisAlignedBB.getBoundingBox(x + lower, y + lower, z + upper, x + upper, y + upper, z + 1.0D));
			if(nZ) bbs.add(AxisAlignedBB.getBoundingBox(x + lower, y + lower, z + 0.0D, x + upper, y + upper, z + lower));
		}

		for(AxisAlignedBB bb : bbs) {
			if(entityBounding.intersectsWith(bb)) {
				list.add(bb);
			}
		}
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityHeatPipe)) {
			this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
			return;
		}
		TileEntityHeatPipe pipe = (TileEntityHeatPipe) te;

		float lower = 0.125F;
		float upper = 0.875F;

		boolean pX = pipe.connections[Library.POS_X.ordinal()];
		boolean nX = pipe.connections[Library.NEG_X.ordinal()];
		boolean pY = pipe.connections[Library.POS_Y.ordinal()];
		boolean nY = pipe.connections[Library.NEG_Y.ordinal()];
		boolean pZ = pipe.connections[Library.POS_Z.ordinal()];
		boolean nZ = pipe.connections[Library.NEG_Z.ordinal()];
		int mask = (pX ? 32 : 0) | (nX ? 16 : 0) | (pY ? 8 : 0) | (nY ? 4 : 0) | (pZ ? 2 : 0) | (nZ ? 1 : 0);

		if(mask == 0) {
			this.setBlockBounds(lower, lower, lower, upper, upper, upper);
		} else if(mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
			this.setBlockBounds(0F, lower, lower, 1F, upper, upper);
		} else if(mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
			this.setBlockBounds(lower, 0F, lower, upper, 1F, upper);
		} else if(mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
			this.setBlockBounds(lower, lower, 0F, upper, upper, 1F);
		} else {
			this.setBlockBounds(
					nX ? 0F : lower,
					nY ? 0F : lower,
					nZ ? 0F : lower,
					pX ? 1F : upper,
					pY ? 1F : upper,
					pZ ? 1F : upper);
		}
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(x + this.minX, y + this.minY, z + this.minZ, x + this.maxX, y + this.maxY, z + this.maxZ);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(x + this.minX, y + this.minY, z + this.minZ, x + this.maxX, y + this.maxY, z + this.maxZ);
	}

	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityHeatPipe)) return this.iconStraight;
		TileEntityHeatPipe pipe = (TileEntityHeatPipe) te;

		IIcon straight = pipe.insulation == 1 ? iconStraightAsbestos : (pipe.insulation == 2 ? iconStraightPolymer : iconStraight);
		IIcon end = pipe.insulation == 1 ? iconEndAsbestos : (pipe.insulation == 2 ? iconEndPolymer : iconEnd);
		IIcon junction = pipe.insulation == 1 ? iconJunctionAsbestos : (pipe.insulation == 2 ? iconJunctionPolymer : iconJunction);
		IIcon curveTL = pipe.insulation == 1 ? iconCurveTLAsbestos : (pipe.insulation == 2 ? iconCurveTLPolymer : iconCurveTL);
		IIcon curveTR = pipe.insulation == 1 ? iconCurveTRAsbestos : (pipe.insulation == 2 ? iconCurveTRPolymer : iconCurveTR);
		IIcon curveBL = pipe.insulation == 1 ? iconCurveBLAsbestos : (pipe.insulation == 2 ? iconCurveBLPolymer : iconCurveBL);
		IIcon curveBR = pipe.insulation == 1 ? iconCurveBRAsbestos : (pipe.insulation == 2 ? iconCurveBRPolymer : iconCurveBR);

		boolean nX = pipe.connections[Library.NEG_X.ordinal()];
		boolean pX = pipe.connections[Library.POS_X.ordinal()];
		boolean nY = pipe.connections[Library.NEG_Y.ordinal()];
		boolean pY = pipe.connections[Library.POS_Y.ordinal()];
		boolean nZ = pipe.connections[Library.NEG_Z.ordinal()];
		boolean pZ = pipe.connections[Library.POS_Z.ordinal()];

		int mask = (pX ? 32 : 0) | (nX ? 16 : 0) | (pY ? 8 : 0) | (nY ? 4 : 0) | (pZ ? 2 : 0) | (nZ ? 1 : 0);
		int count = (pX ? 1 : 0) + (nX ? 1 : 0) + (pY ? 1 : 0) + (nY ? 1 : 0) + (pZ ? 1 : 0) + (nZ ? 1 : 0);

		if((mask & 0b001111) == 0 && mask > 0) {
			return (side == 4 || side == 5) ? end : straight;
		}
		if((mask & 0b111100) == 0 && mask > 0) {
			return (side == 2 || side == 3) ? end : straight;
		}
		if((mask & 0b110011) == 0 && mask > 0) {
			return (side == 0 || side == 1) ? end : straight;
		}
		if(count == 2) {
			if(side == 0 && nY || side == 1 && pY || side == 2 && nZ || side == 3 && pZ || side == 4 && nX || side == 5 && pX)
				return end;
			if(side == 1 && nY || side == 0 && pY || side == 3 && nZ || side == 2 && pZ || side == 5 && nX || side == 4 && pX)
				return straight;

			if(nY && pZ) return side == 4 ? curveBR : curveBL;
			if(nY && nZ) return side == 5 ? curveBR : curveBL;
			if(nY && pX) return side == 3 ? curveBR : curveBL;
			if(nY && nX) return side == 2 ? curveBR : curveBL;
			if(pY && pZ) return side == 4 ? curveTR : curveTL;
			if(pY && nZ) return side == 5 ? curveTR : curveTL;
			if(pY && pX) return side == 3 ? curveTR : curveTL;
			if(pY && nX) return side == 2 ? curveTR : curveTL;

			if(pX && nZ) return side == 0 ? curveTR : curveTR;
			if(pX && pZ) return side == 0 ? curveBR : curveBR;
			if(nX && nZ) return side == 0 ? curveTL : curveTL;
			if(nX && pZ) return side == 0 ? curveBL : curveBL;
		}
		return junction;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(!(te instanceof TileEntityHeatPipe)) return;

		TileEntityHeatPipe pipe = (TileEntityHeatPipe) te;

		List<String> text = new ArrayList<>();

		text.add(EnumChatFormatting.GOLD + "Heat: " + EnumChatFormatting.RESET + BobMathUtil.getShortNumber(pipe.heat) + " TU");

		float loss = pipe.getCurrentLoss();
		String lossColor = pipe.isInsulated() ? EnumChatFormatting.GREEN.toString() : EnumChatFormatting.RED.toString();
		text.add(lossColor + "Loss: " + EnumChatFormatting.RESET + String.format("%.1f", loss * 100) + "% per tick");

		String insulString = pipe.insulation == 0 ? "None" : (pipe.insulation == 1 ? "Asbestos" : "Polymer");
		text.add(EnumChatFormatting.LIGHT_PURPLE + "Insulation: " + EnumChatFormatting.RESET + insulString);

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey("tile.heat_pipe.name"), 0xff6600, 0x402000, text);
	}
}