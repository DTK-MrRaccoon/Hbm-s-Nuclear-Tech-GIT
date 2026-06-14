package com.hbm.tileentity.machine;

import java.util.HashMap;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.MobConfig;
import com.hbm.handler.CompatHandler;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.container.ContainerMachineKrusty;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIMachineKrusty;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.BufferUtil;
import com.hbm.util.CompatEnergyControl;

import api.hbm.energymk2.IEnergyProviderMK2;
import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.fluidmk2.IFluidStandardTransceiverMK2;
import api.hbm.redstoneoverradio.IRORInteractive;
import api.hbm.redstoneoverradio.IRORValueProvider;
import api.hbm.tile.IInfoProviderEC;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

@Optional.InterfaceList({@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")})
public class TileEntityMachineKrusty extends TileEntityMachineBase implements IControlReceiver, SimpleComponent, IGUIProvider, IInfoProviderEC, CompatHandler.OCComponent, IFluidCopiable, IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive, IEnergyProviderMK2 {
	public double targetLevel;
	public double level;
	public double lastLevel;
	public double speed = 0.5D;
	
	public double totalFlux = 0;
	public double[] slotFlux = new double[4];
	public int heat;
	public final int maxHeat = 50000;
	public int heatRemoved;
	
	public static long maxPower = 5_000_000;
	public long power;
	
	public FluidTank[] tanks;
	
	private static final int[] slot_io = new int[] { 0, 1, 2, 3 };
	
	public TileEntityMachineKrusty() {
		super(6);
		this.tanks = new FluidTank[1];
		this.tanks[0] = new FluidTank(Fluids.SODIUM, 16_000);
		this.tanks[0].setFill(0);
	}
	
	private static final HashMap<ComparableStack, ItemStack> fuelMap = new HashMap<ComparableStack, ItemStack>();
	static {
		fuelMap.put(new ComparableStack(ModItems.plate_fuel_u233), new ItemStack(ModItems.waste_plate_u233, 1, 1));
		fuelMap.put(new ComparableStack(ModItems.plate_fuel_u235), new ItemStack(ModItems.waste_plate_u235, 1, 1));
		fuelMap.put(new ComparableStack(ModItems.plate_fuel_mox), new ItemStack(ModItems.waste_plate_mox, 1, 1));
		fuelMap.put(new ComparableStack(ModItems.plate_fuel_pu239), new ItemStack(ModItems.waste_plate_pu239, 1, 1));
		fuelMap.put(new ComparableStack(ModItems.plate_fuel_sa326), new ItemStack(ModItems.waste_plate_sa326, 1, 1));
		fuelMap.put(new ComparableStack(ModItems.plate_fuel_ra226be), new ItemStack(ModItems.waste_plate_ra226be, 1, 1));
		fuelMap.put(new ComparableStack(ModItems.plate_fuel_pu238be), new ItemStack(ModItems.waste_plate_pu238be, 1, 1));
	}
	
	public String getName() {
		return "container.machineKrusty";
	}
	
	@Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        if(i >= 0 && i < 4) {
            if(itemStack.getItem() instanceof ItemPlateFuel)
                return true;
        }
        if(i == 4) return FluidContainerRegistry.getFluidContent(itemStack, tanks[0].getTankType()) > 0;
        return false;
    }
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		heat = nbt.getInteger("heat");
		level = nbt.getDouble("level");
		targetLevel = nbt.getDouble("targetLevel");
		totalFlux = nbt.getDouble("totalFlux");
		for(int i = 0; i < slotFlux.length; i++) slotFlux[i] = nbt.getDouble("slotFlux_" + i);
		for(int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
		this.power = nbt.getLong("power");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("heat", heat);
		nbt.setDouble("level", level);
		nbt.setDouble("targetLevel", targetLevel);
		nbt.setDouble("totalFlux", totalFlux);
		for(int i = 0; i < slotFlux.length; i++) nbt.setDouble("slotFlux_" + i, slotFlux[i]);
		for(int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
		nbt.setLong("power", power);
	}
	
	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return slot_io;
	}
	
	@Override
	public boolean canExtractItem(int i, ItemStack stack, int j) {
		if(i < 4 && i >= 0)
			if(fuelMap.containsValue(stack))
				return true;
		return false;
	}
	
	@Override
	public void updateEntity() {
		
		rodControl();
		
		if(!worldObj.isRemote) {
			totalFlux = 0;
			
			this.tanks[0].loadTank(4, 5, slots);
			
			if(level > 0) {
				reaction();
			}
			
			if (heat > 0) {
				heatRemoved = (int) (heat * 0.4D * this.tanks[0].getFill() / this.tanks[0].getMaxFill());
				heat = heatRemoved < heat ? heat - heatRemoved : 0;
				
				this.power += heatRemoved;
				
				if (power > maxPower)
					power = maxPower;
			}
			
			this.sendPower();
			
			this.networkPackNT(150);
		}
	}
	
	private void sendPower() {
		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			TileEntity te = worldObj.getTileEntity(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
			if(te instanceof IEnergyReceiverMK2) {
				IEnergyReceiverMK2 rec = (IEnergyReceiverMK2) te;
				long toSend = Math.min(power, rec.getMaxPower() - rec.getPower());
				if(toSend > 0) {
					rec.setPower(rec.getPower() + toSend);
					this.power -= toSend;
					if(power <= 0) break;
				}
			}
		}
	}
	
	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(this.heat);
		buf.writeDouble(this.level);
		buf.writeDouble(this.targetLevel);
		for(int i = 0; i < slotFlux.length; i++) buf.writeDouble(this.slotFlux[i]);
		buf.writeDouble(this.totalFlux);
		for(FluidTank tank : tanks) {
			tank.serialize(buf);
		}
		buf.writeLong(this.power);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.heat = buf.readInt();
		this.level = buf.readDouble();
		this.targetLevel = buf.readDouble();
		for(int i = 0; i < slotFlux.length; i++) this.slotFlux[i] = buf.readDouble();
		this.totalFlux = buf.readDouble();
		for(FluidTank tank : tanks) {
			tank.deserialize(buf);
		}
		this.power = buf.readLong();
	}
	
	private int[] getNeighboringSlots(int id) {
		switch(id) {
		case 0:
			return new int[] { 1, 2, 3 };
		case 1:
			return new int[] { 0, 2, 3 };
		case 2:
			return new int[] { 0, 1, 3 };
		case 3:
			return new int[] { 0, 1, 2 };
		}
		return null;
	}
	
	private void reaction() {
		for(byte i = 0; i < 4; i++) {
			if(slots[i] == null) {
				slotFlux[i] = 0;
				continue;
			}
			
			if(slots[i].getItem() instanceof ItemPlateFuel) {
				ItemPlateFuel rod = (ItemPlateFuel) slots[i].getItem();
				
				double outFlux = rod.react(worldObj, slots[i], (int)slotFlux[i]);
				this.heat += outFlux * 2;
				slotFlux[i] = 0;
				totalFlux += outFlux;
				
				int[] neighborSlots = getNeighboringSlots(i);
				
				if(ItemPlateFuel.getLifeTime(slots[i]) > rod.lifeTime) {
					slots[i] = fuelMap.get(new ComparableStack(slots[i])).copy();
				}
				
				for(byte j = 0; j < neighborSlots.length; j++) {
					slotFlux[neighborSlots[j]] += outFlux * level;
				}
				continue;
			}
			slotFlux[i] = 0;
		}
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return Vec3.createVectorHelper(xCoord - player.posX, yCoord - player.posY, zCoord - player.posZ).lengthVector() < 20;
	}
	
	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("level")) {
			this.setTarget(data.getDouble("level"));
		}
		this.markDirty();
	}
	
	public void setTarget(double target) {
		this.targetLevel = target;
	}
	
	public void rodControl() {
		if(worldObj.isRemote) {
			this.lastLevel = this.level;
		} else {
			if(level < targetLevel) {
				level += speed;
				if(level >= targetLevel)
					level = targetLevel;
			}
			if(level > targetLevel) {
				level -= speed;
				if(level <= targetLevel)
					level = targetLevel;
			}
		}
	}
	
	public int[] getDisplayData() {
		int[] data = new int[2];
		data[0] = (int) this.totalFlux;
		data[1] = (int) Math.round((this.heat) * 0.00002 * 980 + 20);
		return data;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
	
	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineKrusty(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineKrusty(player.inventory, this);
	}
	
	@Override
	public FluidTank[] getAllTanks() {
		return tanks;
	}
	
	@Override
	public FluidTank[] getSendingTanks() {
		return null;
	}
	
	@Override
	public FluidTank[] getReceivingTanks() {
		return tanks;
	}

	@Override
	public void provideExtraInfo(NBTTagCompound data) {
		data.setDouble(CompatEnergyControl.D_HEAT_C, Math.round(heat * 2.0E-5D * 980.0D + 20.0D));
		data.setInteger(CompatEnergyControl.I_FLUX, (int)totalFlux);
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public String getComponentName() {
		return "ntm_krusty";
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getHeat(Context context, Arguments args) {
		return new Object[] {heat, maxHeat};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getFlux(Context context, Arguments args) {
		return new Object[] {totalFlux};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getLevel(Context context, Arguments args) {
		return new Object[] {level, targetLevel};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getCoolantInfo(Context context, Arguments args) {
		return new Object[] {tanks[0].getFill(), tanks[0].getMaxFill()};
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getPower(Context context, Arguments args) {
		return new Object[] {power, maxPower};
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getInfo(Context context, Arguments args) {
		return new Object[] {heat, maxHeat, totalFlux, level, targetLevel, tanks[0].getFill(), tanks[0].getMaxFill(), power, maxPower};
	}

	@Callback(direct = true, limit = 4)
	@Optional.Method(modid = "OpenComputers")
	public Object[] setLevel(Context context, Arguments args) {
		targetLevel = MathHelper.clamp_double(args.checkDouble(0), 0.0D, 1.0D);
		this.markChanged();
		return new Object[] {true};
	}

	public static final String[] ROR = new String[] {
		PREFIX_VALUE + "heat",
		PREFIX_VALUE + "flux",
		PREFIX_VALUE + "coolant",
		PREFIX_VALUE + "rods",
		PREFIX_VALUE + "power",
		PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
	};

	@Override
	public String[] getFunctionInfo() {
		return ROR;
	}

	@Override
	public String runRORFunction(String name, String[] params) {
		if((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
			int percent = IRORInteractive.parseInt(params[0], 0, 100);
			this.targetLevel = percent / 100.0D;
			this.markChanged();
			return null;
		}
		return null;
	}

	@Override
	public String provideRORValue(String name) {
		if((PREFIX_VALUE + "heat").equals(name))			return "" + this.heat;
		if((PREFIX_VALUE + "flux").equals(name))			return "" + (int)this.totalFlux;
		if((PREFIX_VALUE + "coolant").equals(name))			return "" + this.tanks[0].getFill();
		if((PREFIX_VALUE + "rods").equals(name))			return "" + this.level * 100;
		if((PREFIX_VALUE + "power").equals(name))			return "" + this.power;
		return null;
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
}