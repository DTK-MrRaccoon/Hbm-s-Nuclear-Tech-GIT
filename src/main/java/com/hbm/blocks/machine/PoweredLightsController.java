package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.SpotlightPowered.TileEntitySpotlightPowered;

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
				player.addChatMessage(new ChatComponentText("Scanned for lights! Found: " + controller.lightCount + " lights (saved up to 350)."));
			}
		}
		return true;
	}

	public static class TileEntityPoweredLightsController extends TileEntity implements IEnergyReceiverMK2 {

		// PERF: Increased buffer
		public static final long maxPower = 500000L;
		public long power;
		public int lightCount;
		public long totalPowerUsage;

		// In-memory cache of loaded TileEntitySpotlightPowered references.
		private final List<TileEntitySpotlightPowered> cachedLights = new ArrayList<TileEntitySpotlightPowered>();

		// Persistent saved positions (x,y,z). This is saved to NBT and NOT regenerated from scans automatically.
		private final List<int[]> savedLightPositions = new ArrayList<int[]>();

		private static final int MAX_SAVED_LIGHTS = 350; // PERF: cap

		// PERF: track whether saved positions were changed and need a write
		private boolean savedPositionsDirty = false;

		private long lastCacheBuildTime = -1L;

		// === NEW: reentrancy guard to avoid infinite recursion between TE <-> controller ===
		private boolean processingNotification = false;

		public void forceRefresh() {
			// PERF: Rebuild cache from saved positions (cheap: limited to saved list)
			refreshCachedLights(true);
		}

		@Override
		public void updateEntity() {
			if(!worldObj.isRemote) {
				// PERF: Keep neighbor subscription minimal (cheap)
				for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
					this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
				}

				// PERF: Build / validate cache only once after load or when explicitly requested.
				if(cachedLights.isEmpty() && !savedLightPositions.isEmpty()) {
					refreshCachedLights(false); // don't force an NBT write
				}

				// PERF: power distribution uses in-memory cache only (no refresh each tick)
				distributePower();

				// PERF: flush saved positions to disk only when changed
				if(savedPositionsDirty) {
					markDirty();
					savedPositionsDirty = false;
				}
			}
		}

		/**
		 * Manual scan (triggered by screwdriver). This is expensive by nature and is intentionally manual.
		 * It will set each found light's controller coordinates and mark them controller-managed.
		 */
		public void scanForLights() {
			// PERF: manual expensive operation only
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
								// PERF: mark light as controller-managed & store controller coords on the light TE
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

			// PERF: mark saved positions dirty and persist
			savedPositionsDirty = true;
			markDirty();
		}

		/**
		 * Called by a light when it changes state or when it unloads/removes itself.
		 * PERF: This updates only the single entry corresponding to the given light position.
		 * This avoids scanning the whole world and avoids heavy operations on toggles.
		 */
		public void onLightStateChanged(int lx, int ly, int lz) {
			// === Reentrancy guard: ignore re-entrant notifications while processing one ===
			if (processingNotification) {
				// already handling a notification; drop this one to avoid recursive loops
				return;
			}
			processingNotification = true;
			try {
				// PERF: find index in saved positions (list <= 350 so linear search is fine)
				int idx = -1;
				for(int i = 0; i < savedLightPositions.size(); i++) {
					int[] pos = savedLightPositions.get(i);
					if(pos[0] == lx && pos[1] == ly && pos[2] == lz) {
						idx = i;
						break;
					}
				}

				// If not found, this light isn't managed by us — nothing to do.
				if(idx == -1) return;

				// Try to get fresh TileEntity reference, may be null if chunk unloaded or block removed
				if(!worldObj.blockExists(lx, ly, lz)) {
					// keep the saved position for future (we remember it) but remove runtime reference
					if(idx < cachedLights.size()) {
						cachedLights.set(idx, null);
					}
					return;
				}

				TileEntity te = worldObj.getTileEntity(lx, ly, lz);
				if(te instanceof TileEntitySpotlightPowered) {
					TileEntitySpotlightPowered light = (TileEntitySpotlightPowered) te;

					// Ensure controller coords are set on the light
					light.setControllerPos(this.xCoord, this.yCoord, this.zCoord);
					light.setControllerManaged(true);
					light.markDirty();

					// Update cachedLights (keep index in sync)
					if(idx < cachedLights.size()) {
						cachedLights.set(idx, light);
					} else {
						// Ensure lists are in sync; add nulls if needed
						while(cachedLights.size() < idx) cachedLights.add(null);
						cachedLights.add(light);
					}
				} else {
					// light removed — prune saved position and cached reference
					if(idx < cachedLights.size()) {
						cachedLights.remove(idx);
					}
					savedLightPositions.remove(idx);
					savedPositionsDirty = true;
					// update counts
					lightCount = cachedLights.size();
					// recalc totalPowerUsage lazily
					recalculateTotalUsage();
					markDirty();
				}
			} finally {
				processingNotification = false;
			}
		}

		/**
		 * Distribute power among cached lights. PERF: does not revalidate saved positions.
		 */
		private void distributePower() {
			// PERF: fast path
			if(cachedLights.isEmpty()) return;

			long powerNeeded = 0;
			for(TileEntitySpotlightPowered light : cachedLights) {
				if(light == null || light.isInvalid()) continue;
				long needed = light.maxPower - light.power;
				if(needed > 0) powerNeeded += needed;
			}

			if(powerNeeded > 0 && power > 0) {
				long powerToDistribute = Math.min(power, powerNeeded);

				// PERF: do single-pass fill (no heavy locking)
				for(TileEntitySpotlightPowered light : cachedLights) {
					if(light == null || light.isInvalid()) continue;
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

		/**
		 * Rebuild cachedLights and prune savedLightPositions to valid ones.
		 * If forceWrite is true we will markDirty() if saved list changed.
		 */
		private void refreshCachedLights(boolean forceWrite) {
			boolean changed = false;

			List<TileEntitySpotlightPowered> newCached = new ArrayList<TileEntitySpotlightPowered>();
			List<int[]> newPositions = new ArrayList<int[]>();

			for(int i = 0; i < savedLightPositions.size(); i++) {
				int[] pos = savedLightPositions.get(i);
				int x = pos[0], y = pos[1], z = pos[2];

				if(!worldObj.blockExists(x, y, z)) {
					// keep the saved position (remember it) but no tile entity ref
					newPositions.add(pos);
					newCached.add(null);
					continue;
				}

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
						newCached.add(light);
						newPositions.add(pos);
						continue;
					}
				}

				// block removed: drop saved position
				changed = true;
			}

			// cap to MAX_SAVED_LIGHTS
			if(newPositions.size() > MAX_SAVED_LIGHTS) {
				newPositions = newPositions.subList(0, MAX_SAVED_LIGHTS);
				newCached = newCached.subList(0, MAX_SAVED_LIGHTS);
				changed = true;
			}

			// replace
			cachedLights.clear();
			cachedLights.addAll(newCached);

			// if position list changed, mark dirty to persist removal
			if(changed) {
				savedLightPositions.clear();
				savedLightPositions.addAll(newPositions);
				savedPositionsDirty = true;
				if(forceWrite) markDirty();
			}

			lightCount = 0;
			totalPowerUsage = 0;
			for(TileEntitySpotlightPowered light : cachedLights) {
				if(light == null) continue;
				lightCount++;
				totalPowerUsage += light.powerConsumption;
			}
			lastCacheBuildTime = worldObj.getTotalWorldTime();
		}

		private void recalculateTotalUsage() {
			totalPowerUsage = 0;
			for(TileEntitySpotlightPowered light : cachedLights) {
				if(light == null) continue;
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

			// Rebuild cache references (cheap, bounded)
			if(this.worldObj != null && !this.worldObj.isRemote) {
				refreshCachedLights(false);
			}
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt) {
			super.writeToNBT(nbt);
			nbt.setLong("power", power);
			
			// Save light positions (cap to MAX_SAVED_LIGHTS)
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
