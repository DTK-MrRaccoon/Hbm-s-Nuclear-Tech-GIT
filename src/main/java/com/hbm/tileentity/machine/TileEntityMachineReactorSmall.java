package com.hbm.tileentity.machine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.MobConfig;
import com.hbm.entity.projectile.EntityZirnoxDebris;
import com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.handler.CompatHandler;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.container.ContainerMachineReactorSmall;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIMachineReactorSmall;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.util.EnumUtil;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.fluid.IFluidStandardTransceiver;
import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.IRORValueProvider;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

@Optional.InterfaceList({
	@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")
})
public class TileEntityMachineReactorSmall extends TileEntityMachineBase
		implements IFluidStandardTransceiver, IGUIProvider, IControlReceiver, SimpleComponent,
				   CompatHandler.OCComponent, IRORValueProvider, IRORInteractive {

	public int hullHeat;
	public final int maxHullHeat = 100000;
	public int coreHeat;
	public final int maxCoreHeat = 100000;
	public int rods;
	public int rodsTarget;
	public final int rodsMax = 100;
	public boolean retracting = true;
	public FluidTank[] tanks;

	private static final Map<BreedingRodType, RodOutput> fuelMap = new HashMap<>();
	static {
		fuelMap.put(BreedingRodType.LITHIUM, new RodOutput(BreedingRodType.TRITIUM, 1.0f, null));
		fuelMap.put(BreedingRodType.CO, new RodOutput(BreedingRodType.CO60, 1.0f, null));
		fuelMap.put(BreedingRodType.TH232, new RodOutput(BreedingRodType.THF, 1.0f, null));
		fuelMap.put(BreedingRodType.THF, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.U235, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.NP237, new RodOutput(BreedingRodType.PU238, 0.5f, BreedingRodType.WASTE));
		fuelMap.put(BreedingRodType.PU238, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.U238, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.PU239, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.RGP, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.RA226, new RodOutput(BreedingRodType.AC227, 1.0f, null));
		fuelMap.put(BreedingRodType.AC227, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.MOX_FUEL, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.PLUTONIUM_FUEL, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.URANIUM_FUEL, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.U233, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
		fuelMap.put(BreedingRodType.LES, new RodOutput(BreedingRodType.WASTE, 1.0f, null));
	}

	private static class RodOutput {
		final BreedingRodType output;
		final float chance;
		final BreedingRodType alternate;
		RodOutput(BreedingRodType output, float chance, BreedingRodType alternate) {
			this.output = output;
			this.chance = chance;
			this.alternate = alternate;
		}
	}

	public TileEntityMachineReactorSmall() {
		super(16);
		tanks = new FluidTank[3];
		tanks[0] = new FluidTank(Fluids.WATER, 16000);
		tanks[1] = new FluidTank(Fluids.COOLANT, 8000);
		tanks[2] = new FluidTank(Fluids.STEAM, 64000);
		rodsTarget = 0;
	}

	@Override
	public String getName() {
		return "container.reactorSmall";
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemStack) {
		if(i >= 0 && i <= 11) {
			Item item = itemStack.getItem();
			return item instanceof ItemBreedingRod || item == ModItems.neutron_reflector;
		}
		if(i == 12) return FluidContainerRegistry.getFluidContent(itemStack, tanks[0].getTankType()) > 0;
		if(i == 14) return FluidContainerRegistry.getFluidContent(itemStack, tanks[1].getTankType()) > 0;
		return false;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack stack, int j) {
		if(i >= 0 && i <= 11) return true;
		if(i == 13 || i == 15) return true;
		return false;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		coreHeat = nbt.getInteger("heat");
		hullHeat = nbt.getInteger("hullHeat");
		rods = nbt.getInteger("rods");
		rodsTarget = nbt.getInteger("rodsTarget");
		retracting = nbt.getBoolean("ret");
		tanks[0].readFromNBT(nbt, "water");
		tanks[1].readFromNBT(nbt, "coolant");
		tanks[2].readFromNBT(nbt, "steam");
		clampHeat();
		if(rodsTarget < 0) rodsTarget = 0;
		if(rodsTarget > rodsMax) rodsTarget = rodsMax;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("heat", coreHeat);
		nbt.setInteger("hullHeat", hullHeat);
		nbt.setInteger("rods", rods);
		nbt.setInteger("rodsTarget", rodsTarget);
		nbt.setBoolean("ret", retracting);
		tanks[0].writeToNBT(nbt, "water");
		tanks[1].writeToNBT(nbt, "coolant");
		tanks[2].writeToNBT(nbt, "steam");
	}

	private void clampHeat() {
		if(coreHeat < 0) coreHeat = 0;
		if(hullHeat < 0) hullHeat = 0;
		if(coreHeat > maxCoreHeat) coreHeat = maxCoreHeat;
		if(hullHeat > maxHullHeat) hullHeat = maxHullHeat;
		if(coreHeat < 5) coreHeat = 0;
		if(hullHeat < 5) hullHeat = 0;
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

	public int getFuelPercent() {
		int totalMax = 0, totalRem = 0;
		for(int i = 0; i < 12; i++) {
			ItemStack stack = slots[i];
			if(stack != null && stack.getItem() instanceof ItemBreedingRod) {
				int max = ItemBreedingRod.getMaxLife(stack);
				if(max > 0) {
					totalMax += max;
					totalRem += ItemBreedingRod.getLifeTime(stack);
				}
			}
		}
		return totalMax == 0 ? 0 : (totalRem * 100 / totalMax);
	}

	@Override
	public void updateEntity() {
		if(worldObj.isRemote) return;

		tanks[0].loadTank(12, 13, slots);
		tanks[1].loadTank(14, 15, slots);

		// Decay heat - always generated by inert rods, regardless of control rod position
		for(int i = 0; i < 12; i++) {
			ItemStack stack = slots[i];
			if(stack != null && stack.getItem() instanceof ItemBreedingRod) {
				BreedingRodType type = EnumUtil.grabEnumSafely(BreedingRodType.class, stack.getItemDamage());
				if(type != null && type.maxLife <= 0) {
					int heat = ItemBreedingRod.getHeatPerTick(stack);
					if(heat > 0) {
						coreHeat += heat;
					}
				}
			}
		}

		if(rods < rodsTarget) {
			retracting = false;
			if(worldObj.getTotalWorldTime() % 3L == 0L) {
				if(rods == 0) worldObj.playSoundEffect(xCoord, yCoord, zCoord, "hbm:block.reactorStart", 1.0F, 0.75F);
				rods++;
				if(rods == rodsTarget) worldObj.playSoundEffect(xCoord, yCoord, zCoord, "hbm:block.reactorStop", 1.0F, 1.0F);
			}
		} else if(rods > rodsTarget) {
			retracting = true;
			if(worldObj.getTotalWorldTime() % 3L == 0L) {
				if(rods == rodsMax) worldObj.playSoundEffect(xCoord, yCoord, zCoord, "hbm:block.reactorStart", 1.0F, 0.75F);
				rods--;
				if(rods == rodsTarget) worldObj.playSoundEffect(xCoord, yCoord, zCoord, "hbm:block.reactorStop", 1.0F, 1.0F);
			}
		}

		if(rods > 0) {
			for(int i = 0; i < 12; i++) {
				ItemStack stack = slots[i];
				if(stack != null && (stack.getItem() instanceof ItemBreedingRod || stack.getItem() == ModItems.neutron_reflector)) {
					processRod(i);
				}
			}
		}

		if(coreHeat > hullHeat) {
			double coeff = 0.6;
			if(isSubmerged()) coeff *= 1.2;
			double transfer = (coreHeat - hullHeat) * coeff;
			coreHeat -= (int)Math.round(transfer);
			hullHeat += (int)Math.round(transfer);
		}

		boolean steamHasSpace = tanks[2].getFill() < tanks[2].getMaxFill() * 0.95;
		if(hullHeat > 0 && tanks[0].getFill() > 0 && steamHasSpace) {
			generateSteam();
		}

		if(tanks[1].getFill() >= 5) {
			int coolantUsed = 0;

			if(coreHeat > 85000) {
				int excess = coreHeat - 85000;
				int cooling = (int)Math.min((excess * excess) / 20000.0 * 1.25, 6250);
				cooling = Math.max(12, cooling);
				coreHeat -= cooling;
				coolantUsed += cooling / 150;
			}

			if(hullHeat > 85000) {
				int excess = hullHeat - 85000;
				int cooling = (int)Math.min((excess * excess) / 8000.0 * 1.25, 10000);
				cooling = Math.max(25, cooling);
				hullHeat -= cooling;
				coolantUsed += cooling / 300;
			}

			if(coolantUsed > 0) {
				coolantUsed = Math.max(1, coolantUsed);
				tanks[1].setFill(Math.max(0, tanks[1].getFill() - coolantUsed));
			}
		}

		if(coreHeat > 0) {
			double passive = isSubmerged() ? 300.0 : 3.0;
			coreHeat -= (int)passive;
		}
		if(hullHeat > 0) {
			double passive = isSubmerged() ? 400.0 : 4.0;
			hullHeat -= (int)passive;
		}

		clampHeat();

		if(coreHeat >= maxCoreHeat || hullHeat >= maxHullHeat) {
			explode();
			return;
		}

		if(rods > 0 && coreHeat > 75000) {
			float rad = (float) coreHeat / (float) maxCoreHeat * 5F;
			ChunkRadiationManager.proxy.incrementRad(worldObj, xCoord, yCoord, zCoord, rad);
		}

		subscribeToAllAround(tanks[0].getTankType(), this);
		subscribeToAllAround(tanks[1].getTankType(), this);
		sendFluidToAll(tanks[2], this);

		TileEntity te = worldObj.getTileEntity(xCoord, yCoord + 2, zCoord);
		if(te instanceof TileEntity) {
			subscribeToAllAround(tanks[0].getTankType(), te);
			subscribeToAllAround(tanks[1].getTankType(), te);
			for(DirPos pos : getConPos(te)) sendFluid(tanks[2], worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
		}

		networkPackNT(20);
	}

	private void processRod(int id) {
		ItemStack stack = slots[id];
		if(stack == null) return;

		// Neutron reflector: doesn't process itself, only boosts neighbors
		if(stack.getItem() == ModItems.neutron_reflector) return;

		BreedingRodType type = EnumUtil.grabEnumSafely(BreedingRodType.class, stack.getItemDamage());
		if(type == null) return;

		int life = ItemBreedingRod.getLifeTime(stack);
		int heatPerTick = ItemBreedingRod.getHeatPerTick(stack);
		boolean isFuel = type.isFuel;
		boolean isBreeding = type.isBreeding;
		int neighbours = getNeighbourCount(id);
		boolean adjacentFuel = hasAdjacentFuelRod(id);
		float powerFactor = rods / 100.0F;

		if(type.maxLife <= 0) return;

		if(life <= 0) {
			convertRod(id, stack, type);
			return;
		}

		float reactionRate = 0.0F;
		int actualHeat = 0;

		if(isFuel) {
			reactionRate = (neighbours + 1) * powerFactor;
		} else if(isBreeding && adjacentFuel) {
			reactionRate = powerFactor;
		}

		int consumption = 0;
		if(reactionRate > 0) {
			int intPart = (int) reactionRate / 5;
			float fracPart = reactionRate - intPart;
			consumption = intPart;
			if(fracPart > 0 && worldObj.rand.nextFloat() < fracPart) {
				consumption++;
			}
		}

		if(consumption > 0) {
			int newLife = Math.max(0, life - consumption);
			ItemBreedingRod.setLifeTime(stack, newLife);
			if(isFuel) {
				actualHeat = heatPerTick * consumption;
				coreHeat += actualHeat;
			}
		}
	}

	private boolean hasAdjacentFuelRod(int id) {
		int[] neighbours = getNeighbouringSlots(id);
		if(neighbours == null) return false;
		for(int i : neighbours) {
			ItemStack s = slots[i];
			if(s != null && ItemBreedingRod.isFuelRod(s)) {
				return true;
			}
		}
		return false;
	}

	private void convertRod(int slot, ItemStack stack, BreedingRodType type) {
		RodOutput out = fuelMap.get(type);
		if(out == null) {
			slots[slot] = null;
			return;
		}

		BreedingRodType chosen = out.output;
		if(out.chance < 1.0f && worldObj.rand.nextFloat() > out.chance) {
			chosen = out.alternate;
		}

		if(chosen == null) {
			slots[slot] = null;
			return;
		}

		ItemStack newStack = new ItemStack(stack.getItem(), 1, chosen.ordinal());
		ItemBreedingRod.setLifeTime(newStack, chosen.maxLife);
		slots[slot] = newStack;
	}

	private int getNeighbourCount(int id) {
		int[] neighbours = getNeighbouringSlots(id);
		if(neighbours == null) return 0;
		int count = 0;
		for(int i : neighbours) {
			ItemStack s = slots[i];
			if(s != null) {
				if(s.getItem() instanceof ItemBreedingRod || s.getItem() == ModItems.neutron_reflector) {
					count++;
				}
			}
		}
		return count;
	}

	private int[] getNeighbouringSlots(int id) {
		switch(id) {
			case 0: return new int[]{1,5};
			case 1: return new int[]{0,6};
			case 2: return new int[]{3,7};
			case 3: return new int[]{2,4,8};
			case 4: return new int[]{3,9};
			case 5: return new int[]{0,6,10};
			case 6: return new int[]{1,5,11};
			case 7: return new int[]{2,8};
			case 8: return new int[]{3,7,9};
			case 9: return new int[]{4,8};
			case 10: return new int[]{5,11};
			case 11: return new int[]{6,10};
			default: return null;
		}
	}

	private void generateSteam() {
		int reqTemp, waterRatio;
		double heatPerMb;

		if(tanks[2].getTankType() == Fluids.STEAM) {
			reqTemp = 10000;
			heatPerMb = 4.0;
			waterRatio = 100;
		} else if(tanks[2].getTankType() == Fluids.HOTSTEAM) {
			reqTemp = 30000;
			heatPerMb = 30.0;
			waterRatio = 10;
		} else {
			reqTemp = 45000;
			heatPerMb = 250.0;
			waterRatio = 1;
		}

		if(hullHeat < reqTemp) return;

		double excess = hullHeat - reqTemp;
		double maxSteam = excess / heatPerMb;
		if(maxSteam <= 0) return;

		int water = tanks[0].getFill();
		int space = tanks[2].getMaxFill() - tanks[2].getFill();
		if(water <= 0 || space <= 0) return;

		double maxFromWater = (double)water * waterRatio;
		int produce = (int)Math.min(maxSteam, Math.min(maxFromWater, space));
		if(produce <= 0) return;

		int waterUse = (int)Math.ceil((double)produce / waterRatio);
		waterUse = Math.min(waterUse, water);
		produce = waterUse * waterRatio;
		if(produce > space) produce = space;

		hullHeat = Math.max(0, hullHeat - (int)Math.round(produce * heatPerMb));
		tanks[0].setFill(tanks[0].getFill() - waterUse);
		tanks[2].setFill(tanks[2].getFill() + produce);
		if(tanks[2].getFill() > tanks[2].getMaxFill()) tanks[2].setFill(tanks[2].getMaxFill());
	}

	public boolean isSubmerged() {
		return worldObj.getBlock(xCoord+1, yCoord+1, zCoord).getMaterial() == Material.water ||
			   worldObj.getBlock(xCoord, yCoord+1, zCoord+1).getMaterial() == Material.water ||
			   worldObj.getBlock(xCoord-1, yCoord+1, zCoord).getMaterial() == Material.water ||
			   worldObj.getBlock(xCoord, yCoord+1, zCoord-1).getMaterial() == Material.water;
	}

	private void explode() {
		for(int i = 0; i < slots.length; i++) slots[i] = null;
		worldObj.setBlockToAir(xCoord, yCoord, zCoord);
		worldObj.setBlockToAir(xCoord, yCoord + 1, zCoord);
		worldObj.setBlockToAir(xCoord, yCoord + 2, zCoord);

		worldObj.playSoundEffect(xCoord, yCoord + 2, zCoord, "hbm:block.rbmk_explosion", 10.0F, 1.0F);
		worldObj.createExplosion(null, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, 4.0F, true);

		for(int i = 0; i < 8; i++) {
			spawnDebris(DebrisType.GRAPHITE);
		}
		for(int i = 0; i < 12; i++) {
			spawnDebris(DebrisType.BLANK);
		}

		worldObj.setBlock(xCoord, yCoord + 1, zCoord, ModBlocks.corium_block);
		worldObj.setBlock(xCoord + 1, yCoord + 1, zCoord, ModBlocks.corium_block);
		worldObj.setBlock(xCoord - 1, yCoord + 1, zCoord, ModBlocks.corium_block);
		worldObj.setBlock(xCoord, yCoord + 1, zCoord + 1, ModBlocks.corium_block);
		worldObj.setBlock(xCoord, yCoord + 1, zCoord - 1, ModBlocks.corium_block);
		worldObj.setBlock(xCoord, yCoord + 2, zCoord, ModBlocks.corium_block);

		ExplosionNukeGeneric.waste(worldObj, xCoord, yCoord, zCoord, 35);
		ChunkRadiationManager.proxy.incrementRad(worldObj, xCoord, yCoord, zCoord, 1000);

		if(MobConfig.enableElementals) {
			List<EntityPlayer> players = worldObj.getEntitiesWithinAABB(EntityPlayer.class,
				AxisAlignedBB.getBoundingBox(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5).expand(100, 100, 100));
			for(EntityPlayer p : players) p.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).setBoolean("radMark", true);
		}
	}

	private void spawnDebris(DebrisType type) {
		EntityZirnoxDebris debris = new EntityZirnoxDebris(worldObj, xCoord + 0.5D, yCoord + 2.5D, zCoord + 0.5D, type);
		debris.motionX = worldObj.rand.nextGaussian() * 0.75D;
		debris.motionZ = worldObj.rand.nextGaussian() * 0.75D;
		debris.motionY = 0.2D + worldObj.rand.nextDouble() * 1.5D;
		worldObj.spawnEntityInWorld(debris);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(rods);
		buf.writeInt(rodsTarget);
		buf.writeBoolean(retracting);
		buf.writeInt(coreHeat);
		buf.writeInt(hullHeat);
		for(int i = 0; i < 3; i++) tanks[i].serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		rods = buf.readInt();
		rodsTarget = buf.readInt();
		retracting = buf.readBoolean();
		coreHeat = buf.readInt();
		hullHeat = buf.readInt();
		for(int i = 0; i < 3; i++) tanks[i].deserialize(buf);
		if(rodsTarget < 0) rodsTarget = 0;
		if(rodsTarget > rodsMax) rodsTarget = rodsMax;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() { return INFINITE_EXTENT_AABB; }
	@Override @SideOnly(Side.CLIENT) public double getMaxRenderDistanceSquared() { return 65536.0D; }

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineReactorSmall(player.inventory, this);
	}
	@Override @SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineReactorSmall(player.inventory, this);
	}

	@Override public FluidTank[] getAllTanks() { return tanks; }
	@Override public FluidTank[] getSendingTanks() { return new FluidTank[]{tanks[2]}; }
	@Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{tanks[0], tanks[1]}; }

	@Override public int[] getAccessibleSlotsFromSide(int side) {
		return new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
	}

	@Override public boolean hasPermission(EntityPlayer player) { return true; }

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("rods")) {
			rodsTarget = data.getInteger("rods");
			if(rodsTarget < 0) rodsTarget = 0;
			if(rodsTarget > rodsMax) rodsTarget = rodsMax;
		}
		if(data.hasKey("active")) {
			rodsTarget = data.getBoolean("active") ? rodsMax : 0;
		}
		if(data.hasKey("compression")) {
			int c = data.getInteger("compression");
			if(c == 0) tanks[2].setTankType(Fluids.STEAM);
			else if(c == 1) tanks[2].setTankType(Fluids.HOTSTEAM);
			else tanks[2].setTankType(Fluids.SUPERHOTSTEAM);
		}
		markDirty();
	}

	private DirPos[] getConPos(TileEntity te) {
		return new DirPos[] {
			new DirPos(te.xCoord + 1, te.yCoord, te.zCoord, Library.POS_X),
			new DirPos(te.xCoord - 1, te.yCoord, te.zCoord, Library.NEG_X),
			new DirPos(te.xCoord, te.yCoord + 1, te.zCoord, Library.POS_Y),
			new DirPos(te.xCoord, te.yCoord - 1, te.zCoord, Library.NEG_Y),
			new DirPos(te.xCoord, te.yCoord, te.zCoord + 1, Library.POS_Z),
			new DirPos(te.xCoord, te.yCoord, te.zCoord - 1, Library.NEG_Z)
		};
	}

	// OpenComputers methods
	@Override
	@Optional.Method(modid = "OpenComputers")
	public String getComponentName() {
		return "small_reactor";
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getCoreHeat(Context context, Arguments args) {
		return new Object[] {coreHeat * 0.00002D * 980D + 20D};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getHullHeat(Context context, Arguments args) {
		return new Object[] {hullHeat * 0.00001D * 980D + 20D};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getWater(Context context, Arguments args) {
		return new Object[] {tanks[0].getFill()};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getCoolant(Context context, Arguments args) {
		return new Object[] {tanks[1].getFill()};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getSteam(Context context, Arguments args) {
		return new Object[] {tanks[2].getFill()};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getRodsLevel(Context context, Arguments args) {
		return new Object[] {rods};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getTargetRodsLevel(Context context, Arguments args) {
		return new Object[] {rodsTarget};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getFuelPercent(Context context, Arguments args) {
		return new Object[] {getFuelPercent()};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getSteamType(Context context, Arguments args) {
		FluidType type = tanks[2].getTankType();
		if(type == Fluids.STEAM) return new Object[] {0};
		else if(type == Fluids.HOTSTEAM) return new Object[] {1};
		else return new Object[] {2};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getInfo(Context context, Arguments args) {
		java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
		map.put("coreHeat", coreHeat);
		map.put("hullHeat", hullHeat);
		map.put("water", tanks[0].getFill());
		map.put("coolant", tanks[1].getFill());
		map.put("steam", tanks[2].getFill());
		map.put("rods", rods);
		map.put("targetRods", rodsTarget);
		map.put("fuelPercent", getFuelPercent());
		return new Object[] {map};
	}

	@Callback(direct = true, limit = 2)
	@Optional.Method(modid = "OpenComputers")
	public Object[] setRodsActive(Context context, Arguments args) {
		boolean active = args.checkBoolean(0);
		rodsTarget = active ? rodsMax : 0;
		markDirty();
		return new Object[] {true};
	}

	@Callback(direct = true, limit = 2)
	@Optional.Method(modid = "OpenComputers")
	public Object[] setRodsLevel(Context context, Arguments args) {
		int level = args.checkInteger(0);
		if(level < 0) level = 0;
		if(level > rodsMax) level = rodsMax;
		rodsTarget = level;
		markDirty();
		return new Object[] {true};
	}

	@Callback(direct = true, limit = 2)
	@Optional.Method(modid = "OpenComputers")
	public Object[] setSteamCompression(Context context, Arguments args) {
		int level = args.checkInteger(0);
		if(level == 0) tanks[2].setTankType(Fluids.STEAM);
		else if(level == 1) tanks[2].setTankType(Fluids.HOTSTEAM);
		else if(level == 2) tanks[2].setTankType(Fluids.SUPERHOTSTEAM);
		else return new Object[] {false, "Invalid level (0-2)"};
		markDirty();
		return new Object[] {true};
	}

	@Override
	public String[] getFunctionInfo() {
		return new String[] {
			PREFIX_VALUE + "coreHeat",
			PREFIX_VALUE + "hullHeat",
			PREFIX_VALUE + "water",
			PREFIX_VALUE + "coolant",
			PREFIX_VALUE + "steam",
			PREFIX_VALUE + "rods",
			PREFIX_VALUE + "targetRods",
			PREFIX_VALUE + "fuelPercent",
			PREFIX_FUNCTION + "setRodsActive" + NAME_SEPARATOR + "active",
			PREFIX_FUNCTION + "setRodsLevel" + NAME_SEPARATOR + "level",
			PREFIX_FUNCTION + "setSteamCompression" + NAME_SEPARATOR + "level"
		};
	}

	@Override
	public String provideRORValue(String name) {
		if ((PREFIX_VALUE + "coreHeat").equals(name)) return Long.toString(Math.round(coreHeat * 0.00002D * 980D + 20D));
		if ((PREFIX_VALUE + "hullHeat").equals(name)) return Long.toString(Math.round(hullHeat * 0.00001D * 980D + 20D));
		if ((PREFIX_VALUE + "water").equals(name)) return Integer.toString(tanks[0].getFill());
		if ((PREFIX_VALUE + "coolant").equals(name)) return Integer.toString(tanks[1].getFill());
		if ((PREFIX_VALUE + "steam").equals(name)) return Integer.toString(tanks[2].getFill());
		if ((PREFIX_VALUE + "rods").equals(name)) return Integer.toString(rods);
		if ((PREFIX_VALUE + "targetRods").equals(name)) return Integer.toString(rodsTarget);
		if ((PREFIX_VALUE + "fuelPercent").equals(name)) return Integer.toString(getFuelPercent());
		return null;
	}

	@Override
	public String runRORFunction(String name, String[] params) {
		if((PREFIX_FUNCTION + "setRodsActive").equals(name) && params.length > 0) {
			boolean active = params[0].equalsIgnoreCase("true") || params[0].equals("1");
			rodsTarget = active ? rodsMax : 0;
			markDirty();
			return null;
		}
		if((PREFIX_FUNCTION + "setRodsLevel").equals(name) && params.length > 0) {
			try {
				int level = Integer.parseInt(params[0]);
				if(level < 0) level = 0;
				if(level > rodsMax) level = rodsMax;
				rodsTarget = level;
				markDirty();
				return null;
			} catch (NumberFormatException e) {
				return "Invalid number";
			}
		}
		if((PREFIX_FUNCTION + "setSteamCompression").equals(name) && params.length > 0) {
			try {
				int level = Integer.parseInt(params[0]);
				if(level == 0) tanks[2].setTankType(Fluids.STEAM);
				else if(level == 1) tanks[2].setTankType(Fluids.HOTSTEAM);
				else if(level == 2) tanks[2].setTankType(Fluids.SUPERHOTSTEAM);
				else return "Invalid level (0-2)";
				markDirty();
				return null;
			} catch (NumberFormatException e) {
				return "Invalid number";
			}
		}
		return null;
	}
}
