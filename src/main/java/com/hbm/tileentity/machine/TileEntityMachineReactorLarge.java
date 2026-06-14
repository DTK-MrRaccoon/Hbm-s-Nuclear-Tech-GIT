package com.hbm.tileentity.machine;

import java.util.Arrays;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockReactorPart;
import com.hbm.config.MobConfig;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.container.ContainerReactorMultiblock;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIReactorMultiblock;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.fluid.IFluidStandardTransceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMachineReactorLarge extends TileEntityLoadedBase implements ISidedInventory, IFluidStandardTransceiver, IGUIProvider, IControlReceiver {

	private ItemStack slots[];

	public int hullHeat;
	public final int maxHullHeat = 100000;
	public int coreHeat;
	public final int maxCoreHeat = 50000;
	public int rods;
	public final int rodsMax = 100;
	public int age = 0;
	public FluidTank[] tanks;
	public ReactorFuelType type;
	public int fuel;
	public int maxFuel;
	public int waste;
	public int maxWaste;
	public int compression = 0;
	public int size = 1;

	public static final int billetsPerLayer = 32;
	public static final int fuelUnitsPerBillet = 1000;
	public static final int cycleDuration = 480000;
	private static final double steamFactor = 160.0 * 100.0;

	private static final int[] slots_top = new int[] { 0, 4, 6 };
	private static final int[] slots_bottom = new int[] { 1, 3, 5, 7 };
	private static final int[] slots_side = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };

	private String customName;

	public TileEntityMachineReactorLarge() {
		slots = new ItemStack[8];
		tanks = new FluidTank[3];
		tanks[0] = new FluidTank(Fluids.WATER, 128000);
		tanks[1] = new FluidTank(Fluids.COOLANT, 64000);
		tanks[2] = new FluidTank(Fluids.STEAM, 32000);
		type = ReactorFuelType.UNKNOWN;
		updateMaxCapacities();
	}

	private void updateMaxCapacities() {
		maxFuel = Math.max(billetsPerLayer * size * fuelUnitsPerBillet, 1);
		maxWaste = maxFuel;
		tanks[0].changeTankSize(Math.max(128000 * size, 1000));
		tanks[1].changeTankSize(Math.max(64000 * size, 1000));
		tanks[2].changeTankSize(Math.max(32000 * size, 1000));
	}

	private boolean isPartAt(int x, int y, int z, BlockReactorPart.ReactorPart expected) {
		Block b = worldObj.getBlock(x, y, z);
		if (b != ModBlocks.reactor_part) return false;
		
		int meta = worldObj.getBlockMetadata(x, y, z);
		return BlockReactorPart.getSubtype(meta) == expected;
	}

	private boolean isEjectorFacing(int x, int y, int z, ForgeDirection facing) {
		Block b = worldObj.getBlock(x, y, z);
		if (b != ModBlocks.reactor_part) return false;
		
		int meta = worldObj.getBlockMetadata(x, y, z);
		if (BlockReactorPart.getSubtype(meta) != BlockReactorPart.ReactorPart.EJECTOR) return false;
		
		return BlockReactorPart.getDirection(meta) == facing;
	}

	private boolean isInserterFacing(int x, int y, int z, ForgeDirection facing) {
		Block b = worldObj.getBlock(x, y, z);
		if (b != ModBlocks.reactor_part) return false;
		
		int meta = worldObj.getBlockMetadata(x, y, z);
		if (BlockReactorPart.getSubtype(meta) != BlockReactorPart.ReactorPart.INSERTER) return false;
		
		return BlockReactorPart.getDirection(meta) == facing;
	}

	public boolean checkBody() {
		return isPartAt(xCoord + 1, yCoord, zCoord + 1, BlockReactorPart.ReactorPart.ELEMENT) &&
				isPartAt(xCoord - 1, yCoord, zCoord + 1, BlockReactorPart.ReactorPart.ELEMENT) &&
				isPartAt(xCoord - 1, yCoord, zCoord - 1, BlockReactorPart.ReactorPart.ELEMENT) &&
				isPartAt(xCoord + 1, yCoord, zCoord - 1, BlockReactorPart.ReactorPart.ELEMENT) &&
				isPartAt(xCoord + 1, yCoord, zCoord, BlockReactorPart.ReactorPart.CONTROL) &&
				isPartAt(xCoord - 1, yCoord, zCoord, BlockReactorPart.ReactorPart.CONTROL) &&
				isPartAt(xCoord, yCoord, zCoord + 1, BlockReactorPart.ReactorPart.CONTROL) &&
				isPartAt(xCoord, yCoord, zCoord - 1, BlockReactorPart.ReactorPart.CONTROL);
	}

	public boolean checkSegment(int offset) {
		return isPartAt(xCoord + 1, yCoord + offset, zCoord + 1, BlockReactorPart.ReactorPart.ELEMENT) &&
				isPartAt(xCoord - 1, yCoord + offset, zCoord + 1, BlockReactorPart.ReactorPart.ELEMENT) &&
				isPartAt(xCoord - 1, yCoord + offset, zCoord - 1, BlockReactorPart.ReactorPart.ELEMENT) &&
				isPartAt(xCoord + 1, yCoord + offset, zCoord - 1, BlockReactorPart.ReactorPart.ELEMENT) &&
				isPartAt(xCoord + 1, yCoord + offset, zCoord, BlockReactorPart.ReactorPart.CONTROL) &&
				isPartAt(xCoord - 1, yCoord + offset, zCoord, BlockReactorPart.ReactorPart.CONTROL) &&
				isPartAt(xCoord, yCoord + offset, zCoord + 1, BlockReactorPart.ReactorPart.CONTROL) &&
				isPartAt(xCoord, yCoord + offset, zCoord - 1, BlockReactorPart.ReactorPart.CONTROL) &&
				isPartAt(xCoord, yCoord + offset, zCoord, BlockReactorPart.ReactorPart.CONDUCTOR);
	}

	private float checkHull() {
		float max = getSize() * 12;
		float count = 0;
		for(int y = yCoord - depth; y <= yCoord + height; y++) {
			if(blocksRad(xCoord - 1, y, zCoord + 2)) count++;
			if(blocksRad(xCoord, y, zCoord + 2)) count++;
			if(blocksRad(xCoord + 1, y, zCoord + 2)) count++;
			if(blocksRad(xCoord - 1, y, zCoord - 2)) count++;
			if(blocksRad(xCoord, y, zCoord - 2)) count++;
			if(blocksRad(xCoord + 1, y, zCoord - 2)) count++;
			if(blocksRad(xCoord + 2, y, zCoord - 1)) count++;
			if(blocksRad(xCoord + 2, y, zCoord)) count++;
			if(blocksRad(xCoord + 2, y, zCoord + 1)) count++;
			if(blocksRad(xCoord - 2, y, zCoord - 1)) count++;
			if(blocksRad(xCoord - 2, y, zCoord)) count++;
			if(blocksRad(xCoord - 2, y, zCoord + 1)) count++;
		}
		if(count == 0) return 1;
		return 1 - (count / max);
	}

	private boolean blocksRad(int x, int y, int z) {
		Block b = worldObj.getBlock(x, y, z);
		if(b == ModBlocks.block_lead || b == ModBlocks.block_desh || b == ModBlocks.brick_concrete) {
			return true;
		}
		return b.getExplosionResistance(null) >= 100;
	}

	int height;
	int depth;

	private void calculateSize() {
		height = 0;
		depth = 0;
		for(int i = 0; i < 7; i++) {
			if(checkSegment(i + 1)) {
				height++;
			} else {
				break;
			}
		}
		for(int i = 0; i < 7; i++) {
			if(checkSegment(-i - 1)) {
				depth++;
			} else {
				break;
			}
		}
		size = height + depth + 1;
	}

	public void updateReactorSize() {
		calculateSize();
		updateMaxCapacities();
	}

	private int getSize() {
		return size;
	}

	private void generate() {
		if(rods <= 0 || fuel <= 0) return;

		int consumption = (int) ((maxFuel / (double) cycleDuration) * (rods / 100.0));
		if(consumption > fuel) consumption = fuel;
		if(consumption + waste > maxWaste) consumption = maxWaste - waste;
		
		fuel -= consumption;
		waste += consumption;

		int heat = (int) (((double) consumption / size) * type.heat);
		this.coreHeat += heat;
		if(this.coreHeat > maxCoreHeat) this.coreHeat = maxCoreHeat;
	}

	@Override
	public void updateEntity() {
		if (!worldObj.isRemote && checkBody()) {
			age++;
			if (age >= 20) age = 0;
			handleFluids();
			calculateSize();
			updateReactorSize();
			networkPackNT(20);
		}

		if(!worldObj.isRemote) {
			tanks[0].loadTank(0, 1, slots);
			tanks[1].loadTank(2, 3, slots);

			handleFuelRodInput();
			handleWasteAbsorberInput();

			if(rods > 0) generate();

			if (this.coreHeat > 0 && this.tanks[1].getFill() > 0 && this.hullHeat < this.maxHullHeat) {
				this.hullHeat += this.coreHeat * 0.175;
				this.coreHeat -= this.coreHeat * 0.1;
				this.tanks[1].setFill(this.tanks[1].getFill() - 10);
				if (this.tanks[1].getFill() < 0) this.tanks[1].setFill(0);
			}
			
			if (this.hullHeat > maxHullHeat) this.hullHeat = maxHullHeat;
			
			if (this.hullHeat > 0 && this.tanks[0].getFill() > 0) {
				generateSteam();
				this.hullHeat -= this.hullHeat * 0.085;
			}
			
			if (this.coreHeat > maxCoreHeat) this.explode();
			
			if (rods > 0 && coreHeat > 0 && age == 5) {
				float rad = (float)coreHeat / (float)maxCoreHeat * 50F;
				rad *= checkHull();
				ChunkRadiationManager.proxy.incrementRad(worldObj, xCoord, yCoord, zCoord, rad);
			}

			if(isEjectorFacing(xCoord, yCoord, zCoord - 2, ForgeDirection.NORTH)) tryEjectInto(xCoord, yCoord, zCoord - 3);
			if(isEjectorFacing(xCoord, yCoord, zCoord + 2, ForgeDirection.SOUTH)) tryEjectInto(xCoord, yCoord, zCoord + 3);
			if(isEjectorFacing(xCoord - 2, yCoord, zCoord, ForgeDirection.WEST)) tryEjectInto(xCoord - 3, yCoord, zCoord);
			if(isEjectorFacing(xCoord + 2, yCoord, zCoord, ForgeDirection.EAST)) tryEjectInto(xCoord + 3, yCoord, zCoord);

			if(isInserterFacing(xCoord, yCoord, zCoord - 2, ForgeDirection.NORTH)) tryInsertFrom(xCoord, yCoord, zCoord - 3);
			if(isInserterFacing(xCoord, yCoord, zCoord + 2, ForgeDirection.SOUTH)) tryInsertFrom(xCoord, yCoord, zCoord + 3);
			if(isInserterFacing(xCoord - 2, yCoord, zCoord, ForgeDirection.WEST)) tryInsertFrom(xCoord - 3, yCoord, zCoord);
			if(isInserterFacing(xCoord + 2, yCoord, zCoord, ForgeDirection.EAST)) tryInsertFrom(xCoord + 3, yCoord, zCoord);
		}
	}

	private void handleFuelRodInput() {
		if(slots[4] != null && (slots[4].getItem() instanceof ItemBreedingRod || isBilletFuel(slots[4]))) {
			ItemStack input = slots[4];
			
			if(isBilletFuel(input)) {
				ReactorFuelType billetType = getFuelTypeFromBillet(input);
				if(billetType != ReactorFuelType.UNKNOWN) {
					if(fuel > 0 || waste > 0) {
						if(billetType != this.type) return;
					}
					int billetFuel = fuelUnitsPerBillet;
					if(fuel + billetFuel <= maxFuel) {
						if(slots[5] == null || (slots[5].getItem() == ModItems.rod_empty && slots[5].stackSize < 64)) {
							fuel += billetFuel;
							slots[4].stackSize--;
							if(fuel == billetFuel && waste == 0) this.type = billetType;
							if(slots[4].stackSize <= 0) slots[4] = null;
						}
					}
				}
				return;
			}
			
			ItemStack rod = input;
			if(ItemBreedingRod.isFuelRod(rod)) {
				ReactorFuelType rodType = getFuelType(rod);
				if(rodType != ReactorFuelType.UNKNOWN) {
					if(fuel > 0 || waste > 0) {
						if(rodType != this.type) return;
					}
					
					int multiplier = 1;
					Item emptyRodType = ModItems.rod_empty;
					
					if(rod.getItem() == ModItems.rod_dual) {
						multiplier = 2;
						emptyRodType = ModItems.rod_dual_empty;
					} else if(rod.getItem() == ModItems.rod_quad) {
						multiplier = 4;
						emptyRodType = ModItems.rod_quad_empty;
					}
					
					int addFuel = fuelUnitsPerBillet * multiplier;
					if(fuel + addFuel <= maxFuel) {
						ItemStack emptyStack = new ItemStack(emptyRodType);
						if(slots[5] == null) {
							slots[5] = emptyStack;
							slots[4].stackSize--;
							if(fuel == 0 && waste == 0) this.type = rodType;
							fuel += addFuel;
						} else if(slots[5].getItem() == emptyStack.getItem() && slots[5].stackSize < 64) {
							slots[5].stackSize++;
							slots[4].stackSize--;
							if(fuel == 0 && waste == 0) this.type = rodType;
							fuel += addFuel;
						}
						
						if(slots[4].stackSize <= 0) slots[4] = null;
					}
				}
			}
		}
	}

	private void handleWasteAbsorberInput() {
		if(slots[6] != null && (slots[6].getItem() == ModItems.rod_empty || slots[6].getItem() == ModItems.rod_dual_empty || slots[6].getItem() == ModItems.rod_quad_empty)) {
			int multiplier = 1;
			Item wasteRodType = ModItems.rod;
			
			if(slots[6].getItem() == ModItems.rod_dual_empty) {
				multiplier = 2;
				wasteRodType = ModItems.rod_dual;
			} else if(slots[6].getItem() == ModItems.rod_quad_empty) {
				multiplier = 4;
				wasteRodType = ModItems.rod_quad;
			}
			
			int wasteNeeded = fuelUnitsPerBillet * multiplier;
			if(waste >= wasteNeeded) {
				ItemStack wasteRod = new ItemStack(wasteRodType, 1, ItemBreedingRod.BreedingRodType.WASTE.ordinal());
				if(slots[7] == null) {
					slots[7] = wasteRod;
					slots[6].stackSize--;
					waste -= wasteNeeded;
				} else if(slots[7].getItem() == wasteRod.getItem() && slots[7].getItemDamage() == wasteRod.getItemDamage() && slots[7].stackSize < 64) {
					slots[7].stackSize++;
					slots[6].stackSize--;
					waste -= wasteNeeded;
				}
				
				if(slots[6].stackSize <= 0) slots[6] = null;
			}
		}
	}

	private boolean isBilletFuel(ItemStack stack) {
		if(stack == null) return false;
		return stack.getItem() == ModItems.billet_u235 || 
			   stack.getItem() == ModItems.billet_u233 ||
			   stack.getItem() == ModItems.billet_uranium_fuel || 
			   stack.getItem() == ModItems.billet_thorium_fuel ||
			   stack.getItem() == ModItems.billet_mox_fuel || 
			   stack.getItem() == ModItems.billet_plutonium_fuel ||
			   stack.getItem() == ModItems.billet_schrabidium;
	}

	private ReactorFuelType getFuelTypeFromBillet(ItemStack stack) {
		if(stack == null) return ReactorFuelType.UNKNOWN;
		if(stack.getItem() == ModItems.billet_u235) return ReactorFuelType.U235;
		if(stack.getItem() == ModItems.billet_u233) return ReactorFuelType.U233;
		if(stack.getItem() == ModItems.billet_uranium_fuel) return ReactorFuelType.URANIUM;
		if(stack.getItem() == ModItems.billet_thorium_fuel) return ReactorFuelType.THORIUM;
		if(stack.getItem() == ModItems.billet_mox_fuel) return ReactorFuelType.MOX;
		if(stack.getItem() == ModItems.billet_plutonium_fuel) return ReactorFuelType.PLUTONIUM;
		if(stack.getItem() == ModItems.billet_schrabidium) return ReactorFuelType.SCHRABIDIUM;
		return ReactorFuelType.UNKNOWN;
	}

	private ReactorFuelType getFuelType(ItemStack rod) {
		if(rod == null) return ReactorFuelType.UNKNOWN;
		int fuel = rod.getItemDamage();
		if(fuel == ItemBreedingRod.BreedingRodType.URANIUM_FUEL.ordinal()) return ReactorFuelType.URANIUM;
		if(fuel == ItemBreedingRod.BreedingRodType.THF.ordinal()) return ReactorFuelType.THORIUM;
		if(fuel == ItemBreedingRod.BreedingRodType.MOX_FUEL.ordinal()) return ReactorFuelType.MOX;
		if(fuel == ItemBreedingRod.BreedingRodType.PLUTONIUM_FUEL.ordinal()) return ReactorFuelType.PLUTONIUM;
		if(fuel == ItemBreedingRod.BreedingRodType.SCHRABIDIUM.ordinal()) return ReactorFuelType.SCHRABIDIUM;
		if(fuel == ItemBreedingRod.BreedingRodType.U235.ordinal()) return ReactorFuelType.U235;
		if(fuel == ItemBreedingRod.BreedingRodType.U233.ordinal()) return ReactorFuelType.U233;
		return ReactorFuelType.UNKNOWN;
	}

	private void handleFluids() {
		for(ForgeDirection dir : new ForgeDirection[] {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}) {
			int hatchX = xCoord + dir.offsetX * 2;
			int hatchZ = zCoord + dir.offsetZ * 2;
			
			if(isPartAt(hatchX, yCoord, hatchZ, BlockReactorPart.ReactorPart.HATCH)) {
				int connX = hatchX + dir.offsetX;
				int connZ = hatchZ + dir.offsetZ;
				this.trySubscribe(tanks[0].getTankType(), worldObj, connX, yCoord, connZ, dir);
				this.trySubscribe(tanks[1].getTankType(), worldObj, connX, yCoord, connZ, dir);
				this.sendFluid(tanks[2], worldObj, connX, yCoord, connZ, dir);
			}
		}
		
		int topY = yCoord + height + 1;
		int botY = yCoord - depth - 1;
		this.sendFluid(tanks[2], worldObj, xCoord, topY, zCoord, ForgeDirection.UP);
		this.sendFluid(tanks[2], worldObj, xCoord, botY, zCoord, ForgeDirection.DOWN);
	}

	private void tryEjectInto(int x, int y, int z) {
		int ejectSize = fuelUnitsPerBillet;
		if(waste < ejectSize) return;
		
		TileEntity te = worldObj.getTileEntity(x, y, z);
		if(te instanceof net.minecraft.inventory.IInventory) {
			net.minecraft.inventory.IInventory chest = (net.minecraft.inventory.IInventory) te;
			ItemStack wasteItem = new ItemStack(ModItems.billet_nuclear_waste, 1);
			
			for(int i = 0; i < chest.getSizeInventory(); i++) {
				ItemStack slot = chest.getStackInSlot(i);
				if(slot != null && slot.getItem() == wasteItem.getItem() && slot.stackSize < slot.getMaxStackSize()) {
					slot.stackSize++;
					this.waste -= ejectSize;
					return;
				}
			}
			
			for(int i = 0; i < chest.getSizeInventory(); i++) {
				if(chest.getStackInSlot(i) == null) {
					chest.setInventorySlotContents(i, wasteItem.copy());
					this.waste -= ejectSize;
					return;
				}
			}
		}
	}

	private void tryInsertFrom(int x, int y, int z) {
		TileEntity te = worldObj.getTileEntity(x, y, z);
		if(te instanceof net.minecraft.inventory.IInventory) {
			net.minecraft.inventory.IInventory chest = (net.minecraft.inventory.IInventory) te;
			for(int i = 0; i < chest.getSizeInventory(); i++) {
				ItemStack stack = chest.getStackInSlot(i);
				
				if(stack != null && isBilletFuel(stack)) {
					ReactorFuelType billetType = getFuelTypeFromBillet(stack);
					if(billetType != ReactorFuelType.UNKNOWN) {
						if(fuel > 0 || waste > 0) {
							if(billetType != this.type) continue;
						}
						
						if(slots[4] == null) {
							slots[4] = stack.copy();
							slots[4].stackSize = 1;
							chest.decrStackSize(i, 1);
							return;
						} else if(slots[4].getItem() == stack.getItem() && slots[4].stackSize < 64) {
							slots[4].stackSize++;
							chest.decrStackSize(i, 1);
							return;
						}
					}
				}
			}
		}
	}

	private void generateSteam() {
		double steam = ((double) hullHeat / (double) maxHullHeat) * steamFactor * size;
		double water = steam;
		FluidType steamType = tanks[2].getTankType();
		
		if(steamType == Fluids.STEAM) water /= 100D;
		if(steamType == Fluids.HOTSTEAM) water /= 10;
		
		tanks[0].setFill(tanks[0].getFill() - (int) Math.ceil(water));
		tanks[2].setFill(tanks[2].getFill() + (int) Math.floor(steam));
		
		if(tanks[0].getFill() < 0) tanks[0].setFill(0);
		if(tanks[2].getFill() > tanks[2].getMaxFill()) tanks[2].setFill(tanks[2].getMaxFill());
	}

	private void explode() {
		for (int i = 0; i < slots.length; i++) {
			this.slots[i] = null;
		}
		
		int rad = (int)(((long)fuel) * 25000L / (maxFuel * 15L));
		ChunkRadiationManager.proxy.incrementRad(worldObj, xCoord, yCoord, zCoord, rad);
		worldObj.createExplosion(null, this.xCoord, this.yCoord, this.zCoord, 7.5F, true);
		ExplosionNukeGeneric.waste(worldObj, this.xCoord, this.yCoord, this.zCoord, 35);
		
		for(int i = yCoord - depth; i <= yCoord + height; i++) {
			if(worldObj.rand.nextInt(2) == 0) randomizeRadBlock(this.xCoord + 1, i, this.zCoord + 1);
			if(worldObj.rand.nextInt(2) == 0) randomizeRadBlock(this.xCoord + 1, i, this.zCoord - 1);
			if(worldObj.rand.nextInt(2) == 0) randomizeRadBlock(this.xCoord - 1, i, this.zCoord - 1);
			if(worldObj.rand.nextInt(2) == 0) randomizeRadBlock(this.xCoord - 1, i, this.zCoord + 1);
			if(worldObj.rand.nextInt(5) == 0) worldObj.createExplosion(null, this.xCoord, this.yCoord, this.zCoord, 5.0F, true);
		}
		
		worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, ModBlocks.sellafield, 5, 3);
		
		if(MobConfig.enableElementals) {
			List<EntityPlayer> players = worldObj.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5).expand(100, 100, 100));
			for(EntityPlayer player : players) {
				player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).setBoolean("radMark", true);
			}
		}
	}

	private void randomizeRadBlock(int x, int y, int z) {
		int rand = worldObj.rand.nextInt(20);
		if(rand < 7) {
			worldObj.setBlock(x, y, z, ModBlocks.toxic_block);
		} else if(rand < 10) {
			worldObj.setBlock(x, y, z, ModBlocks.sellafield, 0, 3);
		} else if(rand < 14) {
			worldObj.setBlock(x, y, z, ModBlocks.sellafield, 1, 3);
		} else if(rand < 17) {
			worldObj.setBlock(x, y, z, ModBlocks.sellafield, 2, 3);
		} else if(rand < 19) {
			worldObj.setBlock(x, y, z, ModBlocks.sellafield, 3, 3);
		} else {
			worldObj.setBlock(x, y, z, ModBlocks.sellafield, 4, 3);
		}
	}

	@Override
	public int getSizeInventory() {
		return slots.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return slots[i];
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		if (slots[i] != null) {
			ItemStack itemStack = slots[i];
			slots[i] = null;
			return itemStack;
		} else {
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemStack) {
		slots[i] = itemStack;
		if (itemStack != null && itemStack.stackSize > getInventoryStackLimit()) {
			itemStack.stackSize = getInventoryStackLimit();
		}
	}

	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container.reactorLarge";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}

	public void setCustomName(String name) {
		this.customName = name;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64;
	}

	@Override public void openInventory() {}
	@Override public void closeInventory() {}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		if(i == 0) return FluidContainerRegistry.getFluidContent(stack, Fluids.WATER) > 0;
		if(i == 2) return FluidContainerRegistry.getFluidContent(stack, Fluids.COOLANT) > 0;
		if(i == 4) {
			if(!(stack.getItem() instanceof ItemBreedingRod) && !isBilletFuel(stack)) return false;
			ReactorFuelType stackType = ReactorFuelType.UNKNOWN;
			if(isBilletFuel(stack)) stackType = getFuelTypeFromBillet(stack);
			else if(stack.getItem() instanceof ItemBreedingRod) stackType = getFuelType(stack);
			
			if(stackType == ReactorFuelType.UNKNOWN) return false;
			if(fuel > 0 || waste > 0) {
				return stackType == this.type;
			}
			return true;
		}
		if(i == 6) return stack.getItem() == ModItems.rod_empty || stack.getItem() == ModItems.rod_dual_empty || stack.getItem() == ModItems.rod_quad_empty;
		return false;
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		if (slots[i] != null) {
			if (slots[i].stackSize <= j) {
				ItemStack itemStack = slots[i];
				slots[i] = null;
				return itemStack;
			}
			ItemStack itemStack1 = slots[i].splitStack(j);
			if (slots[i].stackSize == 0) slots[i] = null;
			return itemStack1;
		} else {
			return null;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagList list = nbt.getTagList("items", 10);
		coreHeat = nbt.getInteger("heat");
		hullHeat = nbt.getInteger("hullHeat");
		rods = nbt.getInteger("rods");
		fuel = nbt.getInteger("fuel");
		waste = nbt.getInteger("waste");
		compression = nbt.getInteger("compression");
		size = nbt.getInteger("size");
		
		if(size < 1) size = 1;
		
		slots = new ItemStack[getSizeInventory()];
		tanks[0].readFromNBT(nbt, "water");
		tanks[1].readFromNBT(nbt, "coolant");
		tanks[2].readFromNBT(nbt, "steam");
		type = ReactorFuelType.getEnum(nbt.getInteger("type"));
		
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound nbt1 = list.getCompoundTagAt(i);
			byte b0 = nbt1.getByte("slot");
			if (b0 >= 0 && b0 < slots.length) {
				slots[b0] = ItemStack.loadItemStackFromNBT(nbt1);
			}
		}
		updateMaxCapacities();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("heat", coreHeat);
		nbt.setInteger("hullHeat", hullHeat);
		nbt.setInteger("rods", rods);
		nbt.setInteger("fuel", fuel);
		nbt.setInteger("waste", waste);
		nbt.setInteger("compression", compression);
		nbt.setInteger("size", size);
		
		NBTTagList list = new NBTTagList();
		tanks[0].writeToNBT(nbt, "water");
		tanks[1].writeToNBT(nbt, "coolant");
		tanks[2].writeToNBT(nbt, "steam");
		nbt.setInteger("type", type.getID());
		
		for (int i = 0; i < slots.length; i++) {
			if (slots[i] != null) {
				NBTTagCompound nbt1 = new NBTTagCompound();
				nbt1.setByte("slot", (byte) i);
				slots[i].writeToNBT(nbt1);
				list.appendTag(nbt1);
			}
		}
		nbt.setTag("items", list);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return side == 0 ? slots_bottom : (side == 1 ? slots_top : slots_side);
	}

	@Override
	public boolean canInsertItem(int i, ItemStack itemStack, int j) {
		return isItemValidForSlot(i, itemStack);
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int j) {
		return i == 1 || i == 3 || i == 5 || i == 7;
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(rods);
		buf.writeInt(coreHeat);
		buf.writeInt(hullHeat);
		buf.writeInt(fuel);
		buf.writeInt(waste);
		buf.writeInt(type.getID());
		buf.writeInt(compression);
		buf.writeInt(size);
		for(int i = 0; i < 3; i++) tanks[i].serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		rods = buf.readInt();
		coreHeat = buf.readInt();
		hullHeat = buf.readInt();
		fuel = buf.readInt();
		waste = buf.readInt();
		type = ReactorFuelType.getEnum(buf.readInt());
		compression = buf.readInt();
		size = buf.readInt();
		
		if(size < 1) size = 1;
		for(int i = 0; i < 3; i++) tanks[i].deserialize(buf);
		
		updateMaxCapacities();
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("rods")) this.rods = data.getInteger("rods");
		
		if(data.hasKey("compression")) {
			this.compression = data.getInteger("compression");
			if(this.compression == 0) tanks[2].setTankType(Fluids.STEAM);
			else if(this.compression == 1) tanks[2].setTankType(Fluids.HOTSTEAM);
			else tanks[2].setTankType(Fluids.SUPERHOTSTEAM);
		}
		this.markDirty();
	}

	public int getCoreHeatScaled(int i) { return maxCoreHeat > 0 ? (coreHeat * i) / maxCoreHeat : 0; }
	public int getHullHeatScaled(int i) { return maxHullHeat > 0 ? (hullHeat * i) / maxHullHeat : 0; }
	public int getFuelScaled(int i) { return maxFuel > 0 ? (fuel * i) / maxFuel : 0; }
	public int getWasteScaled(int i) { return maxWaste > 0 ? (waste * i) / maxWaste : 0; }
	public int getSteamScaled(int i) { return tanks[2].getMaxFill() > 0 ? (tanks[2].getFill() * i) / tanks[2].getMaxFill() : 0; }
	
	public boolean hasCoreHeat() { return coreHeat > 0; }
	public boolean hasHullHeat() { return hullHeat > 0; }

	public enum ReactorFuelType {
		URANIUM(1875),
		THORIUM(750),
		PLUTONIUM(2250),
		MOX(2800),
		SCHRABIDIUM(45000),
		U235(3000),
		U233(4000),
		UNKNOWN(0);
		
		private int heat;
		
		private ReactorFuelType(int i) {
			heat = i;
		}
		
		public int getHeat() {
			return heat;
		}
		
		public int getID() {
			return Arrays.asList(ReactorFuelType.values()).indexOf(this);
		}
		
		public static ReactorFuelType getEnum(int i) {
			if(i < ReactorFuelType.values().length) return ReactorFuelType.values()[i];
			else return ReactorFuelType.UNKNOWN;
		}
	}

	@Override
	public FluidTank[] getAllTanks() {
		return tanks;
	}

	@Override
	public FluidTank[] getSendingTanks() {
		return new FluidTank[] {tanks[2]};
	}

	@Override
	public FluidTank[] getReceivingTanks() {
		return new FluidTank[] {tanks[0], tanks[1]};
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerReactorMultiblock(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIReactorMultiblock(player.inventory, this);
	}
}