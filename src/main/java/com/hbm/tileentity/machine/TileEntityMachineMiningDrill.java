package com.hbm.tileentity.machine;

import java.util.HashMap;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineMiningDrill;
import com.hbm.inventory.gui.GUIMachineMiningDrill;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.ItemStackUtil;
import com.hbm.util.i18n.I18nUtil;

import api.hbm.block.IDrillInteraction;
import api.hbm.block.IMiningDrill;
import api.hbm.energymk2.IEnergyReceiverMK2;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMachineMiningDrill extends TileEntityMachineBase implements IEnergyReceiverMK2, IMiningDrill, IGUIProvider, IUpgradeInfoProvider {

	public long power;
	public int warning;
	public static final long maxPower = 2500000;
	int age = 0;
	int timer = 20;
	int consumption = 2500;
	int fortune = 0;
	boolean flag = true;
	public float torque;
	public float rotation;
	
	// Ore detection radius - starts at 2 (5x5 area)
	public int oreRadius = 2;
	
	public UpgradeManagerNT upgradeManager = new UpgradeManagerNT();

	public TileEntityMachineMiningDrill() {
		super(13);
	}

	@Override
	public String getName() {
		return "container.miningDrill";
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemStack) {
		if(i == 0)
			return true;
		
		if(i == 1)
			return true;
		
		return false;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
	}
	
	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}
	
	// New method to clean up drill pipes when the TileEntity is removed
	@Override
	public void invalidate() {
		// Only run on the server side
		if (!worldObj.isRemote) {
			// Iterate downwards from one block below the drill
			for (int i = this.yCoord - 1; i > 0; i--) {
				Block blockBelow = worldObj.getBlock(xCoord, i, zCoord);
				
				// If the block is a drill pipe, remove it
				if (blockBelow == ModBlocks.drill_pipe) {
					worldObj.setBlockToAir(xCoord, i, zCoord);
				} else {
					// Stop removing blocks once a non-pipe block is found
					break;
				}
			}
		}
		super.invalidate();
	}
	
	@Override
	public void updateEntity() {
		
		if(!worldObj.isRemote) {
			
			this.updateConnections();
			
			// Check upgrades first
			upgradeManager.checkSlots(this, slots, 10, 12);
			
			// Apply upgrades based on examples
			int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
			int effectLevel = upgradeManager.getLevel(UpgradeType.EFFECT);
			int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
			
			// Base values
			this.consumption = 2500;
			this.timer = 40; // Base speed 2x slower
			this.oreRadius = 2; // Base 5x5 area
			
			// SPEED upgrades - mine faster but use more power
			this.timer -= Math.min(speedLevel, 3) * 8; // Adjusting speed reduction for new base timer
			this.consumption += Math.min(speedLevel, 3) * 1670; // Increased power usage (Level 3 ~7510 total)
			
			// POWER upgrades - save power but mine slower
			this.consumption -= Math.min(powerLevel, 3) * 400; // Less power usage
			this.timer += Math.min(powerLevel, 3) * 2; // Slower mining
			
            // --- EFFECT UPGRADE CONSUMPTION LOGIC (NEW) ---
			int effectMultiplier = 1;
			
			// EFFECT upgrades - increase ore radius as specified
			if(effectLevel >= 1) {
				this.oreRadius = 3; // 7x7 area
				effectMultiplier = 2; // 2x power
			}
			if(effectLevel >= 2) {
				this.oreRadius = 5; // 11x11 area  
				effectMultiplier = 5; // 5x power
			}
			if(effectLevel >= 3) {
				this.oreRadius = 7; // 15x15 area
				effectMultiplier = 10; // 10x power
			}
			
			// Apply the effect multiplier to consumption
			this.consumption *= effectMultiplier;
            // ----------------------------------------------
			
			// Ensure timer doesn't go too low
			if(timer < 5) timer = 5;
			
			age++;
			if(age >= timer)
				age -= timer;
			
			power = Library.chargeTEFromItems(slots, 0, power, maxPower);

            // Check if the output inventory (slots 1-9) is full
            boolean isFull = isOutputInventoryFull();
			
			// The operation only runs if there is enough power AND the inventory is NOT full
			if(power >= consumption && !isFull) {
				
				//operation start
				
				if(age == timer - 1) {
					warning = 0; // Clear warning, operation is running
					
					for(int i = this.yCoord - 1; i > 0; i--) {
						
						if(i <= 1) {
							warning = 2;
							break;
						}
						
						if(worldObj.getBlock(xCoord, i, zCoord) != ModBlocks.drill_pipe) {
							
							if(worldObj.getBlock(xCoord, i, zCoord).isReplaceable(worldObj, xCoord, i, zCoord) || this.tryDrillCenter(xCoord, i, zCoord)) {
								
								if(worldObj.getBlock(xCoord, i, zCoord).isReplaceable(worldObj, xCoord, i, zCoord)) {
									worldObj.setBlock(xCoord, i, zCoord, ModBlocks.drill_pipe);
								}
								
								// Mine ores in the radius determined by upgrades
								mineOresInRadius(xCoord, i, zCoord, oreRadius);
								break;
								
							} else {
								this.warning = 1;
								break;
							}
						}
						
						// Mine ores at pipe level too
						mineOresInRadius(xCoord, i, zCoord, oreRadius);
					}
				}
				
				//operation end
				
				power -= consumption; // Consume power only if operation is running
			} else {
				// Stop operation and set warning to 1 (yellow mark) if low power or full inventory
				warning = 1;
			}
			
			int meta = worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
			TileEntity te = null;
			if(meta == 2) {
				te = worldObj.getTileEntity(xCoord - 2, yCoord, zCoord);
			}
			if(meta == 3) {
				te = worldObj.getTileEntity(xCoord + 2, yCoord, zCoord);
			}
			if(meta == 4) {
				te = worldObj.getTileEntity(xCoord, yCoord, zCoord + 2);
			}
			if(meta == 5) {
				te = worldObj.getTileEntity(xCoord, yCoord, zCoord - 2);
			}
			
			if(te != null && te instanceof IInventory) {
				IInventory chest = (IInventory)te;
				
				for(int i = 1; i < 10; i++)
					if(tryFillContainer(chest, i))
						break;
			}
			
			// Rotor speed based on operational status
			if(warning == 0) { // Only spin up if the operation is running
				torque += 0.3;
				if(torque > (300/timer))
					torque = (300/timer);
			} else { // In any warning state (low power, full inventory, un-drillable block)
				torque -= 0.2F;
				if(torque < -(300/timer))
					torque = -(300/timer);
			}
			
			if(torque < 0) {
				torque = 0;
			}
			rotation += torque;
			if(rotation >= 360)
				rotation -= 360;

			this.networkPackNT(50);
		}
	}
	
	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(this.power);
		buf.writeInt(this.warning);
		buf.writeFloat(this.rotation);
		buf.writeFloat(this.torque);
		buf.writeInt(this.oreRadius);
	}
	
	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.power = buf.readLong();
		this.warning = buf.readInt();
		this.rotation = buf.readFloat();
		this.torque = buf.readFloat();
		this.oreRadius = buf.readInt();
	}
	
	private void updateConnections() {
		this.getBlockMetadata();
		
		if(this.blockMetadata == 5 || this.blockMetadata == 4) {
			this.trySubscribe(worldObj, xCoord + 2, yCoord, zCoord, ForgeDirection.EAST);
			this.trySubscribe(worldObj, xCoord - 2, yCoord, zCoord, ForgeDirection.WEST);
			
		} else if(this.blockMetadata == 3 || this.blockMetadata == 2) {
			this.trySubscribe(worldObj, xCoord, yCoord, zCoord + 2, ForgeDirection.SOUTH);
			this.trySubscribe(worldObj, xCoord, yCoord, zCoord - 2, ForgeDirection.NORTH);
		}
	}
	
	/** Mines only the center block for the 1x1 hole */
	public boolean tryDrillCenter(int x, int y, int z) {
		if(worldObj.getBlock(x, y, z).isAir(worldObj, x, y, z) || !isMinableOreo(x, y, z))
			return false;
		if(worldObj.getBlock(x, y, z).getMaterial().isLiquid()) {
			worldObj.func_147480_a(x, y, z, false);
			return false;
		}
		
		Block b = worldObj.getBlock(x, y, z);
		int meta = worldObj.getBlockMetadata(x, y, z);
		
		if(b instanceof IDrillInteraction) {
			IDrillInteraction in = (IDrillInteraction) b;
			
			ItemStack sta = in.extractResource(worldObj, x, y, z, meta, this);

			if(sta != null && hasSpace(sta)) {
				this.addItemToInventory(sta);
			}
			
			if(!in.canBreak(worldObj, x, y, z, meta, this))
				return true;
		}
		
		ItemStack stack = new ItemStack(b.getItemDropped(meta, worldObj.rand, fortune), b.quantityDropped(meta, fortune, worldObj.rand), b.damageDropped(meta));

		if(stack != null && stack.getItem() == null) {
			worldObj.func_147480_a(x, y, z, false);
			return true;
		}
		
		if(hasSpace(stack)) {
			this.addItemToInventory(stack);
			worldObj.func_147480_a(x, y, z, false);
			return true;
		}
		
		return true;
	}
	
	/** Mines ores in the specified radius around the center */
	public void mineOresInRadius(int centerX, int centerY, int centerZ, int rad) {
		for(int ix = centerX - rad; ix <= centerX + rad; ix++) {
			for(int iz = centerZ - rad; iz <= centerZ + rad; iz++) {
				
				// Skip the center block (already handled by tryDrillCenter)
				if(ix == centerX && iz == centerZ) continue;
				
				if(isOre(ix, centerY, iz)) {
					tryMineOre(ix, centerY, iz);
				}
			}
		}
	}
	
	/** Checks if a block is an ore */
	public boolean isOre(int x, int y, int z) {
		Block b = worldObj.getBlock(x, y, z);
		Item blockItem = Item.getItemFromBlock(b);
		
		if(blockItem != null) {
			List<String> names = ItemStackUtil.getOreDictNames(new ItemStack(blockItem));
			for(String name : names) {
				if(name.startsWith("ore")) {
					return true;
				}
			}
		}
		return false;
	}
	
	/** Mines a single ore block */
	public void tryMineOre(int x, int y, int z) {
		Block b = worldObj.getBlock(x, y, z);
		int meta = worldObj.getBlockMetadata(x, y, z);
		
		if(b instanceof IDrillInteraction) {
			IDrillInteraction in = (IDrillInteraction) b;
			ItemStack sta = in.extractResource(worldObj, x, y, z, meta, this);
			if(sta != null && hasSpace(sta)) {
				this.addItemToInventory(sta);
			}
			if(!in.canBreak(worldObj, x, y, z, meta, this)) return;
		}
		
		ItemStack stack = new ItemStack(b.getItemDropped(meta, worldObj.rand, fortune), b.quantityDropped(meta, fortune, worldObj.rand), b.damageDropped(meta));
		
		if(stack != null && stack.getItem() == null) {
			worldObj.func_147480_a(x, y, z, false);
			return;
		}
		
		if(hasSpace(stack)) {
			this.addItemToInventory(stack);
			worldObj.func_147480_a(x, y, z, false);
			return;
		}
	}
	
	public boolean tryFillContainer(IInventory inventory, int slot) {
		
		int size = inventory.getSizeInventory();

		for(int i = 0; i < size; i++) {
			if(inventory.getStackInSlot(i) != null) {
				
				if(slots[slot] == null)
					return false;
				
				ItemStack sta1 = inventory.getStackInSlot(i).copy();
				ItemStack sta2 = slots[slot].copy();
				
				if(!inventory.isItemValidForSlot(i, sta2))
					continue;
				
				if(sta1 != null && sta2 != null) {
					sta1.stackSize = 1;
					sta2.stackSize = 1;
				
					if(ItemStack.areItemStacksEqual(sta1, sta2) && ItemStack.areItemStackTagsEqual(sta1, sta2) && inventory.getStackInSlot(i).stackSize < inventory.getStackInSlot(i).getMaxStackSize()) {
						slots[slot].stackSize--;
						
						if(slots[slot].stackSize <= 0)
							slots[slot] = null;
						
						ItemStack sta3 = inventory.getStackInSlot(i).copy();
						sta3.stackSize++;
						inventory.setInventorySlotContents(i, sta3);
					
						return true;
					}
				}
			}
		}
		for(int i = 0; i < size; i++) {
			
			if(slots[slot] == null)
				return false;
			
			ItemStack sta2 = slots[slot].copy();
			
			if(!inventory.isItemValidForSlot(i, sta2))
				continue;
			
			if(inventory.getStackInSlot(i) == null && sta2 != null) {
				sta2.stackSize = 1;
				slots[slot].stackSize--;
				
				if(slots[slot].stackSize <= 0)
					slots[slot] = null;
				
				inventory.setInventorySlotContents(i, sta2);
					
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isMinableOreo(int x, int y, int z) {
		
		Block b = worldObj.getBlock(x, y, z);
		float hardness = b.getBlockHardness(worldObj, x, y, z);
		
		return (hardness < 70 && hardness >= 0) || b instanceof IDrillInteraction;
	}
	
	public boolean hasSpace(ItemStack stack) {

		ItemStack st = stack.copy();
		
		if(st == null)
			return true;
		
		for(int i = 1; i < 10; i++) {
			if(slots[i] == null)
				return true;
		}
		
		st.stackSize = 1;
		
		ItemStack[] fakeArray = slots.clone();
		boolean flag = true;
		for(int i = 0; i < stack.stackSize; i++) {
			if(!canAddItemToArray(st, fakeArray))
				flag = false;
		}
		
		return flag;
	}
	
	public void addItemToInventory(ItemStack stack) {

		ItemStack st = stack.copy();
		
		if(st == null)
			return;
		
		int size = st.stackSize;
		st.stackSize = 1;
		
		for(int i = 0; i < size; i++)
			canAddItemToArray(st, this.slots);
		
	}
	
	public boolean canAddItemToArray(ItemStack stack, ItemStack[] array) {

		ItemStack st = stack.copy();
		
		if(stack == null || st == null)
			return true;
		
		for(int i = 1; i < 10; i++) {
			
			if(array[i] != null) {
				ItemStack sta = array[i].copy();
				
				if(stack == null || st == null)
					return true;
				
				if(sta != null && sta.getItem() == st.getItem() && sta.stackSize < st.getMaxStackSize()) {
					array[i].stackSize++;
					return true;
				}
			}
		}
		
		for(int i = 1; i < 10; i++) {
			if(array[i] == null) {
				array[i] = stack.copy();
				return true;
			}
			
		}
		
		return false;
	}
	
    /** Checks if the output inventory (slots 1-9) is completely full */
	public boolean isOutputInventoryFull() {
		for(int i = 1; i < 10; i++) {
			// If slot is empty (null) or not at max stack size, it has space
			if(slots[i] == null || (slots[i] != null && slots[i].stackSize < slots[i].getMaxStackSize())) {
				return false;
			}
		}
		// All 9 output slots are occupied and full
		return true;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return 65536.0D;
	}

	@Override
	public DrillType getDrillTier() {
		return DrillType.INDUSTRIAL;
	}

	@Override
	public int getDrillRating() {
		return 100;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineMiningDrill(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineMiningDrill(player.inventory, this);
	}
	
	// IUpgradeInfoProvider implementation based on examples
	@Override
	public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
		return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.EFFECT;
	}

	@Override
	public void provideInfo(UpgradeType type, int level, List<String> info, boolean extendedInfo) {
		info.add(IUpgradeInfoProvider.getStandardLabel(ModBlocks.machine_drill));
		if(type == UpgradeType.SPEED) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("upgrade.speed", "-" + (level * 20) + "%"));
			info.add(EnumChatFormatting.RED + I18nUtil.resolveKey("upgrade.consumption", "+" + (level * 30) + "%"));
		}
		if(type == UpgradeType.POWER) {
			info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("upgrade.consumption", "-" + (level * 16) + "%"));
			info.add(EnumChatFormatting.RED + I18nUtil.resolveKey("upgrade.speed", "+" + (level * 10) + "%"));
		}
		if(type == UpgradeType.EFFECT) {
			int[] radii = {5, 7, 11, 15};
			if(level < radii.length) {
				info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("upgrade.radius", radii[level] + "x" + radii[level]));
			}
		}
	}

	public int getMaxLevel(UpgradeType type) {
		switch(type) {
		case SPEED: return 3;
		case POWER: return 3;
		case EFFECT: return 3;
		default: return 0;
		}
	}
	
	@Override
	public HashMap<UpgradeType, Integer> getValidUpgrades() {
		HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
		upgrades.put(UpgradeType.SPEED, 3);
		upgrades.put(UpgradeType.POWER, 3);
		upgrades.put(UpgradeType.EFFECT, 3);
		return upgrades;
	}
}