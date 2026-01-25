package com.hbm.tileentity.machine;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineMixingVat;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.gui.GUIMachineMixingVat;
import com.hbm.inventory.recipes.MixingVatRecipes;
import com.hbm.inventory.recipes.MixingVatRecipes.MixingRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.machine.ItemMixingVatTemplate;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.BobMathUtil;
import com.hbm.util.InventoryUtil;
import com.hbm.util.fauxpointtwelve.DirPos;
import com.hbm.util.i18n.I18nUtil;

import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.fluid.IFluidStandardTransceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMachineMixingVat extends TileEntityMachineBase implements IEnergyReceiverMK2, IFluidStandardTransceiver, IGUIProvider, IUpgradeInfoProvider {

	public long power;
	public static final long maxPower = 100000;
	public int progress;
	public int maxProgress = 100;
	public boolean isProgressing;

	public float prevRot;
	public float mixerRot;

	private AudioWrapper audio;

	public FluidTank[] tanks;

	int consumption = 1250;
	int speed = 100;

	public UpgradeManagerNT upgradeManager = new UpgradeManagerNT();

	public TileEntityMachineMixingVat() {
		super(21);
		tanks = new FluidTank[4];
		for(int i = 0; i < 4; i++) {
			tanks[i] = new FluidTank(Fluids.NONE, 24_000);
		}
	}

	@Override
	public String getName() {
		return "container.mixingVat";
	}

	int lsl0 = 0;
	int lsl1 = 0;
	int lsu0 = 0;
	int lsu1 = 0;

	@Override
	public void updateEntity() {

		if(!worldObj.isRemote) {
			
			this.upgradeManager.checkSlots(this, slots, 1, 3);
			this.applyUpgrades();

			this.isProgressing = false;
			this.power = Library.chargeTEFromItems(slots, 0, power, maxPower);

			int fluidDelay = 40;

			if(lsu0 >= fluidDelay && tanks[0].loadTank(17, 19, slots)) lsl0 = 0;
			if(lsu1 >= fluidDelay && tanks[1].loadTank(18, 20, slots)) lsl1 = 0;

			if(lsl0 >= fluidDelay && slots[17] != null && !FluidTank.noDualUnload.contains(slots[17].getItem())) if(tanks[0].unloadTank(17, 19, slots)) lsu0 = 0;
			if(lsl1 >= fluidDelay && slots[18] != null && !FluidTank.noDualUnload.contains(slots[18].getItem())) if(tanks[1].unloadTank(18, 20, slots)) lsu1 = 0;

			tanks[2].unloadTank(9, 11, slots);
			tanks[3].unloadTank(10, 12, slots);

			if(lsl0 < fluidDelay) lsl0++;
			if(lsl1 < fluidDelay) lsl1++;
			if(lsu0 < fluidDelay) lsu0++;
			if(lsu1 < fluidDelay) lsu1++;

			loadItems();
			unloadItems();

			if(worldObj.getTotalWorldTime() % 20 == 0) {
				this.updateConnections();
			}

			for(DirPos pos : getConPos()) {
				if(tanks[2].getFill() > 0) this.sendFluid(tanks[2], worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
				if(tanks[3].getFill() > 0) this.sendFluid(tanks[3], worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
			}

			if(!canProcess()) {
				this.progress = 0;
				this.mixerRot = 0;
			} else {
				isProgressing = true;
				process();
			}

			this.networkPackNT(150);
		} else {
			prevRot = mixerRot;
			if(isProgressing) {
				mixerRot += 5.0F;
				if(mixerRot >= 360F) {
					mixerRot -= 360F;
					prevRot -= 360F;
				}
			} else {
				if(mixerRot > 0) {
					mixerRot = Math.max(0, mixerRot - 2.0F);
				}
			}

			float volume = this.getVolume(1F);

			if(isProgressing && volume > 0) {
				if(audio == null) {
					audio = createAudioLoop();
					audio.updateVolume(volume);
					audio.startSound();
				} else if(!audio.isPlaying()) {
					audio = rebootAudio(audio);
					audio.updateVolume(volume);
				}
			} else {
				if(audio != null) {
					audio.stopSound();
					audio = null;
				}
			}
		}
	}

	private void applyUpgrades() {
		int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
		int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
		int overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

		this.speed = 100;
		this.consumption = 1250;

		this.speed -= speedLevel * 25;
		this.consumption += speedLevel * 300;
		
		this.speed += powerLevel * 15;
		this.consumption -= powerLevel * 20;
		
		this.speed /= (overLevel + 1);
		this.consumption *= (overLevel + 1);

		if(this.speed <= 0) this.speed = 1;
	}

	private void updateConnections() {
		for(DirPos pos : getConPos()) {
			this.trySubscribe(worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
			this.trySubscribe(tanks[0].getTankType(), worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
			this.trySubscribe(tanks[1].getTankType(), worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
		}
	}

	public DirPos[] getConPos() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		return new DirPos[] {
				new DirPos(xCoord + dir.offsetX, yCoord, zCoord + dir.offsetZ, dir),
				new DirPos(xCoord + dir.offsetX + rot.offsetX, yCoord, zCoord + dir.offsetZ + rot.offsetZ, dir),
				new DirPos(xCoord + dir.offsetX - rot.offsetX, yCoord, zCoord + dir.offsetZ - rot.offsetZ, dir),
				new DirPos(xCoord - dir.offsetX * 2, yCoord, zCoord - dir.offsetZ * 2, dir.getOpposite()),
				new DirPos(xCoord - dir.offsetX * 2 + rot.offsetX, yCoord, zCoord - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
				new DirPos(xCoord - dir.offsetX * 2 - rot.offsetX, yCoord, zCoord - dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite()),
				new DirPos(xCoord + rot.offsetX * 2, yCoord, zCoord + rot.offsetZ * 2, rot),
				new DirPos(xCoord - dir.offsetX + rot.offsetX * 2, yCoord, zCoord - dir.offsetZ + rot.offsetZ * 2, rot),
				new DirPos(xCoord - rot.offsetX * 2, yCoord, zCoord - rot.offsetZ * 2, rot.getOpposite()),
				new DirPos(xCoord - dir.offsetX - rot.offsetX * 2, yCoord, zCoord - dir.offsetZ - rot.offsetZ * 2, rot.getOpposite())
		};
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeInt(progress);
		buf.writeInt(maxProgress);
		buf.writeBoolean(isProgressing);

		for(int i = 0; i < tanks.length; i++)
			tanks[i].serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		power = buf.readLong();
		progress = buf.readInt();
		maxProgress = buf.readInt();
		isProgressing = buf.readBoolean();

		for(int i = 0; i < tanks.length; i++)
			tanks[i].deserialize(buf);
	}

	@Override
	public AudioWrapper createAudioLoop() {
		return MainRegistry.proxy.getLoopedSound("hbm:block.chemicalPlant", xCoord, yCoord, zCoord, 1.0F, 10F, 1.0F);
	}

	@Override
	public void onChunkUnload() {
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	private boolean canProcess() {
		if(slots[4] == null || !(slots[4].getItem() instanceof ItemMixingVatTemplate))
			return false;

		MixingRecipe recipe = MixingVatRecipes.indexMapping.get(slots[4].getItemDamage());

		if(recipe == null)
			return false;

		setupTanks(recipe);

		if(this.power < this.consumption) return false;
		if(!hasRequiredFluids(recipe)) return false;
		if(!hasSpaceForFluids(recipe)) return false;
		if(!hasRequiredItems(recipe)) return false;
		if(!hasSpaceForItems(recipe)) return false;

		return true;
	}

	private void setupTanks(MixingRecipe recipe) {
		if(recipe.inputFluids[0] != null) tanks[0].withPressure(recipe.inputFluids[0].pressure).setTankType(recipe.inputFluids[0].type); else tanks[0].setTankType(Fluids.NONE);
		if(recipe.inputFluids[1] != null) tanks[1].withPressure(recipe.inputFluids[1].pressure).setTankType(recipe.inputFluids[1].type); else tanks[1].setTankType(Fluids.NONE);
		if(recipe.outputFluids[0] != null) tanks[2].withPressure(recipe.outputFluids[0].pressure).setTankType(recipe.outputFluids[0].type); else tanks[2].setTankType(Fluids.NONE);
		if(recipe.outputFluids[1] != null) tanks[3].withPressure(recipe.outputFluids[1].pressure).setTankType(recipe.outputFluids[1].type); else tanks[3].setTankType(Fluids.NONE);
	}

	private boolean hasRequiredFluids(MixingRecipe recipe) {
		if(recipe.inputFluids[0] != null && tanks[0].getFill() < recipe.inputFluids[0].fill) return false;
		if(recipe.inputFluids[1] != null && tanks[1].getFill() < recipe.inputFluids[1].fill) return false;
		return true;
	}

	private boolean hasSpaceForFluids(MixingRecipe recipe) {
		if(recipe.outputFluids[0] != null && tanks[2].getFill() + recipe.outputFluids[0].fill > tanks[2].getMaxFill()) return false;
		if(recipe.outputFluids[1] != null && tanks[3].getFill() + recipe.outputFluids[1].fill > tanks[3].getMaxFill()) return false;
		return true;
	}

	private boolean hasRequiredItems(MixingRecipe recipe) {
		return InventoryUtil.doesArrayHaveIngredients(slots, 13, 16, recipe.inputs);
	}

	private boolean hasSpaceForItems(MixingRecipe recipe) {
		return InventoryUtil.doesArrayHaveSpace(slots, 5, 8, recipe.outputs);
	}

	private void process() {
		this.power -= this.consumption;
		this.progress++;

		MixingRecipe recipe = MixingVatRecipes.indexMapping.get(slots[4].getItemDamage());

		this.maxProgress = recipe.getDuration() * this.speed / 100;

		if(maxProgress <= 0) maxProgress = 1;

		if(this.progress >= this.maxProgress) {
			consumeFluids(recipe);
			produceFluids(recipe);
			consumeItems(recipe);
			produceItems(recipe);
			this.progress = 0;
			this.markDirty();
		}
	}

	private void consumeFluids(MixingRecipe recipe) {
		if(recipe.inputFluids[0] != null) tanks[0].setFill(tanks[0].getFill() - recipe.inputFluids[0].fill);
		if(recipe.inputFluids[1] != null) tanks[1].setFill(tanks[1].getFill() - recipe.inputFluids[1].fill);
	}

	private void produceFluids(MixingRecipe recipe) {
		if(recipe.outputFluids[0] != null) tanks[2].setFill(tanks[2].getFill() + recipe.outputFluids[0].fill);
		if(recipe.outputFluids[1] != null) tanks[3].setFill(tanks[3].getFill() + recipe.outputFluids[1].fill);
	}

	private void consumeItems(MixingRecipe recipe) {
		for(AStack in : recipe.inputs) {
			if(in != null)
				InventoryUtil.tryConsumeAStack(slots, 13, 16, in);
		}
	}

	private void produceItems(MixingRecipe recipe) {
		for(ItemStack out : recipe.outputs) {
			if(out != null)
				InventoryUtil.tryAddItemToInventory(slots, 5, 8, out.copy());
		}
	}

	private void loadItems() {
		if(slots[4] == null || !(slots[4].getItem() instanceof ItemMixingVatTemplate))
			return;

		MixingRecipe recipe = MixingVatRecipes.indexMapping.get(slots[4].getItemDamage());

		if(recipe != null) {
			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
				ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset).getOpposite();
				ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
				for (int dy = 0; dy <= 1; dy++) {
					for (int dz = -1; dz <= 2; dz++) {
						for (int dx = 0; dx <= 2; dx++) {
							int bx = xCoord + dir.offsetX * dz + rot.offsetX * dx;
							int by = yCoord + dy;
							int bz = zCoord + dir.offsetZ * dz + rot.offsetZ * dx;
							TileEntity te = worldObj.getTileEntity(bx + side.offsetX, by + side.offsetY, bz + side.offsetZ);
							if(te instanceof IInventory) {
								IInventory inv = (IInventory) te;
								ISidedInventory sided = inv instanceof ISidedInventory ? (ISidedInventory) inv : null;
								int[] access = sided != null ? sided.getAccessibleSlotsFromSide(side.getOpposite().ordinal()) : null;

								for(AStack ingredient : recipe.inputs) {
									outer:
									while(!InventoryUtil.doesArrayHaveIngredients(slots, 13, 16, ingredient)) {
										boolean found = false;
										for(int i = 0; i < (access != null ? access.length : inv.getSizeInventory()); i++) {
											int slot = access != null ? access[i] : i;
											ItemStack stack = inv.getStackInSlot(slot);

											if(ingredient.matchesRecipe(stack, true) && (sided == null || sided.canExtractItem(slot, stack, side.getOpposite().ordinal()))) {
												for(int j = 13; j <= 16; j++) {
													if(slots[j] != null && slots[j].stackSize < slots[j].getMaxStackSize() & InventoryUtil.doesStackDataMatch(slots[j], stack)) {
														inv.decrStackSize(slot, 1);
														slots[j].stackSize++;
														found = true;
														continue outer;
													}
												}
												for(int j = 13; j <= 16; j++) {
													if(slots[j] == null) {
														slots[j] = stack.copy();
														slots[j].stackSize = 1;
														inv.decrStackSize(slot, 1);
														found = true;
														continue outer;
													}
												}
											}
										}
										if(!found) break outer;
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void unloadItems() {
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset).getOpposite();
			ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
			for (int dy = 0; dy <= 1; dy++) {
				for (int dz = -1; dz <= 2; dz++) {
					for (int dx = 0; dx <= 2; dx++) {
						int bx = xCoord + dir.offsetX * dz + rot.offsetX * dx;
						int by = yCoord + dy;
						int bz = zCoord + dir.offsetZ * dz + rot.offsetZ * dx;
						TileEntity te = worldObj.getTileEntity(bx + side.offsetX, by + side.offsetY, bz + side.offsetZ);

						if(te instanceof IInventory) {
							IInventory inv = (IInventory) te;
							ISidedInventory sided = inv instanceof ISidedInventory ? (ISidedInventory) inv : null;
							int[] access = sided != null ? sided.getAccessibleSlotsFromSide(side.getOpposite().ordinal()) : null;

							boolean shouldOutput = true;
							while(shouldOutput) {
								shouldOutput = false;
								outer:
								for(int i = 5; i <= 8; i++) {
									ItemStack out = slots[i];
									if(out != null) {
										for(int j = 0; j < (access != null ? access.length : inv.getSizeInventory()); j++) {
											int slot = access != null ? access[j] : j;
											if(!inv.isItemValidForSlot(slot, out)) continue;
											ItemStack target = inv.getStackInSlot(slot);

											if(InventoryUtil.doesStackDataMatch(out, target) && target.stackSize < Math.min(target.getMaxStackSize(), inv.getInventoryStackLimit())) {
												int toDec = Math.min(out.stackSize, Math.min(target.getMaxStackSize(), inv.getInventoryStackLimit()) - target.stackSize);
												this.decrStackSize(i, toDec);
												target.stackSize += toDec;
												shouldOutput = true;
												break outer;
											}
										}
										for(int j = 0; j < (access != null ? access.length : inv.getSizeInventory()); j++) {
											int slot = access != null ? access[j] : j;
											if(!inv.isItemValidForSlot(slot, out)) continue;
											if(inv.getStackInSlot(slot) == null && (sided != null ? sided.canInsertItem(slot, out, side.getOpposite().ordinal()) : inv.isItemValidForSlot(slot, out))) {
												ItemStack copy = out.copy();
												copy.stackSize = 1;
												inv.setInventorySlotContents(slot, copy);
												this.decrStackSize(i, 1);
												shouldOutput = true;
												break outer;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public long getPower() {
		return this.power;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		this.progress = nbt.getInteger("progress");

		for(int i = 0; i < tanks.length; i++) {
			tanks[i].readFromNBT(nbt, "t" + i);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		nbt.setInteger("progress", progress);

		for(int i = 0; i < tanks.length; i++) {
			tanks[i].writeToNBT(nbt, "t" + i);
		}
	}

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) {
			bb = AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord, zCoord - 1, xCoord + 3, yCoord + 2, zCoord + 3);
		}
		return bb;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack stack) {
		super.setInventorySlotContents(i, stack);

		if(stack != null && i >= 1 && i <= 3 && stack.getItem() instanceof ItemMachineUpgrade)
			worldObj.playSoundEffect(xCoord + 0.5, yCoord + 1.5, zCoord + 0.5, "hbm:item.upgradePlug", 1.5F, 1.0F);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public FluidTank[] getSendingTanks() {
		return new FluidTank[] {tanks[2], tanks[3]};
	}

	@Override
	public FluidTank[] getReceivingTanks() {
		return new FluidTank[] {tanks[0], tanks[1]};
	}

	@Override
	public FluidTank[] getAllTanks() {
		return tanks;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineMixingVat(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineMixingVat(player.inventory, this);
	}

	@Override
	public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
		return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
	}

	@Override
	public void provideInfo(UpgradeType type, int level, List<String> info, boolean extendedInfo) {
		info.add(IUpgradeInfoProvider.getStandardLabel(ModBlocks.machine_mixing_vat));
		
		if(type == UpgradeType.SPEED) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("upgrade.speed", "-" + (level * 25) + "%"));
			info.add(EnumChatFormatting.RED + I18nUtil.resolveKey("upgrade.consumption", "+" + (level * 300) + "%"));
		}
		if(type == UpgradeType.POWER) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("upgrade.consumption", "-" + (level * 30) + "%"));
			info.add(EnumChatFormatting.RED + I18nUtil.resolveKey("upgrade.speed", "+" + (level * 15) + "%"));
		}
		if(type == UpgradeType.OVERDRIVE) {
			info.add((BobMathUtil.getBlink() ? EnumChatFormatting.RED : EnumChatFormatting.DARK_GRAY) + "YES");
		}
	}

	@Override
	public HashMap<UpgradeType, Integer> getValidUpgrades() {
		HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
		upgrades.put(UpgradeType.SPEED, 3);
		upgrades.put(UpgradeType.POWER, 3);
		upgrades.put(UpgradeType.OVERDRIVE, 9);
		return upgrades;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return new int[] {5, 6, 7, 8, 13, 14, 15, 16};
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {
		return (slot >= 13 && slot <= 16);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return (slot >= 5 && slot <= 8);
	}
}