package com.hbm.blocks.machine;

import com.hbm.blocks.BlockEnums.LightType;
import com.hbm.blocks.ModBlocks;
import com.hbm.world.gen.nbt.INBTBlockTransformable;

import api.hbm.block.IToolable;
import api.hbm.block.IToolable.ToolType;
import api.hbm.energymk2.IEnergyReceiverMK2;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class SpotlightPowered extends SpotlightBase implements ITileEntityProvider, IToolable {

	public long powerConsumption;

	public SpotlightPowered(Material mat, int beamLength, LightType type, long powerConsumption, boolean isOn) {
		super(mat, beamLength, type, isOn);
		this.powerConsumption = powerConsumption;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntitySpotlightPowered();
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		if(world.isRemote) return;
		if(isOn) updateBeam(world, x, y, z);
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
		ForgeDirection dir = getDirection(metadata);
		super.breakBlock(world, x, y, z, block, metadata);
		if(world.isRemote) return;
		unpropagateBeam(world, x, y, z, dir);
		world.func_147451_t(x, y, z);
		world.notifyBlocksOfNeighborChange(x, y, z, this);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock) {
		if(world.isRemote) return;
		if(neighborBlock instanceof SpotlightBeam) return;

		ForgeDirection dir = getDirection(world, x, y, z);
		if(!canPlace(world, x, y, z, dir)) {
			dropBlockAsItem(world, x, y, z, 0, 0);
			world.setBlockToAir(x, y, z);
			return;
		}
		if(isOn) updateBeam(world, x, y, z);
	}

	private void updateBeam(World world, int x, int y, int z) {
		if(!isOn) return;
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntitySpotlightPowered) {
			TileEntitySpotlightPowered spotlight = (TileEntitySpotlightPowered) te;
			if(spotlight.power < spotlight.powerConsumption) return;
		}
		ForgeDirection dir = getDirection(world, x, y, z);
		propagateBeam(world, x, y, z, dir, beamLength);
	}

	@Override
	public Item getItemDropped(int i, java.util.Random r, int j) {
		return Item.getItemFromBlock(getOff());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		return Item.getItemFromBlock(getOff());
	}

	@Override
	public Block transformBlock(Block block) {
		return block;
	}

	@Override
	protected Block getOff() {
		if(this == ModBlocks.spotlight_incandescent_powered) return ModBlocks.spotlight_incandescent_powered_off;
		if(this == ModBlocks.spotlight_fluoro_powered) return ModBlocks.spotlight_fluoro_powered_off;
		if(this == ModBlocks.spotlight_halogen_powered) return ModBlocks.spotlight_halogen_powered_off;
		return this;
	}

	@Override
	protected Block getOn() {
		if(this == ModBlocks.spotlight_incandescent_powered_off) return ModBlocks.spotlight_incandescent_powered;
		if(this == ModBlocks.spotlight_fluoro_powered_off) return ModBlocks.spotlight_fluoro_powered;
		if(this == ModBlocks.spotlight_halogen_powered_off) return ModBlocks.spotlight_halogen_powered;
		return this;
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
		if(tool != ToolType.SCREWDRIVER) return false;
		if(!world.isRemote && player.isSneaking()) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te instanceof TileEntitySpotlightPowered) {
				TileEntitySpotlightPowered light = (TileEntitySpotlightPowered) te;
				if(light.isControllerManaged()) {
					light.clearControllerPos();
					player.addChatMessage(new ChatComponentText("Light disconnected from controller"));
					return true;
				}
			}
		}
		return false;
	}

	public boolean canConnectToWire(IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntitySpotlightPowered) {
			return ((TileEntitySpotlightPowered) te).canConnect(dir);
		}
		return true;
	}

	@Override
	public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof TileEntitySpotlightPowered) {
			if(((TileEntitySpotlightPowered) te).isControllerManaged()) return false;
		}
		return super.isSideSolid(world, x, y, z, side);
	}

	public static class TileEntitySpotlightPowered extends TileEntity implements IEnergyReceiverMK2 {
		public static final long maxPower = 1000;
		public long power;
		public long powerConsumption;

		private boolean controllerManaged = false;
		private int controllerX = Integer.MIN_VALUE;
		private int controllerY = Integer.MIN_VALUE;
		private int controllerZ = Integer.MIN_VALUE;

		public TileEntitySpotlightPowered() {
			this.powerConsumption = 20;
		}

		@Override
		public void updateEntity() {
			if(!worldObj.isRemote) {
				if(!this.controllerManaged) {
					ForgeDirection dir = getDirection().getOpposite();
					this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);
				}

				Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
				if(!(block instanceof SpotlightPowered)) return;

				SpotlightPowered spotlight = (SpotlightPowered) block;
				this.powerConsumption = spotlight.powerConsumption;

				if(power >= powerConsumption) {
					power -= powerConsumption;
					if(!spotlight.isOn) switchToState(true);
				} else {
					if(spotlight.isOn) switchToState(false);
				}
			}
		}

		public boolean canConnect(ForgeDirection dir) {
			return !this.controllerManaged;
		}

		protected void switchToState(boolean turnOn) {
			Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
			if(!(block instanceof SpotlightPowered)) return;
			SpotlightPowered spotlight = (SpotlightPowered) block;
			Block targetBlock = turnOn ? spotlight.getOn() : spotlight.getOff();
			if(block == targetBlock) return;

			if(!turnOn && spotlight.isOn) {
				ForgeDirection dir = getDirection();
				SpotlightBase.unpropagateBeam(worldObj, xCoord, yCoord, zCoord, dir);
			}

			NBTTagCompound data = new NBTTagCompound();
			this.writeToNBT(data);
			int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
			worldObj.setBlock(xCoord, yCoord, zCoord, targetBlock, meta, 2);

			TileEntity newTE = worldObj.getTileEntity(xCoord, yCoord, zCoord);
			if(newTE instanceof TileEntitySpotlightPowered) {
				((TileEntitySpotlightPowered) newTE).readFromNBT(data);
				newTE.validate();
				if(turnOn) {
					SpotlightPowered newSpotlight = (SpotlightPowered) targetBlock;
					ForgeDirection dir = ((TileEntitySpotlightPowered) newTE).getDirection();
					SpotlightBase.propagateBeam(worldObj, xCoord, yCoord, zCoord, dir, newSpotlight.beamLength);
				}
				worldObj.func_147451_t(xCoord, yCoord, zCoord);
				((TileEntitySpotlightPowered) newTE).notifyControllerLightChanged();
			}
		}

		private ForgeDirection getDirection() {
			int metadata = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
			return ForgeDirection.getOrientation(metadata >> 1);
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
			this.powerConsumption = nbt.getLong("powerConsumption");
			this.controllerManaged = nbt.getBoolean("controllerManaged");
			this.controllerX = nbt.hasKey("controllerX") ? nbt.getInteger("controllerX") : Integer.MIN_VALUE;
			this.controllerY = nbt.hasKey("controllerY") ? nbt.getInteger("controllerY") : Integer.MIN_VALUE;
			this.controllerZ = nbt.hasKey("controllerZ") ? nbt.getInteger("controllerZ") : Integer.MIN_VALUE;
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt) {
			super.writeToNBT(nbt);
			nbt.setLong("power", power);
			nbt.setLong("powerConsumption", powerConsumption);
			nbt.setBoolean("controllerManaged", controllerManaged);
			if(controllerX != Integer.MIN_VALUE) {
				nbt.setInteger("controllerX", controllerX);
				nbt.setInteger("controllerY", controllerY);
				nbt.setInteger("controllerZ", controllerZ);
			}
		}

		@Override public long getPower() { return power; }
		@Override public void setPower(long power) { this.power = power; }
		@Override public long getMaxPower() { return maxPower; }

		private boolean isLoaded = true;
		@Override public boolean isLoaded() { return isLoaded; }

		public boolean isControllerManaged() { return controllerManaged; }
		public void setControllerManaged(boolean val) {
			boolean changed = (this.controllerManaged != val);
			this.controllerManaged = val;
			this.markDirty();
			if(changed && this.worldObj != null) {
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, this.getBlockType());
			}
		}

		public void setControllerPos(int cx, int cy, int cz) {
			this.controllerX = cx;
			this.controllerY = cy;
			this.controllerZ = cz;
			this.markDirty();
		}

		public void clearControllerPos() {
			if(this.controllerManaged && this.controllerX != Integer.MIN_VALUE && this.worldObj != null) {
				if(this.worldObj.blockExists(this.controllerX, this.controllerY, this.controllerZ)) {
					TileEntity te = this.worldObj.getTileEntity(this.controllerX, this.controllerY, this.controllerZ);
					if(te instanceof PoweredLightsController.TileEntityPoweredLightsController) {
						PoweredLightsController.TileEntityPoweredLightsController controller =
								(PoweredLightsController.TileEntityPoweredLightsController) te;
						controller.removeSavedLightPosition(this.xCoord, this.yCoord, this.zCoord);
					}
				}
			}
			this.controllerX = Integer.MIN_VALUE;
			this.controllerY = Integer.MIN_VALUE;
			this.controllerZ = Integer.MIN_VALUE;
			this.controllerManaged = false;
			this.markDirty();
			if(this.worldObj != null) {
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, this.getBlockType());
			}
		}

		public void notifyControllerLightChanged() {
			if(!controllerManaged) return;
			if(controllerX == Integer.MIN_VALUE) return;
			if(worldObj == null) return;
			if(!worldObj.blockExists(controllerX, controllerY, controllerZ)) return;
			TileEntity te = worldObj.getTileEntity(controllerX, controllerY, controllerZ);
			if(te instanceof PoweredLightsController.TileEntityPoweredLightsController) {
				((PoweredLightsController.TileEntityPoweredLightsController) te).onLightStateChanged(xCoord, yCoord, zCoord);
			}
		}
	}
}
