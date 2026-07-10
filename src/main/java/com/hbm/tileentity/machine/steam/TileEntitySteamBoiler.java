package com.hbm.tileentity.machine.steam;

import com.hbm.handler.pollution.PollutionHandler.PollutionType;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.container.ContainerSteamBoiler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUISteamBoiler;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModItems;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.hbm.util.ItemStackUtil;

import api.hbm.fluid.IFluidStandardTransceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;

public class TileEntitySteamBoiler extends TileEntityMachinePolluting implements IFluidStandardTransceiver, IGUIProvider {

	public final FluidTank water;
	public final FluidTank steam;

	public int burnTime;
	public int maxBurnTime;
	public int heat;
	public int maxHeatCap = 500;
	public double waterFractionalBuffer = 0.0;

	public int ashLevelWood;
	public int ashLevelCoal;
	public int ashLevelMisc;

	public boolean bronze = false;

	protected ForgeDirection frontDirection = ForgeDirection.NORTH;
	private boolean wasActiveLastTick = false;

	public TileEntitySteamBoiler() {
		this(false);
	}

	public TileEntitySteamBoiler(boolean bronze) {
		super(4, 50);
		this.bronze = bronze;
		this.water = new FluidTank(Fluids.WATER, 1000);
		this.steam = new FluidTank(Fluids.STEAM, 10000);
	}

	public boolean isBronze() {
		return this.bronze;
	}

	protected double getSteamProductionMultiplier() {
		return this.bronze ? 0.5D : 1.0D;
	}

	@Override
	public String getName() {
		return this.bronze ? "container.steamBoilerSmallBronze" : "container.steamBoilerSmall";
	}

	public ForgeDirection getFrontDirection() {
		return frontDirection != null ? frontDirection : ForgeDirection.NORTH;
	}

	public void setFrontDirection(ForgeDirection direction) {
		this.frontDirection = direction != null ? direction : ForgeDirection.NORTH;
		this.markDirty();
	}

	private void subscribeWater() {
		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			this.trySubscribe(water.getTankType(), worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
		}
	}

	private void sendSteam() {
		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			this.sendFluid(steam, worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
		}
	}

	private void startBurning() {
		if(slots[0] == null) return;

		int fuel = TileEntityFurnace.getItemBurnTime(slots[0]);
		if(fuel <= 0) return;

		this.maxBurnTime = this.burnTime = fuel * (this.bronze ? 3 : 7);

		EnumAshType type = TileEntityFireboxBase.getAshFromFuel(slots[0]);
		if(type == EnumAshType.WOOD) ashLevelWood += fuel;
		else if(type == EnumAshType.COAL) ashLevelCoal += fuel;
		else ashLevelMisc += fuel;

		this.maxHeatCap = 100;

		if (slots[0].getItem() == ModItems.solid_fuel) {
			this.maxHeatCap = 500;
		} else {
			List<String> names = ItemStackUtil.getOreDictNames(slots[0]);
			for(String name : names) {
				if(name.contains("Coke")) {
					this.maxHeatCap = 350;
					break;
				} else if(name.contains("Coal") || name.contains("Lignite")) {
					this.maxHeatCap = 250;
					break;
				}
			}
		}

		int threshold = 2000;
		while(processAsh(ashLevelWood, EnumAshType.WOOD, threshold)) ashLevelWood -= threshold;
		while(processAsh(ashLevelCoal, EnumAshType.COAL, threshold)) ashLevelCoal -= threshold;
		while(processAsh(ashLevelMisc, EnumAshType.MISC, threshold)) ashLevelMisc -= threshold;

		ItemStack container = slots[0].getItem().getContainerItem(slots[0]);
		this.decrStackSize(0, 1);
		if(slots[0] == null && container != null) {
			slots[0] = container.copy();
		}
		this.markDirty();
	}

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			boolean burning = burnTime > 0;
			if(!burning) {
				startBurning();
				burning = burnTime > 0;
			}

