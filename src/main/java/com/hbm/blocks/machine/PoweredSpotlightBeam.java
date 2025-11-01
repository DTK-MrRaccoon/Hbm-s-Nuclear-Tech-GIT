package com.hbm.blocks.machine;

import java.util.List;

import com.hbm.tileentity.TileEntityData;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class PoweredSpotlightBeam extends BlockBeamBase {

	public PoweredSpotlightBeam() {
		super();
		// Don't set light level in constructor - we'll handle it dynamically
		setLightLevel(0.0F);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		updateLightLevel(world, x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock) {
		if (world.isRemote) return;
		if (neighborBlock instanceof SpotlightBeam || neighborBlock instanceof PoweredSpotlightBeam) return;

		// Check if any source lights are still powered
		boolean hasActivePowerSource = false;
		for(ForgeDirection dir : getDirections(world, x, y, z)) {
			if(isSourcePowered(world, x, y, z, dir)) {
				hasActivePowerSource = true;
				break;
			}
		}

		if(!hasActivePowerSource) {
			// Remove beam if no powered sources
			world.setBlockToAir(x, y, z);
			return;
		}

		// Standard beam propagation logic
		for(ForgeDirection dir : getDirections(world, x, y, z)) {
			Spotlight.backPropagate(world, x, y, z, dir, world.getBlockMetadata(x, y, z));
		}
		
		updateLightLevel(world, x, y, z);
	}

	// If a block is placed onto the beam, handle the new cutoff
	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
		if (!world.isRemote) {
			for (ForgeDirection dir : getDirections(world, x, y, z)) {
				Spotlight.unpropagateBeam(world, x, y, z, dir);
			}
		}
		super.breakBlock(world, x, y, z, block, metadata);
	}

	private void updateLightLevel(World world, int x, int y, int z) {
		// Check if any source lights are powered
		boolean hasActivePowerSource = false;
		for(ForgeDirection dir : getDirections(world, x, y, z)) {
			if(isSourcePowered(world, x, y, z, dir)) {
				hasActivePowerSource = true;
				break;
			}
		}

		// Update light level based on power state
		if(hasActivePowerSource) {
			world.setLightValue(net.minecraft.world.EnumSkyBlock.Block, x, y, z, 15);
		} else {
			world.setLightValue(net.minecraft.world.EnumSkyBlock.Block, x, y, z, 0);
		}
		
		// Force light update
		world.func_147451_t(x, y, z);
	}

	private boolean isSourcePowered(World world, int x, int y, int z, ForgeDirection dir) {
		// Trace back to find the source light
		int sourceX = x;
		int sourceY = y;
		int sourceZ = z;
		
		// Follow the beam backwards to find the source
		for(int i = 0; i < 64; i++) { // Max search distance
			sourceX -= dir.offsetX;
			sourceY -= dir.offsetY;
			sourceZ -= dir.offsetZ;
			
			Block block = world.getBlock(sourceX, sourceY, sourceZ);
			if(block instanceof SpotlightPowered) {
				SpotlightPowered spotlight = (SpotlightPowered) block;
				if(!spotlight.isOn) return false;
				
				TileEntity te = world.getTileEntity(sourceX, sourceY, sourceZ);
				if(te instanceof SpotlightPowered.TileEntitySpotlightPowered) {
					SpotlightPowered.TileEntitySpotlightPowered spotlightTE = (SpotlightPowered.TileEntitySpotlightPowered) te;
					return spotlightTE.power >= spotlightTE.powerConsumption;
				}
				return false;
			} else if(!(block instanceof SpotlightBeam) && !(block instanceof PoweredSpotlightBeam)) {
				// Hit a solid block, source not found
				return false;
			}
		}
		
		return false;
	}

	public static List<ForgeDirection> getDirections(World world, int x, int y, int z) {
		return SpotlightBeam.getDirections(world, x, y, z);
	}

	public static int setDirection(World world, int x, int y, int z, ForgeDirection dir, boolean state) {
		int result = SpotlightBeam.setDirection(world, x, y, z, dir, state);
		
		// Update light level when directions change
		if(world.getBlock(x, y, z) instanceof PoweredSpotlightBeam) {
			((PoweredSpotlightBeam) world.getBlock(x, y, z)).updateLightLevel(world, x, y, z);
		}
		
		return result;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityData();
	}
}