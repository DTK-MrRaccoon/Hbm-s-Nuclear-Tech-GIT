package com.hbm.tileentity.machine.storage;

import java.util.HashSet;

import com.hbm.handler.CompatHandler; // Added for OpenComputers support
import com.hbm.inventory.container.ContainerPuF6Tank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIMachinePuF6Tank;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.IPersistentNBT;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import api.hbm.fluidmk2.FluidNode;
import api.hbm.fluidmk2.IFluidStandardTransceiverMK2;
import cpw.mods.fml.common.Optional; // Added for OpenComputers support
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.machine.Arguments; // Added for OpenComputers support
import li.cil.oc.api.machine.Callback; // Added for OpenComputers support
import li.cil.oc.api.machine.Context; // Added for OpenComputers support
import li.cil.oc.api.network.SimpleComponent; // Added for OpenComputers support
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

// Integrated OpenComputers interfaces
@Optional.InterfaceList({@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")})
public class TileEntityMachinePuF6Tank extends TileEntityMachineBase implements IFluidStandardTransceiverMK2, IPersistentNBT, IGUIProvider, IFluidCopiable, SimpleComponent, CompatHandler.OCComponent {

	protected FluidNode node;
	protected FluidType lastType;

	public FluidTank tank;
	public short mode = 1; // **CHANGED: Default mode set to 1 (Buffer/Mix)**
	public static final short modes = 4;
	public byte lastRedstone = 0;
	
	private String customName;
	
	private static final int[] slots_top = new int[] {0};
	private static final int[] slots_bottom = new int[] {1, 3};
	private static final int[] slots_side = new int[] {2};
	
	public TileEntityMachinePuF6Tank() {
		super(4);
		tank = new FluidTank(Fluids.PUF6, 64000);
		lastType = Fluids.PUF6;
	}

	@Override
	public String getName() {
		return this.hasCustomInventoryName() ? this.customName : "container.puf6_tank";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}
	
	public void setCustomName(String name) {
		this.customName = name;
	}

	public byte getComparatorPower() {
		if(tank.getFill() == 0) return 0;
		double frac = (double) tank.getFill() / (double) tank.getMaxFill() * 15D;
		return (byte) (MathHelper.clamp_int((int) frac + 1, 0, 15));
	}

	@Override
	public long getDemand(FluidType type, int pressure) {
		if(this.mode == 2 || this.mode == 3) return 0;
		if(tank.getPressure() != pressure) return 0;
		return type == tank.getTankType() ? tank.getMaxFill() - tank.getFill() : 0;
	}

	@Override
	public long transferFluid(FluidType type, int pressure, long fluid) {
		long toTransfer = Math.min(getDemand(type, pressure), fluid);
		tank.setFill(tank.getFill() + (int) toTransfer);
		return fluid - toTransfer;
	}
	
	@Override
	public void updateEntity() {

		if(!worldObj.isRemote) {

			byte comp = this.getComparatorPower();
			if(comp != this.lastRedstone) {
				this.markDirty();
				for(DirPos pos : getConPos()) this.updateRedstoneConnection(pos);
			}
			this.lastRedstone = comp;

			tank.loadTank(0, 1, slots);
			tank.unloadTank(2, 3, slots);

			// Logic for Buffer mode (mode == 1) allowing input/output/balancing to the network
			if(mode == 1) {
				if(this.node == null || this.node.expired) {
					this.node = (FluidNode) UniNodespace.getNode(worldObj, xCoord, yCoord, zCoord, tank.getTankType().getNetworkProvider());

					if(this.node == null || this.node.expired) {
						this.node = this.createNode(tank.getTankType());
						UniNodespace.createNode(worldObj, this.node);
					}
				}

				if(node != null && node.hasValidNet()) {
					node.net.addProvider(this);
					node.net.addReceiver(this);
				}
			} else {
				// Old logic for Receive (0), Provide (2), Disabled (3)
				if(this.node != null) {
					UniNodespace.destroyNode(worldObj, xCoord, yCoord, zCoord, tank.getTankType().getNetworkProvider());
					this.node = null;
				}

				for(DirPos pos : getConPos()) {
					FluidNode dirNode = (FluidNode) UniNodespace.getNode(worldObj, pos.getX(), pos.getY(), pos.getZ(), tank.getTankType().getNetworkProvider());

					if(mode == 2) {
						tryProvide(tank, worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
					} else {
						if(dirNode != null && dirNode.hasValidNet()) dirNode.net.removeProvider(this);
					}

					if(mode == 0) {
						if(dirNode != null && dirNode.hasValidNet()) dirNode.net.addReceiver(this);
					} else {
						if(dirNode != null && dirNode.hasValidNet()) dirNode.net.removeReceiver(this);
					}
				}
			}

			this.networkPackNT(50);
		}
	}

	protected FluidNode createNode(FluidType type) {
		DirPos[] conPos = getConPos();

		HashSet<BlockPos> posSet = new HashSet<>();
		posSet.add(new BlockPos(this));
		for(DirPos pos : conPos) {
			ForgeDirection dir = pos.getDir();
			posSet.add(new BlockPos(pos.getX() - dir.offsetX, pos.getY() - dir.offsetY, pos.getZ() - dir.offsetZ));
		}

		return new FluidNode(type.getNetworkProvider(), posSet.toArray(new BlockPos[posSet.size()])).setConnections(conPos);
	}

	@Override
	public void invalidate() {
		super.invalidate();

		if(!worldObj.isRemote) {
			if(this.node != null) {
				UniNodespace.destroyNode(worldObj, xCoord, yCoord, zCoord, tank.getTankType().getNetworkProvider());
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeShort(mode);
		tank.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		mode = buf.readShort();
		tank.deserialize(buf);
	}

	// This method checks all 6 directions (POS_X/NEG_X/POS_Y/NEG_Y/POS_Z/NEG_Z), 
	// ensuring connection to Top (POS_Y), Bottom (NEG_Y), and sides is possible.
	protected DirPos[] getConPos() {
		return new DirPos[] {
				new DirPos(xCoord + 1, yCoord, zCoord, Library.POS_X),
				new DirPos(xCoord - 1, yCoord, zCoord, Library.NEG_X),
				new DirPos(xCoord, yCoord + 1, zCoord, Library.POS_Y),
				new DirPos(xCoord, yCoord - 1, zCoord, Library.NEG_Y),
				new DirPos(xCoord, yCoord, zCoord + 1, Library.POS_Z),
				new DirPos(xCoord, yCoord, zCoord - 1, Library.NEG_Z)
		};
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		if(i == 0 && stack.getItem() == ModItems.cell_puf6)
			return true;
		if(i == 2 && stack.getItem() == ModItems.cell_empty)
			return true;
		
		return false;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int j) {
		return i == 1 || i == 3;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 ? slots_bottom : (side == 1 ? slots_top : slots_side);
        }
	
	@Override
	public boolean canInsertItem(int i, ItemStack itemStack, int j) {
		return this.isItemValidForSlot(i, itemStack);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (nbt.hasKey("CustomName", 8)) {
            this.customName = nbt.getString("CustomName");
        }
		mode = nbt.getShort("mode");
		tank.readFromNBT(nbt, "tank");
		tank.setTankType(Fluids.PUF6);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if (this.hasCustomInventoryName()) {
            nbt.setString("CustomName", this.customName);
        }
		nbt.setShort("mode", mode);
		tank.writeToNBT(nbt, "tank");
	}
	
	@Override public boolean canConnect(FluidType fluid, ForgeDirection dir) {
		return fluid == Fluids.PUF6;
	}

	@Override
	public FluidTank[] getSendingTanks() {
		return (mode == 1 || mode == 2) ? new FluidTank[] {tank} : new FluidTank[0];
	}

	@Override
	public FluidTank[] getReceivingTanks() {
		return (mode == 0 || mode == 1) ? new FluidTank[] {tank} : new FluidTank[0];
	}

	@Override
	public FluidTank[] getAllTanks() {
		return new FluidTank[] { tank };
	}

	@Override
	public ConnectionPriority getFluidPriority() {
		// Low priority for buffer mode to allow balancing
		return mode == 1 ? ConnectionPriority.LOW : ConnectionPriority.NORMAL;
	}

	@Override
	public int[] getFluidIDToCopy() {
		return new int[] {tank.getTankType().getID()};
	}

	@Override
	public FluidTank getTankToPaste() {
		return tank;
	}

	@Override
	public void writeNBT(NBTTagCompound nbt) {
		if(tank.getFill() == 0) return;
		NBTTagCompound data = new NBTTagCompound();
		this.tank.writeToNBT(data, "tank");
		data.setShort("mode", mode);
		nbt.setTag(NBT_PERSISTENT_KEY, data);
	}

	@Override
	public void readNBT(NBTTagCompound nbt) {
		NBTTagCompound data = nbt.getCompoundTag(NBT_PERSISTENT_KEY);
		this.tank.readFromNBT(data, "tank");
		this.mode = data.getShort("mode");
		this.tank.setTankType(Fluids.PUF6);
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerPuF6Tank(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachinePuF6Tank(player.inventory, this);
	}
	
	// OpenComputers Implementation (only max capacity and liquid amount)
	@Override
	@Optional.Method(modid = "OpenComputers")
	public String getComponentName() {
		return "ntm_puf6_tank";
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getFluidStored(Context context, Arguments args) {
		return new Object[] {tank.getFill()};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getMaxStored(Context context, Arguments args) {
		return new Object[] {tank.getMaxFill()};
	}

	@Callback(direct = true)
	@Optional.Method(modid = "OpenComputers")
	public Object[] getInfo(Context context, Arguments args) {
		return new Object[]{tank.getFill(), tank.getMaxFill()};
	}

	@Override
	@Optional.Method(modid = "OpenComputers")
	public String[] methods() {
		return new String[] {
				"getFluidStored",
				"getMaxStored",
				"getInfo"
		};
	}

	@Override
	@Optional.Method(modid = "OpenComputers")
	public Object[] invoke(String method, Context context, Arguments args) throws Exception {
		switch (method) {
			case "getFluidStored": return getFluidStored(context, args);
			case "getMaxStored": return getMaxStored(context, args);
			case "getInfo": return getInfo(context, args);
		}
		throw new NoSuchMethodException();
	}
}