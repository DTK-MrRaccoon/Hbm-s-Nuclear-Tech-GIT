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
    public float torque;
    public float rotation;
    public int soundCycle = 0;
    public int oreRadius = 2;
    
    private int age = 0;
    private int timer = 20;
    private int consumption = 2500;
    private int fortune = 0;
    
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
        return i == 0 || i == 1;
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
    
    @Override
    public void invalidate() {
        if (!worldObj.isRemote) {
            for (int i = this.yCoord - 1; i > 0; i--) {
                if (worldObj.getBlock(xCoord, i, zCoord) == ModBlocks.drill_pipe) {
                    worldObj.setBlockToAir(xCoord, i, zCoord);
                } else {
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
            upgradeManager.checkSlots(this, slots, 10, 12);
            
            applyUpgrades();
            
            age++;
            if(age >= timer) age -= timer;
            
            power = Library.chargeTEFromItems(slots, 0, power, maxPower);

            boolean isFull = isOutputInventoryFull();
            
            if(power >= consumption && !isFull) {
                if(age == timer - 1) {
                    warning = 0;
                    drillOperation();
                }
                power -= consumption;
                handleRunningSound();
            } else {
                warning = 1;
                soundCycle = 0;
            }
            
            handleChestTransfer();
            updateRotation();
            this.networkPackNT(50);
        }
    }
    
    private void applyUpgrades() {
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int effectLevel = upgradeManager.getLevel(UpgradeType.EFFECT);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        
        consumption = 2500;
        timer = 40;
        oreRadius = 2;
        
        timer -= Math.min(speedLevel, 3) * 8;
        consumption += Math.min(speedLevel, 3) * 1670;
        
        consumption -= Math.min(powerLevel, 3) * 400;
        timer += Math.min(powerLevel, 3) * 2;
        
        int effectMultiplier = 1;
        if(effectLevel >= 1) {
            oreRadius = 3;
            effectMultiplier = 2;
        }
        if(effectLevel >= 2) {
            oreRadius = 5;
            effectMultiplier = 5;
        }
        if(effectLevel >= 3) {
            oreRadius = 7;
            effectMultiplier = 10;
        }
        
        consumption *= effectMultiplier;
        if(timer < 5) timer = 5;
    }
    
    private void drillOperation() {
        for(int i = this.yCoord - 1; i > 0; i--) {
            if(i <= 1) {
                warning = 2;
                break;
            }
            
            if(worldObj.getBlock(xCoord, i, zCoord) != ModBlocks.drill_pipe) {
                if(worldObj.getBlock(xCoord, i, zCoord).isReplaceable(worldObj, xCoord, i, zCoord) || tryDrillCenter(xCoord, i, zCoord)) {
                    if(worldObj.getBlock(xCoord, i, zCoord).isReplaceable(worldObj, xCoord, i, zCoord)) {
                        worldObj.setBlock(xCoord, i, zCoord, ModBlocks.drill_pipe);
                    }
                    mineOresInRadius(xCoord, i, zCoord, oreRadius);
                    break;
                } else {
                    warning = 1;
                    break;
                }
            }
            mineOresInRadius(xCoord, i, zCoord, oreRadius);
        }
    }
    
    private void handleRunningSound() {
        if(warning == 0) {
            if(soundCycle == 0) {
                float volume = 1.0F;
                try {
                    volume = getVolume(1.0F);
                } catch (NoSuchMethodError e) {}
                this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "minecart.base", volume, 0.75F);
            }
            soundCycle++;
            if(soundCycle >= 50) soundCycle = 0;
        } else {
            soundCycle = 0;
        }
    }
    
    private void handleChestTransfer() {
        int meta = worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        TileEntity te = null;
        
        switch(meta) {
            case 2: te = worldObj.getTileEntity(xCoord - 2, yCoord, zCoord); break;
            case 3: te = worldObj.getTileEntity(xCoord + 2, yCoord, zCoord); break;
            case 4: te = worldObj.getTileEntity(xCoord, yCoord, zCoord + 2); break;
            case 5: te = worldObj.getTileEntity(xCoord, yCoord, zCoord - 2); break;
        }
        
        if(te instanceof IInventory) {
            IInventory chest = (IInventory)te;
            for(int i = 1; i < 10; i++) {
                if(tryFillContainer(chest, i)) break;
            }
        }
    }
    
    private void updateRotation() {
        if(warning == 0) {
            torque += 0.3;
            if(torque > (300/timer)) torque = (300/timer);
        } else {
            torque -= 0.2F;
            if(torque < -(300/timer)) torque = -(300/timer);
        }
        
        if(torque < 0) torque = 0;
        rotation += torque;
        if(rotation >= 360) rotation -= 360;
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
                addItemToInventory(sta);
            }
            if(!in.canBreak(worldObj, x, y, z, meta, this))
                return true;
        }
        
        ItemStack stack = new ItemStack(b.getItemDropped(meta, worldObj.rand, fortune), 
                                      b.quantityDropped(meta, fortune, worldObj.rand), 
                                      b.damageDropped(meta));

        if(stack != null && stack.getItem() == null) {
            worldObj.func_147480_a(x, y, z, false);
            return true;
        }
        
        if(hasSpace(stack)) {
            addItemToInventory(stack);
            worldObj.func_147480_a(x, y, z, false);
            return true;
        }
        
        return true;
    }
    
    public void mineOresInRadius(int centerX, int centerY, int centerZ, int rad) {
        for(int ix = centerX - rad; ix <= centerX + rad; ix++) {
            for(int iz = centerZ - rad; iz <= centerZ + rad; iz++) {
                if(ix == centerX && iz == centerZ) continue;
                if(isOre(ix, centerY, iz)) {
                    tryMineOre(ix, centerY, iz);
                }
            }
        }
    }
    
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
    
    public void tryMineOre(int x, int y, int z) {
        Block b = worldObj.getBlock(x, y, z);
        int meta = worldObj.getBlockMetadata(x, y, z);
        
        if(b instanceof IDrillInteraction) {
            IDrillInteraction in = (IDrillInteraction) b;
            ItemStack sta = in.extractResource(worldObj, x, y, z, meta, this);
            if(sta != null && hasSpace(sta)) {
                addItemToInventory(sta);
            }
            if(!in.canBreak(worldObj, x, y, z, meta, this)) return;
        }
        
        ItemStack stack = new ItemStack(b.getItemDropped(meta, worldObj.rand, fortune), 
                                      b.quantityDropped(meta, fortune, worldObj.rand), 
                                      b.damageDropped(meta));
        
        if(stack != null && stack.getItem() == null) {
            worldObj.func_147480_a(x, y, z, false);
            return;
        }
        
        if(hasSpace(stack)) {
            addItemToInventory(stack);
            worldObj.func_147480_a(x, y, z, false);
        }
    }
    
    public boolean tryFillContainer(IInventory inventory, int slot) {
        if(slots[slot] == null) return false;
        
        int size = inventory.getSizeInventory();
        ItemStack transferStack = slots[slot].copy();
        transferStack.stackSize = 1;

        for(int i = 0; i < size; i++) {
            ItemStack invStack = inventory.getStackInSlot(i);
            
            if(!inventory.isItemValidForSlot(i, transferStack)) continue;
            
            if(invStack != null) {
                if(ItemStack.areItemStacksEqual(invStack, transferStack) && 
                   ItemStack.areItemStackTagsEqual(invStack, transferStack) && 
                   invStack.stackSize < invStack.getMaxStackSize()) {
                    return transferSingleItem(inventory, slot, i, transferStack);
                }
            } else {
                inventory.setInventorySlotContents(i, transferStack);
                slots[slot].stackSize--;
                if(slots[slot].stackSize <= 0) slots[slot] = null;
                return true;
            }
        }
        return false;
    }
    
    private boolean transferSingleItem(IInventory inventory, int slot, int invSlot, ItemStack transferStack) {
        slots[slot].stackSize--;
        if(slots[slot].stackSize <= 0) slots[slot] = null;
        
        ItemStack invStack = inventory.getStackInSlot(invSlot);
        invStack.stackSize++;
        inventory.setInventorySlotContents(invSlot, invStack);
        return true;
    }
    
    public boolean isMinableOreo(int x, int y, int z) {
        Block b = worldObj.getBlock(x, y, z);
        float hardness = b.getBlockHardness(worldObj, x, y, z);
        return (hardness < 70 && hardness >= 0) || b instanceof IDrillInteraction;
    }
    
    public boolean hasSpace(ItemStack stack) {
        if(stack == null) return true;
        
        for(int i = 1; i < 10; i++) {
            if(slots[i] == null) return true;
        }
        
        ItemStack testStack = stack.copy();
        testStack.stackSize = 1;
        ItemStack[] testArray = slots.clone();
        
        for(int i = 0; i < stack.stackSize; i++) {
            if(!canAddItemToArray(testStack, testArray)) return false;
        }
        return true;
    }
    
    public void addItemToInventory(ItemStack stack) {
        if(stack == null) return;
        
        ItemStack singleStack = stack.copy();
        singleStack.stackSize = 1;
        
        for(int i = 0; i < stack.stackSize; i++) {
            canAddItemToArray(singleStack, this.slots);
        }
    }
    
    public boolean canAddItemToArray(ItemStack stack, ItemStack[] array) {
        if(stack == null) return true;
        
        for(int i = 1; i < 10; i++) {
            if(array[i] != null && 
               array[i].getItem() == stack.getItem() && 
               array[i].stackSize < stack.getMaxStackSize()) {
                array[i].stackSize++;
                return true;
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
    
    public boolean isOutputInventoryFull() {
        for(int i = 1; i < 10; i++) {
            if(slots[i] == null || slots[i].stackSize < slots[i].getMaxStackSize()) {
                return false;
            }
        }
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
    public double getMaxRenderDistanceSquared() {
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