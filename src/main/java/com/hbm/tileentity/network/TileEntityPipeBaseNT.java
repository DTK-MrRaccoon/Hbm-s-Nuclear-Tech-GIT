package com.hbm.tileentity.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.IBlockFluidDuct;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.HbmKeybinds;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.main.MainRegistry;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.items.ModItems;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IRepairable;
import com.hbm.tileentity.IRepairable.EnumExtinguishType;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.uninos.UniNodespace;
import com.hbm.util.InventoryUtil;
import com.hbm.util.ParticleUtil;

import api.hbm.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import api.hbm.fluidmk2.FluidNetMK2;
import api.hbm.fluidmk2.FluidNode;
import api.hbm.fluidmk2.IFluidPipeMK2;
import api.hbm.fluidmk2.IFluidReceiverMK2;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityPipeBaseNT extends TileEntityLoadedBase implements IFluidPipeMK2, IFluidReceiverMK2, IFluidCopiable, IRepairable {

	protected FluidNode node;
	protected FluidType type = Fluids.NONE;
	protected FluidType lastType = Fluids.NONE;
	protected boolean damaged = false;

	@Override
	public void updateEntity() {

		if(worldObj.isRemote && lastType != type) {
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			lastType = type;
		}

		if(!worldObj.isRemote) {

			if(this.node == null || this.node.expired) {

				if(this.shouldCreateNode()) {
					this.node = (FluidNode) UniNodespace.getNode(worldObj, xCoord, yCoord, zCoord, type.getNetworkProvider());

					if(this.node == null || this.node.expired) {
						this.node = this.createNode(type);
						UniNodespace.createNode(worldObj, this.node);
					}
				}
			}

			if(this.damaged && this.node != null && this.node.net != null && this.type != Fluids.NONE) {
				this.node.net.addReceiver(this);
			}
		}
	}

	public boolean shouldCreateNode() {
		return true;
	}

	public FluidType getType() {
		return this.type;
	}

	public void setType(FluidType type) {
		FluidType prev = this.type;
		this.type = type;
		this.markDirty();

		if(worldObj instanceof WorldServer) {
			WorldServer world = (WorldServer) worldObj;
			world.getPlayerManager().markBlockForUpdate(xCoord, yCoord, zCoord);
		}
		
		UniNodespace.destroyNode(worldObj, xCoord, yCoord, zCoord, prev.getNetworkProvider());

		if(this.node != null) {
			this.node = null;
		}
	}

	public boolean isDamaged() {
		return this.damaged;
	}

	public void damagePipe() {
		if(this.damaged) return;
		this.damaged = true;
		this.markDirty();
		if(!this.worldObj.isRemote) {
			this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}

	public void repairPipe() {
		if(!this.damaged) return;
		this.damaged = false;
		this.markDirty();
		if(!this.worldObj.isRemote) {
			this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}

	public List<AStack> getRepairMaterialsList() {
		if(this.worldObj != null) {
			return this.getRepairMaterialsList(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
		}
		List<AStack> list = new ArrayList();
		list.add(new ComparableStack(ModItems.plate_aluminium));
		return list;
	}

	public List<AStack> getRepairMaterialsList(World world, int x, int y, int z) {
		List<AStack> list = new ArrayList();
		if(world == null) {
			list.add(new ComparableStack(ModItems.plate_aluminium));
			return list;
		}
		Block block = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		if(block == ModBlocks.fluid_duct_box) {
			switch(Math.abs(meta % 3)) {
			case 0:
				list.add(new ComparableStack(ModItems.plate_iron));
				break;
			case 1:
				list.add(new ComparableStack(ModItems.plate_copper));
				break;
			default:
				list.add(new ComparableStack(ModItems.plate_aluminium));
				break;
			}
			return list;
		}
		if(block == ModBlocks.fluid_duct_neo) {
			if(Math.abs(meta % 3) == 1) {
				list.add(new ComparableStack(ModItems.plate_iron));
			} else {
				list.add(new ComparableStack(ModItems.plate_steel));
			}
			list.add(new ComparableStack(ModItems.plate_aluminium));
			return list;
		}
		if(block == ModBlocks.fluid_duct_paintable) {
			list.add(new ComparableStack(ModItems.plate_steel));
			list.add(new ComparableStack(ModItems.plate_aluminium));
			return list;
		}
		if(block == ModBlocks.fluid_duct_gauge || block == ModBlocks.fluid_valve || block == ModBlocks.fluid_switch || block == ModBlocks.fluid_counter_valve) {
			list.add(new ComparableStack(ModItems.plate_steel));
			return list;
		}
		list.add(new ComparableStack(ModItems.plate_aluminium));
		return list;
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN && type == this.type;
	}

	@Override
	public void invalidate() {
		super.invalidate();

		if(!worldObj.isRemote) {
			if(this.node != null) {
				UniNodespace.destroyNode(worldObj, xCoord, yCoord, zCoord, type.getNetworkProvider());
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(this.damaged);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.damaged = buf.readBoolean();
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
		this.type = Fluids.fromID(nbt.getInteger("type"));
		this.damaged = nbt.getBoolean("damaged");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("type", this.type.getID());
		nbt.setBoolean("damaged", this.damaged);
	}

	public boolean isLoaded = true;

	@Override
	public boolean isLoaded() {
		return isLoaded;
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		this.isLoaded = false;
	}

	@Override
	public long transferFluid(FluidType type, int pressure, long amount) {
		if(!this.damaged || amount <= 0 || this.type == Fluids.NONE || type != this.type) return amount;
		long used = Math.min(amount, 2L);
		this.leakFluid(used);
		return amount - used;
	}

	@Override
	public long getDemand(FluidType type, int pressure) {
		if(!this.damaged || this.type == Fluids.NONE || type != this.type) return 0;
		return 2L;
	}

	@Override
	public ConnectionPriority getFluidPriority() {
		return ConnectionPriority.LOWEST;
	}

	@Override
	public FluidTank[] getAllTanks() {
		return new FluidTank[0];
	}

	private void leakFluid(long amount) {
		if(amount <= 0 || this.type == Fluids.NONE) return;

		if(this.type.hasTrait(FT_Flammable.class)) {
			ParticleUtil.spawnGasFlame(this.worldObj, this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5, 0, 0.02, 0);
		}

		NBTTagCompound data = new NBTTagCompound();
		if(this.type.hasTrait(FT_Gaseous.class)) {
			data.setString("type", "tower");
			data.setFloat("lift", 0.5F);
			data.setFloat("base", 0.375F);
			data.setFloat("max", 3F);
			data.setInteger("life", 100 + this.worldObj.rand.nextInt(50));
		} else {
			data.setString("type", "splash");
		}
		data.setInteger("color", this.type.getColor());
		data.setDouble("posX", this.xCoord + 0.5 + (this.worldObj.rand.nextFloat() - 0.5F) * 0.5F);
		data.setDouble("posY", this.yCoord + 0.5);
		data.setDouble("posZ", this.zCoord + 0.5 + (this.worldObj.rand.nextFloat() - 0.5F) * 0.5F);
		PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, data.getDouble("posX"), data.getDouble("posY"), data.getDouble("posZ")), new TargetPoint(this.worldObj.provider.dimensionId, data.getDouble("posX"), data.getDouble("posY"), data.getDouble("posZ"), 48));

		if(this.type.hasTrait(FT_Polluting.class)) {
			FT_Polluting.pollute(this.worldObj, this.xCoord, this.yCoord, this.zCoord, this.type, this.type.hasTrait(FT_Flammable.class) ? FluidReleaseType.BURN : FluidReleaseType.SPILL, (float) amount);
		}
	}

	@Override
	public int[] getFluidIDToCopy() {
		return new int[]{ type.getID() };
	}

	@Override
	public FluidTank getTankToPaste() {
		return null;
	}

	@Override
	public void pasteSettings(NBTTagCompound nbt, int index, World world, EntityPlayer player, int x, int y, int z) {
		int[] ids = nbt.getIntArray("fluidID");
		if(ids.length > 0) {
			int id;
			if(index < ids.length) {
				id = ids[index];
			} else {
				id = 0;
			}

			FluidType fluid = Fluids.fromID(id);

			if(HbmPlayerProps.getData(player).getKeyPressed(HbmKeybinds.EnumKeybind.TOOL_CTRL)) {
				IBlockFluidDuct pipe = (IBlockFluidDuct) world.getBlock(x, y, z);
				pipe.changeTypeRecursively(world, x, y, z, getType(), fluid, 64);
			} else {
				this.setType(fluid);
			}
		}
	}

	@Override
	public List<AStack> getRepairMaterials() {
		return this.getRepairMaterialsList();
	}


	@Override
	public void tryExtinguish(World world, int x, int y, int z, EnumExtinguishType type) {
	}
	@Override
	public void repair() {
		this.repairPipe();
	}
}
