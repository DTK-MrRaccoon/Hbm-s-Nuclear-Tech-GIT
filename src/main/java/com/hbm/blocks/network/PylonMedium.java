package com.hbm.blocks.network;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.tileentity.IRepairable;
import com.hbm.tileentity.network.TileEntityPylonBase;
import com.hbm.tileentity.network.TileEntityPylonMedium;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;
import com.hbm.blocks.ModBlocks;

import api.hbm.block.IToolable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;

import java.util.ArrayList;
import java.util.List;

public class PylonMedium extends BlockDummyable implements ITooltipProvider, ILookOverlay, IToolable {

	public PylonMedium(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityPylonMedium();
		return null;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		Block block = Block.getBlockFromItem(stack.getItem());
		
		boolean isCon = block == ModBlocks.red_pylon_medium_wood_connector ||
		                block == ModBlocks.red_pylon_medium_concrete_connector||
		                block == ModBlocks.red_pylon_medium_steel_connector;

		boolean isTrans = block == ModBlocks.red_pylon_medium_wood_transformer ||
		                  block == ModBlocks.red_pylon_medium_concrete_transformer ||
		                  block == ModBlocks.red_pylon_medium_steel_transformer;

		if(isCon) {
			list.add(EnumChatFormatting.GOLD + "Connection Type: " + EnumChatFormatting.YELLOW + "Triple + Universal");
			list.add(EnumChatFormatting.GOLD + "Connection Range: " + EnumChatFormatting.YELLOW + "45m / 25m");
			list.add(EnumChatFormatting.GOLD + "Max Capacity: " + EnumChatFormatting.YELLOW + "0.75 MHE/t");
			list.add(EnumChatFormatting.RED + "DANGER: Will melt if overloaded!");
		} else if(isTrans) {
			list.add(EnumChatFormatting.GOLD + "Connection Type: " + EnumChatFormatting.YELLOW + "Triple + Heavy");
			list.add(EnumChatFormatting.GOLD + "Connection Range: " + EnumChatFormatting.YELLOW + "45m");
			list.add(EnumChatFormatting.GOLD + "Max Capacity: " + EnumChatFormatting.YELLOW + "0.75 MHE/t");
			list.add(EnumChatFormatting.RED + "DANGER: Will melt if overloaded!");
		} else {
			list.add(EnumChatFormatting.GOLD + "Connection Type: " + EnumChatFormatting.YELLOW + "Triple");
			list.add(EnumChatFormatting.GOLD + "Connection Range: " + EnumChatFormatting.YELLOW + "45m");
		}
	}

	@Override
	public int[] getDimensions() {
		return new int[] {6, 0, 0, 0, 0, 0};
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block b, int m) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntityPylonBase) ((TileEntityPylonBase)te).disconnectAll();
		super.breakBlock(world, x, y, z, b, m);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {
			return true;
		} else if(!player.isSneaking()) {
			int[] pos = this.findCore(world, x, y, z);
			if(pos == null) return false;

			TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

			if(te instanceof TileEntityPylonBase) {
				return ((TileEntityPylonBase)te).setColor(player.getHeldItem());
			}
		}
		return false;
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
		if(tool == ToolType.TORCH) {
			return IRepairable.tryRepairMultiblock(world, x, y, z, this, player);
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) return;
		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

		if(te instanceof TileEntityPylonMedium) {
			TileEntityPylonMedium pylon = (TileEntityPylonMedium) te;

			if(!pylon.hasTransformer() && !pylon.isConnector()) return;

			List<AStack> mats = IRepairable.getRepairMaterials(world, x, y, z, this, net.minecraft.client.Minecraft.getMinecraft().thePlayer);

			if (mats != null && pylon.isBroken) {
				IRepairable.addGenericOverlay(event, world, x, y, z, this);
				return;
			}

			List<String> text = new ArrayList<>();
			text.add(EnumChatFormatting.GOLD + "Power Transfer: " + EnumChatFormatting.YELLOW + BobMathUtil.getShortNumber(pylon.powerTransfer) + " HE/t");
			text.add(EnumChatFormatting.GOLD + "Max Transfer: " + EnumChatFormatting.RED + BobMathUtil.getShortNumber(TileEntityPylonMedium.MAX_POWER) + " HE/t");

			String tempColor = EnumChatFormatting.GREEN.toString();
			if(pylon.temperature > 100) tempColor = EnumChatFormatting.YELLOW.toString();
			if(pylon.temperature > 300) tempColor = EnumChatFormatting.RED.toString();

			text.add(EnumChatFormatting.GOLD + "Temperature: " + tempColor + (int)pylon.temperature + " / " + (int)TileEntityPylonMedium.MAX_TEMP + " °C");

			if(pylon.isBroken) {
				text.add(EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "BROKEN - CRITICAL FAILURE");
			}

			ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
		}
	}
}
