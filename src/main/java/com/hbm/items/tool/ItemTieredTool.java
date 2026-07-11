package com.hbm.items.tool;

import java.util.List;
import java.util.Locale;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.IBlockSideRotation;
import com.hbm.blocks.machine.BlockMachineBase;
import com.hbm.blocks.machine.MachineSteamMulti;
import com.hbm.blocks.network.CableDiode;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.machine.steam.TileEntitySteamBoiler;
import com.hbm.tileentity.machine.steam.TileEntitySteamMachineBase;
import com.hbm.tileentity.network.TileEntityPipelineBase;

import api.hbm.block.IToolable;
import api.hbm.block.IToolable.ToolType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class ItemTieredTool extends Item {

	public static enum Role {
		WRENCH("wrench", "Wrench", 3, 120, true, true, false, ToolType.WRENCH),
		HAMMER("hammer", "Hammer", 3, 128, false, true, true, null),
		MORTAR("mortar", "Mortar", 4, 32, false, true, true, null),
		SCREWDRIVER("screwdriver", "Screwdriver", 3, 96, true, true, false, ToolType.SCREWDRIVER),
		DRILL("hand_drill", "Hand Drill", 3, 180, true, true, false, ToolType.HAND_DRILL),
		FILE("file", "File", 2, 64, false, true, true, null),
		WIRECUTTER("wirecutter", "Wire Cutter", 2, 96, false, true, false, null);

		public final String baseName;
		public final String displayName;
		public final int tiers;
		public final int baseDurability;
		public final boolean registerToolType;
		public final boolean showTierInName;
		public final boolean showTierInTooltip;
		public final ToolType toolType;

		private Role(String baseName, String displayName, int tiers, int baseDurability, boolean registerToolType, boolean showTierInName, boolean showTierInTooltip, ToolType toolType) {
			this.baseName = baseName;
			this.displayName = displayName;
			this.tiers = tiers;
			this.baseDurability = baseDurability;
			this.registerToolType = registerToolType;
			this.showTierInName = showTierInName;
			this.showTierInTooltip = showTierInTooltip;
			this.toolType = toolType;
		}
	}

	public final Role role;
	private final String[] tierNames;
	private final int[] tierLevels;
	private final int[] durability;
	@SideOnly(Side.CLIENT) private IIcon[] icons;

	public ItemTieredTool(Role role, String[] tierNames, int[] tierLevels, int[] durability) {
		this.role = role;
		this.tierNames = tierNames.clone();
		this.tierLevels = tierLevels == null ? buildDefaultTierLevels(tierNames.length) : tierLevels.clone();
		this.durability = durability.clone();
		this.setHasSubtypes(true);
		this.setMaxStackSize(1);
		this.setNoRepair();
		this.setCreativeTab(MainRegistry.partsTab);
		this.setFull3D();

		if(this.tierNames.length > 0) {
			this.setTextureName(RefStrings.MODID + ":" + role.baseName + "_" + this.getTextureTierName(0));
		}

		if(role.registerToolType) {
			for(int i = 0; i < this.tierNames.length; i++) {
				ToolType toolType = role.toolType;
				if(toolType != null) {
					toolType.register(new ItemStack(this, 1, i));
				}
			}
		}
	}

	public int getTierLevel(int meta) {
		if(meta < 0 || meta >= this.tierLevels.length) return this.tierLevels[0];
		return this.tierLevels[meta];
	}

	public String getTierName(int meta) {
		if(meta < 0 || meta >= this.tierNames.length) return this.tierNames[0];
		return this.tierNames[meta];
	}

	public int getMaxToolDamage(ItemStack stack) {
		int meta = this.clampMeta(stack.getItemDamage());
		return this.durability[meta];
	}

	private static int[] buildDefaultTierLevels(int length) {
		int[] ret = new int[length];
		for(int i = 0; i < length; i++) ret[i] = i;
		return ret;
	}

	private String getMaterialLabel(int meta) {
		String name = this.getTierName(meta);
//		if("bronze".equals(name)) return "Tin Bronze";
		if("flint".equals(name)) return "Flint";
		if("ferrouranium".equals(name)) return "Ironuranium";
		if(name == null || name.isEmpty()) return this.role.displayName;
		return name.substring(0, 1).toUpperCase(Locale.US) + name.substring(1);
	}

	private String getTextureTierName(int meta) {
		String name = this.getTierName(meta);
		return name;
	}

	private int getStoredDamage(ItemStack stack) {
		if(stack == null || !stack.hasTagCompound()) return 0;
		return stack.stackTagCompound.getInteger("wear");
	}

	private void setStoredDamage(ItemStack stack, int damage) {
		if(stack == null) return;
		if(stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setInteger("wear", Math.max(0, damage));
	}

	private void damageTool(ItemStack stack, EntityLivingBase owner, int amount) {
		if(stack == null || amount <= 0) return;
		if(owner instanceof EntityPlayer && ((EntityPlayer) owner).capabilities.isCreativeMode) return;

		int wear = getStoredDamage(stack) + amount;
		int max = getMaxToolDamage(stack);

		if(max > 0 && wear >= max) {
			stack.stackSize = Math.max(0, stack.stackSize - 1);
			return;
		}

		setStoredDamage(stack, wear);
	}

	private int clampMeta(int meta) {
		if(meta < 0) return 0;
		if(meta >= this.tierNames.length) return this.tierNames.length - 1;
		return meta;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName() + "." + getTierName(stack.getItemDamage());
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		if(!this.role.showTierInName) return this.role.displayName;
		return this.getMaterialLabel(stack.getItemDamage()) + " " + this.role.displayName;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tabs, List list) {
		for(int i = 0; i < this.tierNames.length; i++) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		this.icons = new IIcon[this.tierNames.length];
		for(int i = 0; i < this.tierNames.length; i++) {
			this.icons[i] = reg.registerIcon(RefStrings.MODID + ":" + this.role.baseName + "_" + this.getTextureTierName(i));
		}
		this.itemIcon = this.icons.length > 0 ? this.icons[0] : super.getIconFromDamage(0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int meta) {
		if(this.icons == null || this.icons.length == 0) return this.itemIcon;
		return this.icons[this.clampMeta(meta)];
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		int max = this.getMaxToolDamage(stack);
		if(max <= 0) return false;
		return this.getStoredDamage(stack) > 0;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		int max = this.getMaxToolDamage(stack);
		if(max <= 0) return 0.0D;
		return (double) this.getStoredDamage(stack) / (double) max;
	}

	@Override
	public boolean hasContainerItem(ItemStack stack) {
		int max = this.getMaxToolDamage(stack);
		if(max <= 0) return true;
		return this.getStoredDamage(stack) < max;
	}

	@Override
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack getContainerItem(ItemStack stack) {
		if(stack == null) return null;

		int max = this.getMaxToolDamage(stack);
		if(max <= 0) return stack.copy();
		int wear = this.getStoredDamage(stack) + 1;

		if(wear >= max) return null;

		ItemStack copy = stack.copy();
		this.setStoredDamage(copy, wear);
		return copy;
	}

	private ForgeDirection getFacingFromSide(int side) {
		if(side == 0) return ForgeDirection.UP;
		if(side == 1) return ForgeDirection.DOWN;
		if(side == 2) return ForgeDirection.NORTH;
		if(side == 3) return ForgeDirection.SOUTH;
		if(side == 4) return ForgeDirection.WEST;
		if(side == 5) return ForgeDirection.EAST;
		return ForgeDirection.NORTH;
	}

	private int getMetadataFromFacing(ForgeDirection dir) {
		if(dir == ForgeDirection.UP) return 1;
		if(dir == ForgeDirection.DOWN) return 0;
		if(dir == ForgeDirection.NORTH) return 2;
		if(dir == ForgeDirection.SOUTH) return 3;
		if(dir == ForgeDirection.WEST) return 4;
		if(dir == ForgeDirection.EAST) return 5;
		return 2;
	}

	private boolean rotateBlock(World world, int x, int y, int z, Block block, int side, boolean sneak) {
		TileEntity te = world.getTileEntity(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);

		if(block instanceof MachineSteamMulti || block instanceof BlockMachineBase || block instanceof IBlockSideRotation) {
			if(side == 0 || side == 1) {
				return false;
			}
		}

		ForgeDirection target;
		if(sneak) {
			ForgeDirection clicked = this.getFacingFromSide(side);
			target = clicked.getOpposite();
		} else {
			target = this.getFacingFromSide(side);
		}
		int targetMeta = this.getMetadataFromFacing(target);

		if(block instanceof CableDiode) {
			if(sneak && meta == targetMeta) {
				if(!world.isRemote) {
					Item droppedItem = Item.getItemFromBlock(block);
					if(droppedItem != null) {
						ItemStack drop = new ItemStack(droppedItem, 1, meta);
						world.setBlockToAir(x, y, z);
						EntityItem item = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, drop);
						world.spawnEntityInWorld(item);
					}
				}
				return true;
			}

			if(!world.isRemote) {
				world.setBlockMetadataWithNotify(x, y, z, targetMeta, 3);
				world.markBlockForUpdate(x, y, z);
				world.notifyBlocksOfNeighborChange(x, y, z, block);
			}
			return true;
		}

		if(block instanceof MachineSteamMulti) {
			if(te instanceof TileEntitySteamMachineBase) {
				TileEntitySteamMachineBase machine = (TileEntitySteamMachineBase) te;
				ForgeDirection current = machine.getFrontDirection();

				if(sneak && current == target) {
					if(!world.isRemote) {
						int type = MachineSteamMulti.getTypeIndex(meta);
						Item droppedItem = Item.getItemFromBlock(block);
						if(droppedItem != null) {
							ItemStack drop = new ItemStack(droppedItem, 1, type);
							world.setBlockToAir(x, y, z);
							EntityItem item = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, drop);
							world.spawnEntityInWorld(item);
						}
					}
					return true;
				}

				if(!world.isRemote) {
					machine.setFrontDirection(target);
					int rot = (target == ForgeDirection.EAST || target == ForgeDirection.WEST) ? 1 : 0;
					int type = MachineSteamMulti.getTypeIndex(meta);
					int packed = MachineSteamMulti.packMeta(type, rot);
					world.setBlockMetadataWithNotify(x, y, z, packed, 3);
					world.markBlockForUpdate(x, y, z);
					world.notifyBlocksOfNeighborChange(x, y, z, block);
				}
				return true;
			}

			if(te instanceof TileEntitySteamBoiler) {
				TileEntitySteamBoiler boiler = (TileEntitySteamBoiler) te;
				ForgeDirection current = boiler.getFrontDirection();

				if(sneak && current == target) {
					if(!world.isRemote) {
						int type = MachineSteamMulti.getTypeIndex(meta);
						Item droppedItem = Item.getItemFromBlock(block);
						if(droppedItem != null) {
							ItemStack drop = new ItemStack(droppedItem, 1, type);
							world.setBlockToAir(x, y, z);
							EntityItem item = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, drop);
							world.spawnEntityInWorld(item);
						}
					}
					return true;
				}

				if(!world.isRemote) {
					boiler.setFrontDirection(target);
					int rot = (target == ForgeDirection.EAST || target == ForgeDirection.WEST) ? 1 : 0;
					int type = MachineSteamMulti.getTypeIndex(meta);
					int packed = MachineSteamMulti.packMeta(type, rot);
					world.setBlockMetadataWithNotify(x, y, z, packed, 3);
					world.markBlockForUpdate(x, y, z);
					world.notifyBlocksOfNeighborChange(x, y, z, block);
				}
				return true;
			}
		}

		if(block instanceof BlockMachineBase || block instanceof IBlockSideRotation) {
			if(sneak && meta == targetMeta) {
				if(!world.isRemote) {
					Item droppedItem = Item.getItemFromBlock(block);
					if(droppedItem != null) {
						ItemStack drop = new ItemStack(droppedItem, 1, meta);
						world.setBlockToAir(x, y, z);
						EntityItem item = new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, drop);
						world.spawnEntityInWorld(item);
					}
				}
				return true;
			}

			if(!world.isRemote) {
				if(te instanceof TileEntitySteamMachineBase) {
					((TileEntitySteamMachineBase) te).setFrontDirection(target);
				} else if(te instanceof TileEntitySteamBoiler) {
					((TileEntitySteamBoiler) te).setFrontDirection(target);
				}

				world.setBlockMetadataWithNotify(x, y, z, targetMeta, 3);
				world.markBlockForUpdate(x, y, z);
			}
			return true;
		}

		return false;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float fX, float fY, float fZ) {
		Block block = world.getBlock(x, y, z);

		if(block instanceof BlockDummyable) {
			int[] core = ((BlockDummyable) block).findCore(world, x, y, z);
			if(core != null) {
				x = core[0];
				y = core[1];
				z = core[2];
				block = world.getBlock(x, y, z);
			}
		}

		if(this.role == Role.WRENCH) {
			TileEntity te = world.getTileEntity(x, y, z);

			if(te instanceof TileEntityPipelineBase) {
				if(stack.stackTagCompound == null) {
					stack.stackTagCompound = new NBTTagCompound();
					stack.stackTagCompound.setInteger("x", x);
					stack.stackTagCompound.setInteger("y", y);
					stack.stackTagCompound.setInteger("z", z);

					if(!world.isRemote) player.addChatMessage(new ChatComponentText("Pipe start"));
				} else if(!world.isRemote) {
					int x1 = stack.stackTagCompound.getInteger("x");
					int y1 = stack.stackTagCompound.getInteger("y");
					int z1 = stack.stackTagCompound.getInteger("z");

					if(world.getTileEntity(x1, y1, z1) instanceof TileEntityPipelineBase) {
						TileEntityPipelineBase first = (TileEntityPipelineBase) world.getTileEntity(x1, y1, z1);
						TileEntityPipelineBase second = (TileEntityPipelineBase) te;

						switch(TileEntityPipelineBase.canConnect(first, second)) {
							case 0:
								first.addConnection(x, y, z);
								second.addConnection(x1, y1, z1);
								player.addChatMessage(new ChatComponentText("Pipe end"));
								stack.stackTagCompound = null;
								break;
							case 1:
								player.addChatMessage(new ChatComponentText("Pipe error - Pipes are not the same type"));
								break;
							case 2:
								player.addChatMessage(new ChatComponentText("Pipe error - Cannot connect to the same pipe anchor"));
								break;
							case 3:
								player.addChatMessage(new ChatComponentText("Pipe error - Pipe anchor is too far away"));
								break;
							case 4:
								player.addChatMessage(new ChatComponentText("Pipe error - Pipe anchor fluid types do not match"));
								break;
						}
					} else {
						player.addChatMessage(new ChatComponentText("Pipe error"));
						stack.stackTagCompound = null;
					}
				}

				player.swingItem();
				if(!world.isRemote) this.damageTool(stack, player, 1);
				return true;
			}

			if(this.rotateBlock(world, x, y, z, block, side, player.isSneaking())) {
				if(!world.isRemote) this.damageTool(stack, player, 1);
				return true;
			}
		}

		if(this.role == Role.HAMMER || this.role == Role.MORTAR || this.role == Role.SCREWDRIVER || this.role == Role.DRILL || this.role == Role.FILE || this.role == Role.WIRECUTTER) {
			if(block instanceof IToolable) {
				ToolType type = this.role.toolType;
				if(type != null && ((IToolable) block).onScrew(world, player, x, y, z, side, fX, fY, fZ, type)) {
					if(!world.isRemote) this.damageTool(stack, player, 1);
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase entity, EntityLivingBase player) {
		if(this.role == Role.WRENCH) {
			World world = entity.worldObj;
			Vec3 vec = player.getLookVec();

			double dX = vec.xCoord * 0.5D;
			double dY = vec.yCoord * 0.5D;
			double dZ = vec.zCoord * 0.5D;

			entity.motionX += dX;
			entity.motionY += dY;
			entity.motionZ += dZ;
			world.playSoundAtEntity(entity, "random.anvil_land", 3.0F, 0.75F);
		}

		if(!player.worldObj.isRemote) this.damageTool(stack, player, 1);
		return true;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		int meta = this.clampMeta(stack.getItemDamage());
		if(this.role.showTierInName) {
			list.add(EnumChatFormatting.GRAY + "Material: " + EnumChatFormatting.YELLOW + this.getMaterialLabel(meta));
		}
		if(this.role.showTierInTooltip) {
			list.add(EnumChatFormatting.GRAY + "Tier: " + EnumChatFormatting.YELLOW + this.getTierLevel(meta));
		}

		int max = this.getMaxToolDamage(stack);
		if (max > 0) {
			int wear = this.getStoredDamage(stack);
			list.add(EnumChatFormatting.GRAY + "Durability: " + EnumChatFormatting.YELLOW + Math.max(0, max - wear) + "/" + max);
		}
	}
}
