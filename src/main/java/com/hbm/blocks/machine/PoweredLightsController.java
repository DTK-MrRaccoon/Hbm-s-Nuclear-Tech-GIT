package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.SpotlightPowered.TileEntitySpotlightPowered;
import com.hbm.lib.Library;

import api.hbm.block.IToolable;
import api.hbm.block.IToolable.ToolType;
import api.hbm.energymk2.IEnergyReceiverMK2;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class PoweredLightsController extends BlockContainer implements IToolable {

	public PoweredLightsController(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityPoweredLightsController();
	}

	@Override
	public boolean isOpaqueCube() {
		return true;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return true;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		if(!world.isRemote) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof TileEntityPoweredLightsController) {
				TileEntityPoweredLightsController controller = (TileEntityPoweredLightsController) te;
				player.addChatMessage(new ChatComponentText("Lights: " + controller.lightCount + " | Power Usage: " + controller.totalPowerUsage + " HE/t | Buffer: " + controller.power + "/" + controller.maxPower + " HE"));
			}
		}
		return true;
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
		if(tool != ToolType.SCREWDRIVER) return false;
		
		if(!world.isRemote) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof TileEntityPoweredLightsController) {
				TileEntityPoweredLightsController controller = (TileEntityPoweredLightsController) te;
				controller.scanForLights();
				player.addChatMessage(new ChatComponentText("Scanned for lights! Found: " + controller.lightCount + " lights"));
			}
		}
		return true;
	}

	public static class TileEntityPoweredLightsController extends TileEntity implements IEnergyReceiverMK2 {

		public static final long maxPower = 10000;
		public long power;
		public int lightCount;
		public long totalPowerUsage;
		private List<TileEntitySpotlightPowered> cachedLights = new ArrayList<TileEntitySpotlightPowered>();
		private List<int[]> savedLightPositions = new ArrayList<int[]>(); // Persistent light positions
		public int refreshTimer = 0;
		
		public void forceRefresh() {
			refreshCachedLights();
		}

		@Override
		public void updateEntity() {
			if(!worldObj.isRemote) {
				for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
					this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
				}

				// Load cached lights from saved positions if cache is empty
				if(cachedLights.isEmpty() && !savedLightPositions.isEmpty()) {
					loadLightsFromSavedPositions();
				}

				// Refresh cache every 5 seconds to handle block state changes
				refreshTimer++;
				if(refreshTimer >= 100) { // 100 ticks = 5 seconds
					refreshTimer = 0;
					if(!savedLightPositions.isEmpty()) {
						refreshCachedLights();
					}
				}

				distributePower();
			}
		}

		public void scanForLights() {
			cachedLights.clear();
			savedLightPositions.clear();
			lightCount = 0;
			totalPowerUsage = 0;

			int radius = 48;
			
			for(int dx = -radius; dx <= radius; dx++) {
				for(int dy = -radius; dy <= radius; dy++) {
					for(int dz = -radius; dz <= radius; dz++) {
						int x = xCoord + dx;
						int y = yCoord + dy;
						int z = zCoord + dz;
						
						if(!worldObj.blockExists(x, y, z)) continue;
						
						Block block = worldObj.getBlock(x, y, z);
						if(block == ModBlocks.spotlight_incandescent_powered || 
						   block == ModBlocks.spotlight_incandescent_powered_off ||
						   block == ModBlocks.spotlight_fluoro_powered || 
						   block == ModBlocks.spotlight_fluoro_powered_off ||
						   block == ModBlocks.spotlight_halogen_powered ||
						   block == ModBlocks.spotlight_halogen_powered_off) {
							
							TileEntity te = worldObj.getTileEntity(x, y, z);
							if(te instanceof TileEntitySpotlightPowered) {
								TileEntitySpotlightPowered light = (TileEntitySpotlightPowered) te;
								cachedLights.add(light);
								savedLightPositions.add(new int[]{x, y, z});
								lightCount++;
								totalPowerUsage += light.powerConsumption;
							}
						}
					}
				}
			}
			
			// Save the scan results
			markDirty();
		}

		private void loadLightsFromSavedPositions() {
			refreshCachedLights();
		}

		private void distributePower() {
			if(cachedLights.isEmpty()) return;
			
			// Refresh the cached lights to handle block state changes
			refreshCachedLights();
			
			long powerNeeded = 0;
			for(TileEntitySpotlightPowered light : cachedLights) {
				if(light.isInvalid()) continue;
				long needed = light.maxPower - light.power;
				if(needed > 0) powerNeeded += needed;
			}

			if(powerNeeded > 0 && power > 0) {
				long powerToDistribute = Math.min(power, powerNeeded);
				
				for(TileEntitySpotlightPowered light : cachedLights) {
					if(light.isInvalid()) continue;
					long needed = light.maxPower - light.power;
					if(needed > 0) {
						long transfer = Math.min(needed, powerToDistribute);
						light.power += transfer;
						power -= transfer;
						powerToDistribute -= transfer;
						light.markDirty();
						
						if(powerToDistribute <= 0) break;
					}
				}
			}
		}

		private void refreshCachedLights() {
			List<TileEntitySpotlightPowered> validLights = new ArrayList<TileEntitySpotlightPowered>();
			List<int[]> validPositions = new ArrayList<int[]>();
			
			for(int i = 0; i < savedLightPositions.size(); i++) {
				int[] pos = savedLightPositions.get(i);
				int x = pos[0], y = pos[1], z = pos[2];
				
				if(!worldObj.blockExists(x, y, z)) continue;
				
				Block block = worldObj.getBlock(x, y, z);
				if(block == ModBlocks.spotlight_incandescent_powered || 
				   block == ModBlocks.spotlight_incandescent_powered_off ||
				   block == ModBlocks.spotlight_fluoro_powered || 
				   block == ModBlocks.spotlight_fluoro_powered_off ||
				   block == ModBlocks.spotlight_halogen_powered ||
				   block == ModBlocks.spotlight_halogen_powered_off) {
					
					TileEntity te = worldObj.getTileEntity(x, y, z);
					if(te instanceof TileEntitySpotlightPowered) {
						TileEntitySpotlightPowered light = (TileEntitySpotlightPowered) te;
						validLights.add(light);
						validPositions.add(pos);
					}
				}
			}
			
			cachedLights = validLights;
			savedLightPositions = validPositions;
			lightCount = validLights.size();
			
			// Recalculate total power usage
			totalPowerUsage = 0;
			for(TileEntitySpotlightPowered light : cachedLights) {
				totalPowerUsage += light.powerConsumption;
			}
		}

		@Override
		public Packet getDescriptionPacket() {
			NBTTagCompound nbt = new NBTTagCompound();
			this.writeToNBT(nbt);
			return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, nbt);
		}

		@Override
		public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
			this.readFromNBT(pkt.func_148857_g());
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt) {
			super.readFromNBT(nbt);
			this.power = nbt.getLong("power");
			
			// Load saved light positions
			savedLightPositions.clear();
			NBTTagList lightList = nbt.getTagList("lightPositions", 10);
			for(int i = 0; i < lightList.tagCount(); i++) {
				NBTTagCompound posTag = lightList.getCompoundTagAt(i);
				int[] pos = new int[]{
					posTag.getInteger("x"),
					posTag.getInteger("y"),
					posTag.getInteger("z")
				};
				savedLightPositions.add(pos);
			}
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt) {
			super.writeToNBT(nbt);
			nbt.setLong("power", power);
			
			// Save light positions
			NBTTagList lightList = new NBTTagList();
			for(int[] pos : savedLightPositions) {
				NBTTagCompound posTag = new NBTTagCompound();
				posTag.setInteger("x", pos[0]);
				posTag.setInteger("y", pos[1]);
				posTag.setInteger("z", pos[2]);
				lightList.appendTag(posTag);
			}
			nbt.setTag("lightPositions", lightList);
		}

		@Override
		public long getPower() {
			return power;
		}

		@Override
		public void setPower(long power) {
			this.power = power;
		}

		@Override
		public long getMaxPower() {
			return maxPower;
		}

		private boolean isLoaded = true;

		@Override
		public boolean isLoaded() {
			return isLoaded;
		}

		@Override
		public void onChunkUnload() {
			this.isLoaded = false;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public double getMaxRenderDistanceSquared() {
			return 65536.0D;
		}

		@Override
		public AxisAlignedBB getRenderBoundingBox() {
			return AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord - 1, zCoord - 1, xCoord + 2, yCoord + 2, zCoord + 2);
		}
	}
}
