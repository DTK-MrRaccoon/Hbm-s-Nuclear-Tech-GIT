package com.hbm.blocks.machine;

import java.util.Random;

import com.hbm.blocks.BlockEnums.LightType;
import com.hbm.blocks.ModBlocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class Spotlight extends SpotlightBase {

	// I'd be extending the ReinforcedLamp class if it wasn't for the inverted behaviour of these specific lights
	// I want these blocks to be eminently useful, so removing the need for redstone by default is desired,
	// these act more like redstone torches, in that applying a signal turns them off

	public Spotlight(Material mat, int beamLength, LightType type, boolean isOn) {
		super(mat, beamLength, type, isOn);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		if(world.isRemote) return;
		if(updatePower(world, x, y, z)) return;
		updateBeam(world, x, y, z);
	}

	private boolean updatePower(World world, int x, int y, int z) {
		if(isBroken(world.getBlockMetadata(x, y, z))) return false;

		boolean isPowered = world.isBlockIndirectlyGettingPowered(x, y, z);
		if(isOn && isPowered) {
			world.scheduleBlockUpdate(x, y, z, this, 4);
			return true;
		} else if(!isOn && !isPowered) {
			world.setBlock(x, y, z, getOn(), world.getBlockMetadata(x, y, z), 2);
			return true;
		}
		return false;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
		ForgeDirection dir = getDirection(metadata);
		super.breakBlock(world, x, y, z, block, metadata);
		if(world.isRemote) return;
		unpropagateBeam(world, x, y, z, dir);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random p_149674_5_) {
		if(world.isRemote) return;
		if(isOn && world.isBlockIndirectlyGettingPowered(x, y, z)) {
			world.setBlock(x, y, z, getOff(), world.getBlockMetadata(x, y, z), 2);
		}
	}

	// Repropagate the beam if we've become unblocked
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock) {
		if(world.isRemote) return;
		if(neighborBlock instanceof SpotlightBeam) return;
		if(neighborBlock == Blocks.air) return;

		ForgeDirection dir = getDirection(world, x, y, z);
		if(!canPlace(world, x, y, z, dir)) {
			dropBlockAsItem(world, x, y, z, 0, 0);
			world.setBlockToAir(x, y, z);
			return;
		}
		if(updatePower(world, x, y, z)) return;
		updateBeam(world, x, y, z);
	}

	private void updateBeam(World world, int x, int y, int z) {
		if(!isOn) return;
		ForgeDirection dir = getDirection(world, x, y, z);
		propagateBeam(world, x, y, z, dir, beamLength);
	}

	// Replace bulbs on broken lights with a click
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		int meta = world.getBlockMetadata(x, y, z);
		if(!isBroken(meta)) return false;
		repair(world, x, y, z);
		return true;
	}

	private void repair(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if(!isBroken(meta)) return;
		world.setBlock(x, y, z, getOn(), meta - 1, 2);
		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			int ox = x + dir.offsetX;
			int oy = y + dir.offsetY;
			int oz = z + dir.offsetZ;
			Block block = world.getBlock(ox, oy, oz);
			if(block == this) repair(world, ox, oy, oz);
		}
	}

	public boolean isBroken(int metadata) {
		return (metadata & 1) == 1;
	}

	@Override
	public Item getItemDropped(int i, Random r, int j) {
		return Item.getItemFromBlock(getOn());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return Item.getItemFromBlock(getOn());
	}

	@Override
	protected ItemStack createStackedBlock(int e) {
		return new ItemStack(getOn());
	}

	@Override
	protected Block getOff() {
		if(this == ModBlocks.spotlight_incandescent) return ModBlocks.spotlight_incandescent_off;
		if(this == ModBlocks.spotlight_fluoro) return ModBlocks.spotlight_fluoro_off;
		if(this == ModBlocks.spotlight_halogen) return ModBlocks.spotlight_halogen_off;
		return this;
	}

	@Override
	protected Block getOn() {
		if(this == ModBlocks.spotlight_incandescent_off) return ModBlocks.spotlight_incandescent;
		if(this == ModBlocks.spotlight_fluoro_off) return ModBlocks.spotlight_fluoro;
		if(this == ModBlocks.spotlight_halogen_off) return ModBlocks.spotlight_halogen;
		return this;
	}

	@Override
	public Block transformBlock(Block block) {
		if(!disableOnGeneration) return block;
		if(block == ModBlocks.spotlight_incandescent) return ModBlocks.spotlight_incandescent_off;
		if(block == ModBlocks.spotlight_fluoro) return ModBlocks.spotlight_fluoro_off;
		if(block == ModBlocks.spotlight_halogen) return ModBlocks.spotlight_halogen_off;
		return block;
	}
}