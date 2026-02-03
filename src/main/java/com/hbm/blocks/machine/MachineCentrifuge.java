package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;

import com.hbm.blocks.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;

public class MachineCentrifuge extends BlockDummyable implements ILookOverlay {

	public MachineCentrifuge(Material mat) {
		super(mat);
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.5D, 0D, -0.5D, 0.5D, 1D, 0.5D));
		this.bounding.add(AxisAlignedBB.getBoundingBox(-0.375D, 1D, -0.375D, 0.375D, 4D, 0.375D));
		this.maxY = 0.999D; //item bounce prevention
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int meta) {

		if(meta >= 12)
			return new TileEntityMachineCentrifuge();
		if(meta >= 6)
			return new TileEntityProxyCombo(false, true, true);

		return null;
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {
			return true;
		} else if(!player.isSneaking()) {
			int[] pos = this.findCore(world, x, y, z);

			if(pos == null)
				return false;

			FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, pos[0], pos[1], pos[2]);
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int[] getDimensions() {
		return new int[] {3, 0, 0, 0, 0, 0,}; 
	}

	@Override
	public int getOffset() {
		return 0;
	}
	
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = this.findCore(world, x, y, z);

		if(pos == null)
			return;

		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

		if(!(te instanceof TileEntityMachineCentrifuge))
			return;

		TileEntityMachineCentrifuge centrifuge = (TileEntityMachineCentrifuge) te;

		List<String> text = new ArrayList<String>();
		String powerColor = (centrifuge.power < TileEntityMachineCentrifuge.maxPower / 20 ? EnumChatFormatting.RED : EnumChatFormatting.GREEN).toString();
		text.add(powerColor + "Power: " + BobMathUtil.getShortNumber(centrifuge.power) + " / " + BobMathUtil.getShortNumber(TileEntityMachineCentrifuge.maxPower) + "HE");

		try {
			int cnt = centrifuge.clientInputCount;
			if(cnt <= 0) {
				text.add(EnumChatFormatting.GRAY + "Slot: " + EnumChatFormatting.RESET + "empty");
			} else {
				text.add(EnumChatFormatting.YELLOW + "Slot: " + EnumChatFormatting.RESET + cnt + " items");
			}
		} catch (Exception e) {
			text.add(EnumChatFormatting.RED + "Slot info unavailable");
		}

		int percent = 0;
		if (TileEntityMachineCentrifuge.processingSpeed > 0) {
			percent = (int) (centrifuge.progress * 100L / (long) TileEntityMachineCentrifuge.processingSpeed);
			if (percent < 0) percent = 0;
			if (percent > 100) percent = 100;
		}
		text.add(EnumChatFormatting.AQUA + "Progress: " + EnumChatFormatting.RESET + percent + "%");

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}
}