			if(burning) {
				burnTime--;
				if(this.worldObj.getTotalWorldTime() % 4 == 0) {
					if(heat < maxHeatCap) heat++;
					else if(heat > maxHeatCap) heat--;
				}
				if(worldObj.getTotalWorldTime() % 40 == 0) {
					this.pollute(PollutionType.SOOT, 0.125F);
				}
			} else if(heat > 0 && this.worldObj.getTotalWorldTime() % 4 == 0) {
				heat--;
			}

			if(heat >= 100) {
				int room = steam.getMaxFill() - steam.getFill();
				if(room > 0 && water.getFill() > 0) {
					int steamProduction = 10;
					if(heat >= 500) steamProduction = 100;
					else if(heat >= 350) steamProduction = 75 + (heat - 350) * 25 / 150;
					else if(heat >= 250) steamProduction = 50 + (heat - 250) * 25 / 100;
					else steamProduction = 10 + (heat - 100) * 40 / 150;

					steamProduction = Math.max(1, (int)Math.round(steamProduction * this.getSteamProductionMultiplier()));
					int process = Math.min(steamProduction, room);

					if(process > 0) {
						waterFractionalBuffer += (process / 100.0);
						steam.setFill(steam.getFill() + process);

						if(waterFractionalBuffer >= 1.0) {
							int waterToDeduct = (int) Math.floor(waterFractionalBuffer);
							if(waterToDeduct > water.getFill()) {
								waterToDeduct = water.getFill();
							}
							water.setFill(water.getFill() - waterToDeduct);
							waterFractionalBuffer -= waterToDeduct;
						}
					}
				} else {
					waterFractionalBuffer = 0.0;
				}
			} else {
				waterFractionalBuffer = 0.0;
			}

			if(heat < 0) heat = 0;
			if(heat > 500) heat = 500;

			if(burning && worldObj.getTotalWorldTime() % 4 == 0) {
				int threshold = (int)(smoke.getMaxFill() * 0.90);
				if(smoke.getFill() > threshold) {
					((net.minecraft.world.WorldServer)worldObj).func_147487_a("smoke", xCoord + 0.5, yCoord + 1.1, zCoord + 0.5, 1, 0.0, 0.05, 0.0, 0.0);
				}
			}

			for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
				this.sendSmoke(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
			}

			if(this.worldObj.getTotalWorldTime() % 20 == 0) {
				this.subscribeWater();
			}

			if(water.loadTank(2, 3, slots)) {
				this.markDirty();
			}

			this.sendSteam();

			boolean isActiveNow = burnTime > 0;
			if(isActiveNow != wasActiveLastTick) {
				wasActiveLastTick = isActiveNow;
				this.markDirty();
				this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			}

