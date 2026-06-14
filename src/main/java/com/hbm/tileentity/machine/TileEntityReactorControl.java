package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ReactorResearch;
import com.hbm.handler.CompatHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerReactorControl;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.gui.GUIReactorControl;
import com.hbm.items.ModItems;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.TileEntityDummy;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

@Optional.InterfaceList({@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")})
public class TileEntityReactorControl extends TileEntityMachineBase implements IControlReceiver, IGUIProvider, SimpleComponent, CompatHandler.OCComponent {

	public TileEntityReactorControl() {
		super(1);
	}
	
	@Override
	public String getName() {
		return "container.reactorControl";
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagList list = nbt.getTagList("items", 10);

		isLinked = nbt.getBoolean("isLinked");
		reactorType = ReactorType.values()[nbt.getInteger("reactorType")];
		linkX = nbt.getInteger("linkX");
		linkY = nbt.getInteger("linkY");
		linkZ = nbt.getInteger("linkZ");
		auto = nbt.getBoolean("auto");
		redstoned = nbt.getBoolean("redstoned");
		lastRods = nbt.getInteger("lastRods");
		levelLower = nbt.getDouble("levelLower");
		levelUpper = nbt.getDouble("levelUpper");
		heatLower = nbt.getDouble("heatLower");
		heatUpper = nbt.getDouble("heatUpper");
		function = RodFunction.values()[nbt.getInteger("function")];
		
		slots = new ItemStack[getSizeInventory()];
		for(int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound nbt1 = list.getCompoundTagAt(i);
			byte b0 = nbt1.getByte("slot");
			if(b0 >= 0 && b0 < slots.length) {
				slots[b0] = ItemStack.loadItemStackFromNBT(nbt1);
			}
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		NBTTagList list = new NBTTagList();
		
		nbt.setBoolean("isLinked", isLinked);
		nbt.setInteger("reactorType", reactorType.ordinal());
		nbt.setInteger("linkX", linkX);
		nbt.setInteger("linkY", linkY);
		nbt.setInteger("linkZ", linkZ);
		nbt.setBoolean("auto", auto);
		nbt.setBoolean("redstoned", redstoned);
		nbt.setInteger("lastRods", lastRods);
		nbt.setDouble("levelLower", levelLower);
		nbt.setDouble("levelUpper", levelUpper);
		nbt.setDouble("heatLower", heatLower);
		nbt.setDouble("heatUpper", heatUpper);
		nbt.setInteger("function", function.ordinal());
		
		for(int i = 0; i < slots.length; i++) {
			if(slots[i] != null) {
				NBTTagCompound nbt1 = new NBTTagCompound();
				nbt1.setByte("slot", (byte)i);
				slots[i].writeToNBT(nbt1);
				list.appendTag(nbt1);
			}
		}
		nbt.setTag("items", list);
	}
	
	// Research Reactor fields
	public TileEntityReactorResearch reactor;
	public boolean isLinked;
	public int flux;
	public double level;
	public int heat;
	public double levelLower;
	public double levelUpper;
	public double heatLower;
	public double heatUpper;
	public RodFunction function = RodFunction.LINEAR;
	
	// Small Reactor fields
	public int linkX, linkY, linkZ;
	public ReactorType reactorType = ReactorType.NONE;
	public int hullHeat;
	public int coreHeat;
	public int fuel;
	public int water, cool, steam;
	public int maxWater, maxCool, maxSteam;
	public int compression;
	public int rods;
	public int maxRods;
	public boolean isOn;
	public boolean auto;
	public boolean redstoned;
	private int lastRods = 100;

	public enum ReactorType {
		NONE,
		RESEARCH,
		SMALL
	}

	public enum RodFunction {
		LINEAR,
		QUAD,
		LOG
	}

	@Override
	public void updateEntity() {
		if(worldObj.isRemote) return;

		isLinked = establishLink();

		if(isLinked) {
			if(reactorType == ReactorType.RESEARCH) {
				updateResearchReactor();
			} else if(reactorType == ReactorType.SMALL) {
				updateSmallReactor();
			}
		} else {
			resetSmallReactorData();
		}

		this.networkPackNT(20);
	}

	private void updateResearchReactor() {
		TileEntity te = worldObj.getTileEntity(linkX, linkY, linkZ);
		if(!(te instanceof TileEntityReactorResearch)) {
			isLinked = false;
			return;
		}
		reactor = (TileEntityReactorResearch) te;
		this.flux = reactor.totalFlux;
		this.level = reactor.level;
		this.heat = reactor.heat;
		this.coreHeat = reactor.heat;
		this.hullHeat = 0;

		double fauxLevel = 0;
		double lowerBound = Math.min(this.heatLower, this.heatUpper);
		double upperBound = Math.max(this.heatLower, this.heatUpper);

		if(this.heat < lowerBound) {
			fauxLevel = this.levelLower;
		} else if(this.heat > upperBound) {
			fauxLevel = this.levelUpper;
		} else {
			fauxLevel = getTargetLevel(this.function, this.heat);
		}

		double newLevel = MathHelper.clamp_double(fauxLevel * 0.01D, 0D, 1D);
		if(newLevel != this.level) {
			reactor.setTarget(newLevel);
		}
	}

	private void updateSmallReactor() {
		TileEntity te = worldObj.getTileEntity(linkX, linkY, linkZ);
		if(!(te instanceof TileEntityMachineReactorSmall)) {
			isLinked = false;
			return;
		}
		TileEntityMachineReactorSmall reactor = (TileEntityMachineReactorSmall) te;

		this.hullHeat = reactor.hullHeat;
		this.coreHeat = reactor.coreHeat;
		this.fuel = reactor.getFuelPercent();
		this.water = reactor.tanks[0].getFill();
		this.cool = reactor.tanks[1].getFill();
		this.steam = reactor.tanks[2].getFill();
		this.maxWater = reactor.tanks[0].getMaxFill();
		this.maxCool = reactor.tanks[1].getMaxFill();
		this.maxSteam = reactor.tanks[2].getMaxFill();
		this.rods = reactor.rods;
		this.maxRods = reactor.rodsMax;
		this.isOn = reactor.rods > 0;

		FluidType steamType = reactor.tanks[2].getTankType();
		if(steamType == Fluids.HOTSTEAM) this.compression = 1;
		else if(steamType == Fluids.SUPERHOTSTEAM) this.compression = 2;
		else this.compression = 0;

		if(!redstoned) {
			if(worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)) {
				redstoned = true;
				reactor.rodsTarget = reactor.rodsTarget == 0 ? lastRods : 0;
				reactor.markDirty();
			}
		} else {
			if(!worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)) {
				redstoned = false;
			}
		}

		if(auto && (water < 100 || cool < 100 || coreHeat > 85000) && fuel > 0) {
			reactor.rodsTarget = 0;
			reactor.markDirty();
		}

		if(reactor.rodsTarget != 0) {
			lastRods = reactor.rodsTarget;
		}
	}

	private void resetSmallReactorData() {
		hullHeat = coreHeat = fuel = water = cool = steam = 0;
		maxWater = maxCool = maxSteam = 0;
		rods = maxRods = 0;
		isOn = false;
		compression = 0;
		flux = 0;
		level = 0;
		heat = 0;
	}
	
	private boolean establishLink() {
		if(slots[0] != null && slots[0].getItem() == ModItems.reactor_sensor && slots[0].stackTagCompound != null) {
			int x = slots[0].stackTagCompound.getInteger("x");
			int y = slots[0].stackTagCompound.getInteger("y");
			int z = slots[0].stackTagCompound.getInteger("z");
			Block b = worldObj.getBlock(x, y, z);
			TileEntity te = worldObj.getTileEntity(x, y, z);

			if(b == ModBlocks.reactor_research) {
				int[] pos = ((ReactorResearch) ModBlocks.reactor_research).findCore(worldObj, x, y, z);
				if(pos != null) {
					TileEntity core = worldObj.getTileEntity(pos[0], pos[1], pos[2]);
					if(core instanceof TileEntityReactorResearch) {
						linkX = pos[0]; linkY = pos[1]; linkZ = pos[2];
						reactorType = ReactorType.RESEARCH;
						return true;
					}
				}
			} else if(b == ModBlocks.machine_reactor_small) {
				linkX = x; linkY = y; linkZ = z;
				reactorType = ReactorType.SMALL;
				return true;
			} else if(b == ModBlocks.dummy_block_reactor_small || b == ModBlocks.dummy_port_reactor_small) {
				if(te instanceof TileEntityDummy) {
					TileEntityDummy dummy = (TileEntityDummy) te;
					linkX = dummy.targetX; linkY = dummy.targetY; linkZ = dummy.targetZ;
					reactorType = ReactorType.SMALL;
					return true;
				}
			}
		}
		reactorType = ReactorType.NONE;
		return false;
	}
	
	public double getTargetLevel(RodFunction function, int heat) {
		switch(function) {
		case LINEAR:
			return (heat - this.heatLower) * ((this.levelUpper - this.levelLower) / (this.heatUpper - this.heatLower)) + this.levelLower;
		case LOG:
			return Math.pow((heat - this.heatUpper) / (this.heatLower - this.heatUpper), 2) * (this.levelLower - this.levelUpper) + this.levelUpper;
		case QUAD:
			return Math.pow((heat - this.heatLower) / (this.heatUpper - this.heatLower), 2) * (this.levelUpper - this.levelLower) + this.levelLower;
		default: return 0.0D;
		}
	}
	
	public int[] getDisplayData() {
		if(isLinked && reactorType == ReactorType.RESEARCH) {
			return new int[] { (int)(level * 100), flux, (int)Math.round(heat * 0.00002 * 980 + 20) };
		}
		return new int[] { 0, 0, 0 };
	}
	
	@Override
	public void receiveControl(NBTTagCompound data) {
		if(reactorType == ReactorType.RESEARCH) {
			if(data.hasKey("function")) {
				this.function = RodFunction.values()[data.getInteger("function")];
			} else {
				this.levelLower = data.getDouble("levelLower");
				this.levelUpper = data.getDouble("levelUpper");
				this.heatLower = data.getDouble("heatLower");
				this.heatUpper = data.getDouble("heatUpper");
			}
		} else if(reactorType == ReactorType.SMALL) {
			TileEntity te = worldObj.getTileEntity(linkX, linkY, linkZ);
			if(te instanceof TileEntityMachineReactorSmall) {
				TileEntityMachineReactorSmall reactor = (TileEntityMachineReactorSmall) te;
				if(data.hasKey("rods")) {
					int target = data.getInteger("rods");
					reactor.rodsTarget = MathHelper.clamp_int(target, 0, reactor.rodsMax);
				}
				if(data.hasKey("active")) {
					reactor.rodsTarget = data.getBoolean("active") ? reactor.rodsMax : 0;
				}
				if(data.hasKey("compression")) {
					int c = data.getInteger("compression");
					if(c == 0) reactor.tanks[2].setTankType(Fluids.STEAM);
					else if(c == 1) reactor.tanks[2].setTankType(Fluids.HOTSTEAM);
					else reactor.tanks[2].setTankType(Fluids.SUPERHOTSTEAM);
				}
				if(data.hasKey("auto")) {
					this.auto = data.getBoolean("auto");
				}
				reactor.markDirty();
			}
		}
		this.markDirty();
	}
	
	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(isLinked);
		buf.writeInt(reactorType.ordinal());
		buf.writeInt(linkX);
		buf.writeInt(linkY);
		buf.writeInt(linkZ);
		buf.writeInt(hullHeat);
		buf.writeInt(coreHeat);
		buf.writeInt(fuel);
		buf.writeInt(water);
		buf.writeInt(cool);
		buf.writeInt(steam);
		buf.writeInt(maxWater);
		buf.writeInt(maxCool);
		buf.writeInt(maxSteam);
		buf.writeInt(compression);
		buf.writeInt(rods);
		buf.writeInt(maxRods);
		buf.writeBoolean(isOn);
		buf.writeBoolean(auto);
		buf.writeInt(flux);
		buf.writeDouble(level);
		buf.writeInt(heat);
		buf.writeDouble(levelLower);
		buf.writeDouble(levelUpper);
		buf.writeDouble(heatLower);
		buf.writeDouble(heatUpper);
		buf.writeByte(function.ordinal());
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		isLinked = buf.readBoolean();
		reactorType = ReactorType.values()[buf.readInt()];
		linkX = buf.readInt();
		linkY = buf.readInt();
		linkZ = buf.readInt();
		hullHeat = buf.readInt();
		coreHeat = buf.readInt();
		fuel = buf.readInt();
		water = buf.readInt();
		cool = buf.readInt();
		steam = buf.readInt();
		maxWater = buf.readInt();
		maxCool = buf.readInt();
		maxSteam = buf.readInt();
		compression = buf.readInt();
		rods = buf.readInt();
		maxRods = buf.readInt();
		isOn = buf.readBoolean();
		auto = buf.readBoolean();
		flux = buf.readInt();
		level = buf.readDouble();
		heat = buf.readInt();
		levelLower = buf.readDouble();
		levelUpper = buf.readDouble();
		heatLower = buf.readDouble();
		heatUpper = buf.readDouble();
		function = RodFunction.values()[buf.readByte()];
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return Vec3.createVectorHelper(xCoord - player.posX, yCoord - player.posY, zCoord - player.posZ).lengthVector() < 20;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerReactorControl(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIReactorControl(player.inventory, this);
	}

	// OpenComputers
	@Override
	@Optional.Method(modid = "OpenComputers")
	public String getComponentName() {
		return "reactor_control";
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] isLinked(Context context, Arguments args) {
		return new Object[] {isLinked};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getReactorType(Context context, Arguments args) {
		return new Object[] {reactorType.toString()};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getInfo(Context context, Arguments args) {
		if(reactorType == ReactorType.SMALL) {
			java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
			map.put("coreHeat", coreHeat);
			map.put("hullHeat", hullHeat);
			map.put("water", water);
			map.put("coolant", cool);
			map.put("steam", steam);
			map.put("rods", rods);
			map.put("maxRods", maxRods);
			map.put("fuelPercent", fuel);
			map.put("isOn", isOn);
			map.put("compression", compression);
			return new Object[] {map};
		} else if(reactorType == ReactorType.RESEARCH) {
			return new Object[] {getDisplayData()};
		}
		return new Object[] {null};
	}

	@Callback(direct = true, limit = 2)
	@Optional.Method(modid = "OpenComputers")
	public Object[] setRodsActive(Context context, Arguments args) {
		if(reactorType != ReactorType.SMALL) return new Object[] {false, "Not a Small Reactor"};
		boolean active = args.checkBoolean(0);
		NBTTagCompound data = new NBTTagCompound();
		data.setBoolean("active", active);
		receiveControl(data);
		return new Object[] {true};
	}

	@Callback(direct = true, limit = 2)
	@Optional.Method(modid = "OpenComputers")
	public Object[] setRodsLevel(Context context, Arguments args) {
		if(reactorType != ReactorType.SMALL) return new Object[] {false, "Not a Small Reactor"};
		int level = args.checkInteger(0);
		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("rods", level);
		receiveControl(data);
		return new Object[] {true};
	}

	@Callback(direct = true, limit = 2)
	@Optional.Method(modid = "OpenComputers")
	public Object[] setSteamCompression(Context context, Arguments args) {
		if(reactorType != ReactorType.SMALL) return new Object[] {false, "Not a Small Reactor"};
		int level = args.checkInteger(0);
		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("compression", level);
		receiveControl(data);
		return new Object[] {true};
	}

	@Callback(direct = true, limit = 2)
	@Optional.Method(modid = "OpenComputers")
	public Object[] setResearchParams(Context context, Arguments args) {
		if(reactorType != ReactorType.RESEARCH) return new Object[] {false, "Not a Research Reactor"};
		int newFunction = args.checkInteger(0);
		double newMaxHeat = args.checkDouble(1);
		double newMinHeat = args.checkDouble(2);
		double newMaxLevel = args.checkDouble(3)/100.0;
		double newMinLevel = args.checkDouble(4)/100.0;
		function = RodFunction.values()[MathHelper.clamp_int(newFunction, 0, 2)];
		heatUpper = MathHelper.clamp_double(newMaxHeat, 0, 9999);
		heatLower = MathHelper.clamp_double(newMinHeat, 0, 9999);
		levelUpper = MathHelper.clamp_double(newMaxLevel, 0, 1);
		levelLower = MathHelper.clamp_double(newMinLevel, 0, 1);
		markDirty();
		return new Object[] {true};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getParams(Context context, Arguments args) {
		return new Object[] {function.ordinal(), heatUpper, heatLower, levelUpper, levelLower};
	}
}
