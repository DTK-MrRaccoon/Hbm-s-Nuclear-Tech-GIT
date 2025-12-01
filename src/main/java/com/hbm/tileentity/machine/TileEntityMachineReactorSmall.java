package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.MobConfig;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.container.ContainerMachineReactorSmall;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIMachineReactorSmall;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.inventory.recipes.BreederRecipes.BreederRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.AuxButtonPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.handler.radiation.ChunkRadiationManager;

import api.hbm.fluid.IFluidStandardTransceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class TileEntityMachineReactorSmall extends TileEntityMachineBase implements IFluidStandardTransceiver, IGUIProvider, IControlReceiver {

	public int hullHeat;
	public final int maxHullHeat = 100000;
	public int coreHeat;
	public final int maxCoreHeat = 50000;
	public int rods;
	public final int rodsMax = 100;
	public boolean retracting = true;
	public int age = 0;
	public FluidTank[] tanks;

	// Fuel rod data stored in reactor (not in items)
	public int[] rodFlux = new int[12];        // Current flux for each rod
	public int[] rodHeat = new int[12];        // Heat generated per tick
	public int[] rodDuration = new int[12];    // Remaining duration
	public int[] rodMaxDuration = new int[12]; // Max duration for this rod
	public boolean[] rodLocked = new boolean[12]; // Whether rod is locked (active)
	public ItemStack[] rodOutput = new ItemStack[12]; // What the rod becomes when depleted

	public TileEntityMachineReactorSmall() {
		super(16);
		tanks = new FluidTank[3];
		tanks[0] = new FluidTank(Fluids.WATER, 32000);
		tanks[1] = new FluidTank(Fluids.COOLANT, 16000);
		tanks[2] = new FluidTank(Fluids.STEAM, 128000);
	}

	@Override
	public String getName() {
		return "container.reactorSmall";
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemStack) {
		// Fuel rod slots (0-11)
		if(i >= 0 && i <= 11) {
			// Can only insert if not locked
			if(rodLocked[i])
				return false;
			// Check if it's a breeding rod
			if(itemStack.getItem() instanceof ItemBreedingRod)
				return true;
		}
		// Water input
		if(i == 12)
			return FluidContainerRegistry.getFluidContent(itemStack, tanks[0].getTankType()) > 0;
		// Coolant input
		if(i == 14)
			return FluidContainerRegistry.getFluidContent(itemStack, tanks[1].getTankType()) > 0;
		return false;
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		if(slots[i] != null) {
			// Don't allow removal of locked rods or partially used rods
			if(i >= 0 && i <= 11) {
				if(rodLocked[i]) {
					return null; // Rod is currently active
				}
				// Check if rod has been used (duration is less than max)
				if(rodMaxDuration[i] > 0 && rodDuration[i] < rodMaxDuration[i] && rodDuration[i] > 0) {
					return null; // Rod is partially used
				}
			}
				
			if(slots[i].stackSize <= j) {
				ItemStack itemStack = slots[i];
				slots[i] = null;
				return itemStack;
			}
			ItemStack itemStack1 = slots[i].splitStack(j);
			if(slots[i].stackSize == 0) {
				slots[i] = null;
			}

			return itemStack1;
		}
		return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		coreHeat = nbt.getInteger("heat");
		hullHeat = nbt.getInteger("hullHeat");
		rods = nbt.getInteger("rods");
		retracting = nbt.getBoolean("ret");
		
		// Load rod data
		for(int i = 0; i < 12; i++) {
			rodFlux[i] = nbt.getInteger("rodFlux" + i);
			rodHeat[i] = nbt.getInteger("rodHeat" + i);
			rodDuration[i] = nbt.getInteger("rodDuration" + i);
			rodMaxDuration[i] = nbt.getInteger("rodMaxDuration" + i);
			rodLocked[i] = nbt.getBoolean("rodLocked" + i);
			if(nbt.hasKey("rodOutput" + i)) {
				rodOutput[i] = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("rodOutput" + i));
			}
		}
		
		tanks[0].readFromNBT(nbt, "water");
		tanks[1].readFromNBT(nbt, "coolant");
		tanks[2].readFromNBT(nbt, "steam");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("heat", coreHeat);
		nbt.setInteger("hullHeat", hullHeat);
		nbt.setInteger("rods", rods);
		nbt.setBoolean("ret", retracting);
		
		// Save rod data
		for(int i = 0; i < 12; i++) {
			nbt.setInteger("rodFlux" + i, rodFlux[i]);
			nbt.setInteger("rodHeat" + i, rodHeat[i]);
			nbt.setInteger("rodDuration" + i, rodDuration[i]);
			nbt.setInteger("rodMaxDuration" + i, rodMaxDuration[i]);
			nbt.setBoolean("rodLocked" + i, rodLocked[i]);
			if(rodOutput[i] != null) {
				NBTTagCompound outputTag = new NBTTagCompound();
				rodOutput[i].writeToNBT(outputTag);
				nbt.setTag("rodOutput" + i, outputTag);
			}
		}
		
		tanks[0].writeToNBT(nbt, "water");
		tanks[1].writeToNBT(nbt, "coolant");
		tanks[2].writeToNBT(nbt, "steam");
	}

	public int getCoreHeatScaled(int i) {
		return (coreHeat * i) / maxCoreHeat;
	}

	public int getHullHeatScaled(int i) {
		return (hullHeat * i) / maxHullHeat;
	}

	public int getSteamScaled(int i) {
		return (tanks[2].getFill() * i) / tanks[2].getMaxFill();
	}

	public boolean hasCoreHeat() {
		return coreHeat > 0;
	}

	public boolean hasHullHeat() {
		return hullHeat > 0;
	}

	private int[] getNeighbouringSlots(int id) {
		switch(id) {
		case 0:
			return new int[] { 1, 5 };
		case 1:
			return new int[] { 0, 6 };
		case 2:
			return new int[] { 3, 7 };
		case 3:
			return new int[] { 2, 4, 8 };
		case 4:
			return new int[] { 3, 9 };
		case 5:
			return new int[] { 0, 6, 10 };
		case 6:
			return new int[] { 1, 5, 11 };
		case 7:
			return new int[] { 2, 8 };
		case 8:
			return new int[] { 3, 7, 9 };
		case 9:
			return new int[] { 4, 8 };
		case 10:
			return new int[] { 5, 11 };
		case 11:
			return new int[] { 6, 10 };
		}
		return null;
	}

	public int getFuelPercent() {
		if(getRodCount() == 0)
			return 0;

		int rodMax = 0;
		int rod = 0;

		for(int i = 0; i < 12; i++) {
			if(rodMaxDuration[i] > 0) {
				rodMax += rodMaxDuration[i];
				rod += rodDuration[i];
			}
		}

		if(rodMax == 0)
			return 0;

		return rod * 100 / rodMax;
	}

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			age++;
			if(age >= 20) {
				age = 0;
			}

			tanks[0].loadTank(12, 13, slots);
			tanks[1].loadTank(14, 15, slots);

			// Control rod movement
			if(retracting && rods > 0) {
				if(rods == rodsMax)
					this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:block.reactorStart", 1.0F, 0.75F);
				rods--;
				if(rods == 0)
					this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:block.reactorStop", 1.0F, 1.0F);
			}
			if(!retracting && rods < rodsMax) {
				if(rods == 0)
					this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:block.reactorStart", 1.0F, 0.75F);
				rods++;
				if(rods == rodsMax)
					this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:block.reactorStop", 1.0F, 1.0F);
			}

			// Process fuel rods when fully inserted
			if(rods >= rodsMax) {
				for(int i = 0; i < 12; i++) {
					if(slots[i] != null && slots[i].getItem() instanceof ItemBreedingRod) {
						processRod(i);
					}
				}
			}

			coreHeatMod = 1.0;
			hullHeatMod = 1.0;
			conversionMod = 1.0;

			getInteractions();

			// Emergency coolant system - only if water/steam is depleted
			if(this.coreHeat > 0 && this.tanks[0].getFill() <= 0 && this.tanks[1].getFill() > 0 && this.hullHeat < this.maxHullHeat) {
				this.hullHeat += this.coreHeat * 0.175 * hullHeatMod;
				this.coreHeat -= this.coreHeat * 0.1;
				this.tanks[1].setFill(this.tanks[1].getFill() - 10);
				if(this.tanks[1].getFill() < 0)
					this.tanks[1].setFill(0);
			}

			if(this.hullHeat > maxHullHeat) {
				this.hullHeat = maxHullHeat;
			}

			// Primary cooling: water -> steam
			if(this.hullHeat > 0 && this.tanks[0].getFill() > 0) {
				generateSteam();
				this.hullHeat -= this.hullHeat * 0.085;
			}

			// Better core to hull heat transfer (increased from 5% to 15%)
			if(this.coreHeat > 0 && this.hullHeat < this.maxHullHeat) {
				int transfer = (int)(this.coreHeat * 0.15);
				this.hullHeat += transfer;
				this.coreHeat -= transfer;
			}

			// Natural cooling - much slower when no water or coolant is available
			if(this.coreHeat > 0) {
				double coolingRate = (double) this.coreHeat / (double) this.maxCoreHeat;
				// Check if we have cooling available
				boolean hasCooling = tanks[0].getFill() > 0 || tanks[1].getFill() > 0 || isSubmerged();
				
				if(hasCooling) {
					// Normal cooling with water/coolant
					this.coreHeat -= (int)(coolingRate * 10); // Up to 10 heat/tick at max temp
				} else {
					// Much slower cooling when no water or coolant
					// BUT: If rods are down (0), allow faster cooling even without coolant
					if(rods == 0) {
						// With rods down, cool at moderate speed even without coolant
						this.coreHeat -= (int)(coolingRate * 5); // 5 heat/tick max at max temp (half of normal)
					} else {
						// With rods up and no coolant, very slow cooling
						this.coreHeat -= (int)(coolingRate * 1); // Only 1 heat/tick max at max temp (90% slower)
					}
				}
			}
			
			if(this.hullHeat > 0) {
				double coolingRate = (double) this.hullHeat / (double) this.maxHullHeat;
				// Check if we have cooling available
				boolean hasCooling = tanks[0].getFill() > 0 || tanks[1].getFill() > 0 || isSubmerged();
				
				if(hasCooling) {
					// Normal cooling with water/coolant
					this.hullHeat -= (int)(coolingRate * 5); // Up to 5 heat/tick at max temp
				} else {
					// Much slower cooling when no water or coolant
					// BUT: If rods are down (0), allow faster cooling even without coolant
					if(rods == 0) {
						// With rods down, cool at moderate speed even without coolant
						this.hullHeat -= (int)(coolingRate * 3); // 3 heat/tick max at max temp
					} else {
						// With rods up and no coolant, very slow cooling
						this.hullHeat -= (int)(coolingRate * 0.5); // Only 0.5 heat/tick max at max temp (90% slower)
					}
				}
			}

			// Water blocks help cooling (but only if water blocks exist around)
			if(isSubmerged() && this.hullHeat > 0) {
				this.hullHeat -= this.hullHeat * 0.02;
			}

			if(this.coreHeat > maxCoreHeat) {
				this.explode();
			}

			// Radiation leak only when overheating (>50% max temp) AND rods are up
			if(rods > 0 && coreHeat > maxCoreHeat * 0.50) {
				float rad = (float) coreHeat / (float) maxCoreHeat * 50F;
				ChunkRadiationManager.proxy.incrementRad(worldObj, xCoord, yCoord, zCoord, rad);
			}

			// Fluid network subscriptions
			this.subscribeToAllAround(tanks[0].getTankType(), this);
			this.subscribeToAllAround(tanks[1].getTankType(), this);
			this.sendFluidToAll(tanks[2], this);

			this.networkPackNT(20);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(rods);
		buf.writeBoolean(retracting);
		buf.writeInt(coreHeat);
		buf.writeInt(hullHeat);
		for(int i = 0; i < 3; i++) tanks[i].serialize(buf);
		// Serialize rod data for client
		for(int i = 0; i < 12; i++) {
			buf.writeInt(rodFlux[i]);
			buf.writeInt(rodHeat[i]);
			buf.writeInt(rodDuration[i]);
			buf.writeInt(rodMaxDuration[i]);
			buf.writeBoolean(rodLocked[i]);
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.rods = buf.readInt();
		this.retracting = buf.readBoolean();
		this.coreHeat = buf.readInt();
		this.hullHeat = buf.readInt();
		for(int i = 0; i < 3; i++) tanks[i].deserialize(buf);
		// Deserialize rod data on client
		for(int i = 0; i < 12; i++) {
			rodFlux[i] = buf.readInt();
			rodHeat[i] = buf.readInt();
			rodDuration[i] = buf.readInt();
			rodMaxDuration[i] = buf.readInt();
			rodLocked[i] = buf.readBoolean();
		}
	}

	private void processRod(int id) {
		if(id > 11)
			return;

		ItemStack stack = slots[id];
		if(stack == null || !(stack.getItem() instanceof ItemBreedingRod))
			return;

		// Initialize rod if not locked yet
		if(!rodLocked[id]) {
			BreederRecipe recipe = BreederRecipes.getOutput(stack);
			if(recipe != null) {
				// Lock the rod and initialize its data
				rodLocked[id] = true;
				rodFlux[id] = recipe.flux;
				// Multiply duration by 60 for 20+ real-time minutes (20 ticks/sec * 60 sec/min * 20+ min)
				rodMaxDuration[id] = recipe.flux * 60;
				rodDuration[id] = recipe.flux * 60;
				rodOutput[id] = recipe.output.copy();
				
				// Determine heat based on rod type
				BreedingRodType type = BreedingRodType.values()[stack.getItemDamage()];
				 int baseHeat = (int)(getHeatForRodType(type) / 2);

				// Apply multiplier based on rod type (single, dual, quad)
                // User specified: quad = as is, single = /4, dual = /2
                if(stack.getItem() == ModItems.rod) {
                    rodHeat[id] = baseHeat / 4;
                } else if(stack.getItem() == ModItems.rod_dual) {
                    rodHeat[id] = baseHeat / 2;
                } else if(stack.getItem() == ModItems.rod_quad) {
                    rodHeat[id] = baseHeat;
                } else {
                    // Fallback for other items? Default to quad heat.
                    rodHeat[id] = baseHeat;
                }
			}
		}

		// Process locked rod
		if(rodLocked[id] && rodDuration[id] > 0) {
			int neighbours = getNeightbourCount(id);
			int processRate = neighbours + 1;

			// Generate heat
			this.coreHeat += rodHeat[id] * processRate * coreHeatMod;

			// Decrease duration
			rodDuration[id] -= processRate;

			if(rodDuration[id] <= 0) {
				// Rod depleted - unlock and replace with output
				rodDuration[id] = 0;
				rodLocked[id] = false;
				if(rodOutput[id] != null) {
					slots[id] = rodOutput[id].copy();
				}
				// Clear rod data
				rodFlux[id] = 0;
				rodHeat[id] = 0;
				rodMaxDuration[id] = 0;
				rodOutput[id] = null;
			}
		}
	}

	private int getHeatForRodType(BreedingRodType type) {
		// THESE VALUES ARE NOW FOR QUAD RODS
		switch(type) {
		case U235: return 75;
		case PU239: return 100;
		case U238: return 25;
		case URANIUM: return 40;
		case TH232: return 15;
		case THF: return 60;
		case RGP: return 90;
		case NP237: return 50;
		case PU238: return 80;
		case LITHIUM: return 0;
		case TRITIUM: return 0;
		case CO: return 0;
		case CO60: return 5;
		case RA226: return 15;
		case AC227: return 20;
		case WASTE: return 5;
		case LEAD: return 0;
		default: return 25;
		}
	}

	private void generateSteam() {
		// Base steam generation scales with heat percentage
		double heatPercent = (double) hullHeat / (double) maxHullHeat;
		double baseSteam = heatPercent * 25000D * conversionMod; // Up to 25000 steam/tick at max heat
		
		double steam = baseSteam;
		double water = baseSteam;

		FluidType type = tanks[2].getTankType();
		if(type == Fluids.STEAM) {
			water /= 100D; // 1:100 ratio (1000mb water → 100000mb steam)
		} else if(type == Fluids.HOTSTEAM) {
			water /= 10D; // 1:10 ratio (1000mb water → 10000mb hot steam)
		} else if(type == Fluids.SUPERHOTSTEAM) {
			// 1:1 ratio (1000mb water → 1000mb super hot steam)
		}

		int waterUsed = (int) Math.ceil(water);
		int steamProduced = (int) Math.floor(steam);
		
		// FIXED: Use as much water as available, even if less than waterUsed
		// This allows water to go to 0 instead of getting stuck
		int availableWater = tanks[0].getFill();
		if(availableWater > 0) {
			// Calculate the actual amount we can use
			waterUsed = Math.min(waterUsed, availableWater);
			
			// Adjust steam production based on actual water used
			if(water > 0) {
				double ratio = (double) waterUsed / water;
				steamProduced = (int) Math.floor(steam * ratio);
			} else {
				steamProduced = 0;
			}
			
			tanks[0].setFill(tanks[0].getFill() - waterUsed);
			tanks[2].setFill(tanks[2].getFill() + steamProduced);
		}

		if(tanks[0].getFill() < 0)
			tanks[0].setFill(0);

		if(tanks[2].getFill() > tanks[2].getMaxFill())
			tanks[2].setFill(tanks[2].getMaxFill());
	}

	private void getInteractions() {
		getInteractionForBlock(xCoord + 1, yCoord + 1, zCoord);
		getInteractionForBlock(xCoord - 1, yCoord + 1, zCoord);
		getInteractionForBlock(xCoord, yCoord + 1, zCoord + 1);
		getInteractionForBlock(xCoord, yCoord + 1, zCoord - 1);
	}

	private double coreHeatMod = 1.0D;
	private double hullHeatMod = 1.0D;
	private double conversionMod = 1.0D;

	private void getInteractionForBlock(int x, int y, int z) {
		Block b = worldObj.getBlock(x, y, z);

		if(b == Blocks.lava || b == Blocks.flowing_lava) {
			hullHeatMod *= 3;
			conversionMod *= 0.5;
		} else if(b == Blocks.redstone_block) {
			conversionMod *= 1.15;
		} else if(b == ModBlocks.block_lead) {
			coreHeatMod *= 0.95;
		} else if(b == Blocks.water || b == Blocks.flowing_water) {
			tanks[0].setFill(tanks[0].getFill() + 25);
			if(tanks[0].getFill() > tanks[0].getMaxFill())
				tanks[0].setFill(tanks[0].getMaxFill());
		} else if(b == ModBlocks.block_uranium) {
			coreHeatMod *= 1.05;
		} else if(b == Blocks.coal_block) {
			hullHeatMod *= 1.1;
		} else if(b == ModBlocks.block_beryllium) {
			hullHeatMod *= 0.95;
			conversionMod *= 1.05;
		} else if(b == ModBlocks.block_schrabidium) {
			conversionMod *= 1.25;
			hullHeatMod *= 1.1;
		}
	}

	public int getRodCount() {
		int count = 0;
		for(int i = 0; i < 12; i++) {
			if(slots[i] != null && slots[i].getItem() instanceof ItemBreedingRod)
				count++;
		}
		return count;
	}

	private boolean hasFuelRod(int id) {
		if(id > 11)
			return false;
		if(slots[id] != null)
			return slots[id].getItem() instanceof ItemBreedingRod && rodLocked[id];
		return false;
	}

	private int getNeightbourCount(int id) {
		int[] neighbours = this.getNeighbouringSlots(id);
		if(neighbours == null)
			return 0;
		int count = 0;
		for(int i = 0; i < neighbours.length; i++)
			if(hasFuelRod(neighbours[i]))
				count++;
		return count;
	}

	private void explode() {
		for(int i = 0; i < slots.length; i++) {
			this.slots[i] = null;
		}

		worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
		worldObj.createExplosion(null, this.xCoord, this.yCoord, this.zCoord, 18.0F, true);
		ExplosionNukeGeneric.waste(worldObj, this.xCoord, this.yCoord, this.zCoord, 35);
		worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, ModBlocks.toxic_block);

		ChunkRadiationManager.proxy.incrementRad(worldObj, xCoord, yCoord, zCoord, 1000);
		
		if(MobConfig.enableElementals) {
			List<EntityPlayer> players = worldObj.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5).expand(100, 100, 100));
			for(EntityPlayer player : players) {
				player.getEntityData().getCompoundTag(player.PERSISTED_NBT_TAG).setBoolean("radMark", true);
			}
		}
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
	
	public boolean isSubmerged() {
		return worldObj.getBlock(xCoord + 1, yCoord + 1, zCoord).getMaterial() == Material.water ||
				worldObj.getBlock(xCoord, yCoord + 1, zCoord + 1).getMaterial() == Material.water ||
				worldObj.getBlock(xCoord - 1, yCoord + 1, zCoord).getMaterial() == Material.water ||
				worldObj.getBlock(xCoord, yCoord + 1, zCoord - 1).getMaterial() == Material.water;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineReactorSmall(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineReactorSmall(player.inventory, this);
	}

	@Override
	public FluidTank[] getAllTanks() {
		return tanks;
	}

	@Override
	public FluidTank[] getSendingTanks() {
		return new FluidTank[] { tanks[2] };
	}

	@Override
	public FluidTank[] getReceivingTanks() {
		return new FluidTank[] { tanks[0], tanks[1] };
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		// All slots accessible from all sides
		return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int j) {
		// Fuel rod slots - can only extract if not locked and not partially used
		if(i >= 0 && i <= 11) {
			if(rodLocked[i])
				return false;
			// Check if rod has been used (duration is less than max)
			if(rodMaxDuration[i] > 0 && rodDuration[i] < rodMaxDuration[i] && rodDuration[i] > 0) {
				return false; // Rod is partially used
			}
			return true;
		}
		// Output slots
		if(i == 13 || i == 15)
			return true;

		return false;
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return true;
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		// Control rod button
		if(data.hasKey("rods")) {
			this.retracting = !this.retracting;
		}
		
		// Steam compression button
		if(data.hasKey("compression")) {
			int compression = data.getInteger("compression");
			if(compression == 0) {
				tanks[2].setTankType(Fluids.STEAM);
			} else if(compression == 1) {
				tanks[2].setTankType(Fluids.HOTSTEAM);
			} else if(compression == 2) {
				tanks[2].setTankType(Fluids.SUPERHOTSTEAM);
			}
		}
		
		this.markDirty();
	}
}