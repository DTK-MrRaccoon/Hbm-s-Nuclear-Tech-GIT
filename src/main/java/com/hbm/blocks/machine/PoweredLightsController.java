package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.SpotlightPowered.TileEntitySpotlightPowered;

import api.hbm.block.IToolable;
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
import net.minecraft.world.IBlockAccess;
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
				player.addChatMessage(new ChatComponentText("Lights: " + controller.lightCount + " | Power Usage: " + controller.totalPowerUsage + " HE/t | Buffer: " + controller.power + "/" + controller.maxPower + " HE | RedstoneDisabled: " + controller.redstoneDisabled));
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
				player.addChatMessage(new ChatComponentText("Scanned for lights! Found: " + controller.lightCount + " lights (saved up to 350)."));
			}
		}
		return true;
	}

	@Override
	public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
		return true;
	}

	public static class TileEntityPoweredLightsController extends TileEntity implements IEnergyReceiverMK2 {

		public static final long maxPower = 50000L;
		public long power;
		public int lightCount;
		public long totalPowerUsage;

		private final List<TileEntitySpotlightPowered> cachedLights = new ArrayList<TileEntitySpotlightPowered>();
		private final List<int[]> savedLightPositions = new ArrayList<int[]>();
		private static final int MAX_SAVED_LIGHTS = 350;

		private boolean savedPositionsDirty = false;
		private boolean processingNotification = false;
		private boolean needsInitialization = true;

		public boolean redstoneDisabled = false;
		private boolean lastRedstonePower = false;

		public void forceRefresh() {
			refreshCachedLights(true);
		}

		@Override
		public void updateEntity() {
			if(!worldObj.isRemote) {
				
				if(needsInitialization) {
					if(!savedLightPositions.isEmpty()) {
						refreshCachedLights(false);
					}
					needsInitialization = false;
				}

				for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
					this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
				}

				boolean currentlyPowered = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
				if(currentlyPowered != lastRedstonePower) {
					lastRedstonePower = currentlyPowered;
					if(currentlyPowered) {
						redstoneDisabled = true;
						disableAllManagedLights();
						markDirty();
					} else {
						redstoneDisabled = false;
						restoreManagedLights();
						markDirty();
					}
				}

				distributePower();

				if(savedPositionsDirty) {
					markDirty();
					savedPositionsDirty = false;
				}
			}
		}

		public void scanForLights() {
			cachedLights.clear();
			savedLightPositions.clear();
			lightCount = 0;
			totalPowerUsage = 0;

			int radius = 48;

			outer:
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
								light.setControllerManaged(true);
								light.setControllerPos(this.xCoord, this.yCoord, this.zCoord);
								light.markDirty();

								cachedLights.add(light);
								savedLightPositions.add(new int[]{x, y, z});
								lightCount++;
								totalPowerUsage += light.powerConsumption;

								if(savedLightPositions.size() >= MAX_SAVED_LIGHTS) {
									break outer;
								}
							}
						}
					}
				}
			}

			savedPositionsDirty = true;
			markDirty();
		}

		public void removeSavedLightPosition(int lx, int ly, int lz) {
			int idx = -1;
			for (int i = 0; i < savedLightPositions.size(); i++) {
				int[] pos = savedLightPositions.get(i);
				if (pos[0] == lx && pos[1] == ly && pos[2] == lz) {
					idx = i;
					break;
				}
			}
			if (idx == -1) return;

			savedLightPositions.remove(idx);
			if (idx < cachedLights.size()) {
				cachedLights.remove(idx);
			}

			lightCount = cachedLights.size();
			recalculateTotalUsage();
			savedPositionsDirty = true;
			markDirty();
		}

		public void onLightStateChanged(int lx, int ly, int lz) {
			if (processingNotification) return;
			processingNotification = true;
			try {
				int idx = -1;
				for(int i = 0; i < savedLightPositions.size(); i++) {
					int[] pos = savedLightPositions.get(i);
					if(pos[0] == lx && pos[1] == ly && pos[2] == lz) {
						idx = i;
						break;
					}
				}

				if(idx == -1) return;

				if(!worldObj.blockExists(lx, ly, lz)) {
					if(idx < cachedLights.size()) cachedLights.set(idx, null);
					return;
				}

				TileEntity te = worldObj.getTileEntity(lx, ly, lz);
				if(te instanceof TileEntitySpotlightPowered) {
					TileEntitySpotlightPowered light = (TileEntitySpotlightPowered) te;
					light.setControllerPos(this.xCoord, this.yCoord, this.zCoord);
					light.setControllerManaged(true);
					light.markDirty();

					if(idx < cachedLights.size()) {
						cachedLights.set(idx, light);
					} else {
						while(cachedLights.size() < idx) cachedLights.add(null);
						cachedLights.add(light);
					}
				} else {
					if(idx < cachedLights.size()) cachedLights.remove(idx);
					savedLightPositions.remove(idx);
					savedPositionsDirty = true;
					lightCount = cachedLights.size();
					recalculateTotalUsage();
					markDirty();
				}
			} finally {
				processingNotification = false;
			}
		}

		private void distributePower() {
			if(redstoneDisabled || cachedLights.isEmpty() || power <= 0) return;

			long powerNeeded = 0;
			for(TileEntitySpotlightPowered light : cachedLights) {
				if(light == null || light.isInvalid()) continue;
				long needed = light.maxPower - light.power;
				if(needed > 0) powerNeeded += needed;
			}

			if(powerNeeded > 0) {
				long powerToDistribute = Math.min(power, powerNeeded);
				for(TileEntitySpotlightPowered light : cachedLights) {
					if(light == null || light.isInvalid()) continue;
					long needed = light.maxPower - light.power;
					if(needed > 0) {
						long transfer = Math.min(needed, powerToDistribute);
						light.power += transfer;
						this.power -= transfer;
						powerToDistribute -= transfer;
						light.markDirty();
						if(powerToDistribute <= 0) break;
					}
				}
			}
		}

		private void refreshCachedLights(boolean forceWrite) {
			List<TileEntitySpotlightPowered> newCached = new ArrayList<TileEntitySpotlightPowered>();
			
			for(int[] pos : savedLightPositions) {
				int x = pos[0], y = pos[1], z = pos[2];

				if(!worldObj.blockExists(x, y, z)) {
					newCached.add(null);
					continue;
				}

				TileEntity te = worldObj.getTileEntity(x, y, z);
				if(te instanceof TileEntitySpotlightPowered) {
					newCached.add((TileEntitySpotlightPowered) te);
				} else {
					newCached.add(null);
				}
			}

			cachedLights.clear();
			cachedLights.addAll(newCached);
			
			lightCount = 0;
			totalPowerUsage = 0;
			for(TileEntitySpotlightPowered light : cachedLights) {
				if(light != null) {
					lightCount++;
					totalPowerUsage += light.powerConsumption;
				}
			}
			if(forceWrite) markDirty();
		}

		private void recalculateTotalUsage() {
			totalPowerUsage = 0;
			for(TileEntitySpotlightPowered light : cachedLights) {
				if(light != null) totalPowerUsage += light.powerConsumption;
			}
		}

		private void disableAllManagedLights() {
			for(int[] pos : savedLightPositions) {
				int lx = pos[0], ly = pos[1], lz = pos[2];
				if(!worldObj.blockExists(lx, ly, lz)) continue;
				Block b = worldObj.getBlock(lx, ly, lz);
				if(b instanceof SpotlightPowered) {
					int meta = worldObj.getBlockMetadata(lx, ly, lz);
					ForgeDirection dir = ForgeDirection.getOrientation(meta >> 1);
					Spotlight.unpropagateBeam(worldObj, lx, ly, lz, dir);
					SpotlightPowered sp = (SpotlightPowered) b;
					NBTTagCompound data = new NBTTagCompound();
					TileEntity te = worldObj.getTileEntity(lx, ly, lz);
					if(te instanceof TileEntitySpotlightPowered) ((TileEntitySpotlightPowered)te).writeToNBT(data);
					
					worldObj.setBlock(lx, ly, lz, sp.getOff(), meta, 2);
					TileEntity newTE = worldObj.getTileEntity(lx, ly, lz);
					if(newTE instanceof TileEntitySpotlightPowered) ((TileEntitySpotlightPowered)newTE).readFromNBT(data);
				}
			}
		}

		private void restoreManagedLights() {
			for(int[] pos : savedLightPositions) {
				int lx = pos[0], ly = pos[1], lz = pos[2];
				if(!worldObj.blockExists(lx, ly, lz)) continue;
				Block b = worldObj.getBlock(lx, ly, lz);
				if(b instanceof SpotlightPowered) {
					int meta = worldObj.getBlockMetadata(lx, ly, lz);
					TileEntity te = worldObj.getTileEntity(lx, ly, lz);
					if(te instanceof TileEntitySpotlightPowered) {
						TileEntitySpotlightPowered light = (TileEntitySpotlightPowered) te;
						SpotlightPowered sp = (SpotlightPowered) b;
						if(!sp.isOn && light.power >= light.powerConsumption) {
							NBTTagCompound data = new NBTTagCompound();
							light.writeToNBT(data);
							Block on = sp.getOn();
							worldObj.setBlock(lx, ly, lz, on, meta, 2);
							TileEntity newTE = worldObj.getTileEntity(lx, ly, lz);
							if(newTE instanceof TileEntitySpotlightPowered) {
								((TileEntitySpotlightPowered)newTE).readFromNBT(data);
								ForgeDirection dir = ForgeDirection.getOrientation(meta >> 1);
								Spotlight.propagateBeam(worldObj, lx, ly, lz, dir, ((SpotlightPowered)on).beamLength);
							}
						}
					}
				}
			}
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt) {
			super.readFromNBT(nbt);
			this.power = nbt.getLong("power");
			this.redstoneDisabled = nbt.getBoolean("redstoneDisabled");

			savedLightPositions.clear();
			NBTTagList lightList = nbt.getTagList("lightPositions", 10);
			for(int i = 0; i < lightList.tagCount(); i++) {
				NBTTagCompound posTag = lightList.getCompoundTagAt(i);
				savedLightPositions.add(new int[]{posTag.getInteger("x"), posTag.getInteger("y"), posTag.getInteger("z")});
			}
			
			this.needsInitialization = true;
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt) {
			super.writeToNBT(nbt);
			nbt.setLong("power", power);
			nbt.setBoolean("redstoneDisabled", redstoneDisabled);

			NBTTagList lightList = new NBTTagList();
			for(int i = 0; i < savedLightPositions.size() && i < MAX_SAVED_LIGHTS; i++) {
				int[] pos = savedLightPositions.get(i);
				NBTTagCompound posTag = new NBTTagCompound();
				posTag.setInteger("x", pos[0]);
				posTag.setInteger("y", pos[1]);
				posTag.setInteger("z", pos[2]);
				lightList.appendTag(posTag);
			}
			nbt.setTag("lightPositions", lightList);
		}

		@Override public Packet getDescriptionPacket() { NBTTagCompound nbt = new NBTTagCompound(); this.writeToNBT(nbt); return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, nbt); }
		@Override public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) { this.readFromNBT(pkt.func_148857_g()); }
		@Override public long getPower() { return power; }
		@Override public void setPower(long power) { this.power = power; }
		@Override public long getMaxPower() { return maxPower; }
		
		private boolean isLoaded = true;
		@Override public boolean isLoaded() { return isLoaded; }
		@Override public void onChunkUnload() { this.isLoaded = false; }
		@Override @SideOnly(Side.CLIENT) public double getMaxRenderDistanceSquared() { return 65536.0D; }
		@Override public AxisAlignedBB getRenderBoundingBox() { return AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord - 1, zCoord - 1, xCoord + 2, yCoord + 2, zCoord + 2); }
	}
}