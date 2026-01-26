package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IPersistentNBT;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.storage.TileEntityMachineRadTank;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineRadTank extends BlockDummyable implements IPersistentInfoProvider {

	public MachineRadTank(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityMachineRadTank();
		if(meta >= 6) return new TileEntityProxyCombo(false, false, true);
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {2, 0, 1, 1, 1, 1};
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);
		int cx = x + dir.offsetX * o;
		int cz = z + dir.offsetZ * o;

		this.makeExtra(world, cx, y + 1, cz - 1);
		this.makeExtra(world, cx, y + 1, cz + 1);
		this.makeExtra(world, cx - 1, y + 1, cz);
		this.makeExtra(world, cx + 1, y + 1, cz);
		this.makeExtra(world, cx, y + 2, cz);
		this.makeExtra(world, cx, y, cz);
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) return true;
		int[] pos = this.findCore(world, x, y, z);
		if(pos == null) return false;
		
		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		if(!(te instanceof TileEntityMachineRadTank)) return false;
		
		TileEntityMachineRadTank tank = (TileEntityMachineRadTank) te;
		ItemStack stack = player.getHeldItem();

		if(player.isSneaking() && stack != null && stack.getItem() instanceof IItemFluidIdentifier) {
			FluidType type = ((IItemFluidIdentifier)stack.getItem()).getType(world, pos[0], pos[1], pos[2], stack);
			if(type != null && (type == Fluids.NONE || tank.tank.getFill() == 0)) {
				tank.tank.setTankType(type);
				tank.markDirty();
				player.addChatComponentMessage(new ChatComponentText("Changed type to ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)).appendSibling(new ChatComponentTranslation(type.getConditionalName())).appendSibling(new ChatComponentText("!")));
			}
			return true;
		}

		FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, pos[0], pos[1], pos[2]);
		return true;
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		return IPersistentNBT.getDrops(world, x, y, z, this);
	}

	@Override
	public void addInformation(ItemStack stack, NBTTagCompound persistentTag, EntityPlayer player, List list, boolean ext) {
		FluidTank tank = new FluidTank(Fluids.NONE, 0);
		tank.readFromNBT(persistentTag, "tank");
		if(tank.getFill() > 0) {
			list.add(EnumChatFormatting.YELLOW + "" + tank.getFill() + "/" + tank.getMaxFill() + "mB " + tank.getTankType().getLocalizedName());
		}
	}
}