			this.networkPackNT(50);
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
		this.worldObj.markBlockRangeForRenderUpdate(this.xCoord, this.yCoord, this.zCoord, this.xCoord, this.yCoord, this.zCoord);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(burnTime);
		buf.writeInt(maxBurnTime);
		buf.writeInt(heat);
		buf.writeInt(maxHeatCap);
		buf.writeInt(ashLevelWood);
		buf.writeInt(ashLevelCoal);
		buf.writeInt(ashLevelMisc);
		buf.writeBoolean(this.bronze);
		buf.writeByte(this.getFrontDirection().ordinal());
		water.serialize(buf);
		steam.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		burnTime = buf.readInt();
		maxBurnTime = buf.readInt();
		heat = buf.readInt();
		maxHeatCap = buf.readInt();
		ashLevelWood = buf.readInt();
		ashLevelCoal = buf.readInt();
		ashLevelMisc = buf.readInt();
		bronze = buf.readBoolean();
		frontDirection = readFacing(buf.readByte());
		water.deserialize(buf);
		steam.deserialize(buf);
	}

	private ForgeDirection readFacing(byte ordinal) {
		ForgeDirection[] directions = ForgeDirection.values();
		if(ordinal < 0 || ordinal >= directions.length) return ForgeDirection.NORTH;
		ForgeDirection dir = directions[ordinal];
		return (dir == ForgeDirection.NORTH || dir == ForgeDirection.EAST || dir == ForgeDirection.SOUTH || dir == ForgeDirection.WEST) ? dir : ForgeDirection.NORTH;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		burnTime = nbt.getInteger("burnTime");
		maxBurnTime = nbt.getInteger("maxBurnTime");
		heat = nbt.getInteger("heat");
		maxHeatCap = nbt.getInteger("maxHeatCap");
		if(maxHeatCap <= 0) maxHeatCap = 100;
		ashLevelWood = nbt.getInteger("ashLevelWood");
		ashLevelCoal = nbt.getInteger("ashLevelCoal");
		ashLevelMisc = nbt.getInteger("ashLevelMisc");
		bronze = nbt.getBoolean("bronze");
		frontDirection = readFacing(nbt.getByte("frontDir"));
		water.readFromNBT(nbt, "water");
		steam.readFromNBT(nbt, "steam");
		wasActiveLastTick = burnTime > 0;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("burnTime", burnTime);
		nbt.setInteger("maxBurnTime", maxBurnTime);
		nbt.setInteger("heat", heat);
		nbt.setInteger("maxHeatCap", maxHeatCap);
		nbt.setInteger("ashLevelWood", ashLevelWood);
		nbt.setInteger("ashLevelCoal", ashLevelCoal);
		nbt.setInteger("ashLevelMisc", ashLevelMisc);
		nbt.setByte("frontDir", (byte)this.getFrontDirection().ordinal());
		nbt.setBoolean("bronze", bronze);
		water.writeToNBT(nbt, "water");
		steam.writeToNBT(nbt, "steam");
	}

	protected boolean processAsh(int level, EnumAshType type, int threshold) {
		if(level >= threshold) {
			if(slots[1] == null) {
				slots[1] = DictFrame.fromOne(ModItems.powder_ash, type);
				return true;
			} else if(slots[1].stackSize < slots[1].getMaxStackSize() && slots[1].getItem() == ModItems.powder_ash && slots[1].getItemDamage() == type.ordinal()) {
				slots[1].stackSize++;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if(stack == null) return false;
		if(slot == 0) return TileEntityFurnace.getItemBurnTime(stack) > 0;
		if(slot == 2) return FluidContainerRegistry.getFluidContent(stack, water.getTankType()) > 0;
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		ForgeDirection dir = ForgeDirection.getOrientation(side);
		if(dir == ForgeDirection.DOWN) return new int[] { 1, 3 };
		if(dir == ForgeDirection.UP) return new int[] { 0 };
		return new int[] { 2 };
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return slot == 1 || slot == 3;
	}

	@Override
	public FluidTank[] getReceivingTanks() {
		return new FluidTank[] { water };
	}

	@Override
	public FluidTank[] getSendingTanks() {
		FluidTank[] smokeTanks = this.getSmokeTanks();
		FluidTank[] sending = new FluidTank[1 + smokeTanks.length];
		sending[0] = steam;
		System.arraycopy(smokeTanks, 0, sending, 1, smokeTanks.length);
		return sending;
	}

	@Override
	public FluidTank[] getAllTanks() {
		FluidTank[] smokeTanks = this.getSmokeTanks();
		FluidTank[] all = new FluidTank[2 + smokeTanks.length];
		all[0] = water;
		all[1] = steam;
		System.arraycopy(smokeTanks, 0, all, 2, smokeTanks.length);
		return all;
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, net.minecraft.world.World world, int x, int y, int z) {
		return new ContainerSteamBoiler(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, net.minecraft.world.World world, int x, int y, int z) {
		return new GUISteamBoiler(player.inventory, this);
	}

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) {
			bb = AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord, zCoord - 1, xCoord + 2, yCoord + 1, zCoord + 2);
		}
		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
