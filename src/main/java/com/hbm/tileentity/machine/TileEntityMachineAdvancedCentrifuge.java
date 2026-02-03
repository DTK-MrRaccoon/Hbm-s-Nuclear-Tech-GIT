package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineAdvancedCentrifuge;
import com.hbm.inventory.gui.GUIMachineAdvancedCentrifuge;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
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
import api.hbm.energymk2.IBatteryItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashMap;
import java.util.List;

public class TileEntityMachineAdvancedCentrifuge extends TileEntityMachineBase implements IEnergyReceiverMK2, ISidedInventory, IGUIProvider, IUpgradeInfoProvider {

    public long power;
    public static final long maxPower = 200000;

    public int progress;
    public int maxProgress = 200;
    public boolean isProgressing;

    private AudioWrapper audio;
    private int audioDuration = 0;

    int consumption = 5000;
    int speed = 100;

    public UpgradeManagerNT upgradeManager = new UpgradeManagerNT();

    public int[] clientInputCounts = new int[4];

    private static final int[] ALL_SLOTS;
    static {
        ALL_SLOTS = new int[23];
        for (int i = 0; i < 23; i++) ALL_SLOTS[i] = i;
    }

    public TileEntityMachineAdvancedCentrifuge() {
        super(23);
    }

    @Override
    public String getName() {
        return "container.advancedCentrifuge";
    }

    @Override
    public void updateEntity() {
        if(!worldObj.isRemote) {

            for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
                this.trySubscribe(worldObj, xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir);

            this.speed = 100;
            this.consumption = 100;

            this.isProgressing = false;
            this.power = Library.chargeTEFromItems(slots, 4, power, maxPower);

            upgradeManager.checkSlots(this, slots, 21, 22);

            int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
            int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
            int overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

            this.speed -= speedLevel * 25;
            this.consumption += speedLevel * 300;
            this.speed += powerLevel * 5;
            this.consumption -= powerLevel * 20;
            this.speed /= (overLevel + 1);
            this.consumption *= (overLevel + 1);

            if(this.speed <= 0) {
                this.speed = 1;
            }

            if(worldObj.getTotalWorldTime() % 20 == 0) {
                updateConnections();
            }

            boolean canProcessAny = false;
            for(int slot = 0; slot < 4; slot++) {
                if(canProcess(slot)) {
                    canProcessAny = true;
                    break;
                }
            }

            if(!canProcessAny) {
                this.progress = 0;
            } else {
                isProgressing = true;
                process();
            }

            for (int i = 0; i < 4; i++) {
                clientInputCounts[i] = (slots[i] == null) ? 0 : slots[i].stackSize;
            }

            this.networkPackNT(150);
        } else {
            if(isProgressing) {
                audioDuration += 2;
            } else {
                audioDuration -= 3;
            }

            if (audioDuration < 0) audioDuration = 0;
            if (audioDuration > 60) audioDuration = 60;

            if(audioDuration > 10 && MainRegistry.proxy.me().getDistance(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 25) {
                if(audio == null) {
                    audio = createAudioLoop();
                    audio.startSound();
                } else if(!audio.isPlaying()) {
                    audio = rebootAudio(audio);
                }
                audio.updateVolume(getVolume(1F));
                audio.updatePitch((audioDuration - 10) / 100F + 0.5F);
                audio.keepAlive();
            } else {
                if(audio != null) {
                    audio.stopSound();
                    audio = null;
                }
            }
        }
    }

    private void updateConnections() {
        for(DirPos pos : getConPos()) {
            this.trySubscribe(worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
        }
    }

    private DirPos[] getConPos() {
        ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);

        return new DirPos[] {
            new DirPos(this.xCoord - dir.offsetX * 2, this.yCoord, this.zCoord - dir.offsetZ * 2, dir.getOpposite()),
            new DirPos(this.xCoord - dir.offsetX * 2 + rot.offsetX, this.yCoord, this.zCoord - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
            new DirPos(this.xCoord + dir.offsetX, this.yCoord, this.zCoord + dir.offsetZ, dir),
            new DirPos(this.xCoord + dir.offsetX + rot.offsetX, this.yCoord, this.zCoord + dir.offsetZ + rot.offsetZ, dir),
            new DirPos(this.xCoord - rot.offsetX, this.yCoord, this.zCoord - rot.offsetZ, rot.getOpposite()),
            new DirPos(this.xCoord - dir.offsetX - rot.offsetX, this.yCoord, this.zCoord - dir.offsetZ - rot.offsetZ, rot.getOpposite()),
            new DirPos(this.xCoord + rot.offsetX * 2, this.yCoord, this.zCoord + rot.offsetZ * 2, rot),
            new DirPos(this.xCoord - dir.offsetX + rot.offsetX * 2, this.yCoord, this.zCoord - dir.offsetZ + rot.offsetZ * 2, rot),
        };
    }

    public boolean hasPower() {
        return this.power > 0;
    }

    public int getPowerRemainingScaled(int scale) {
        return (int) (this.power * scale / this.maxPower);
    }

    public int getProgressScaled(int scale) {
        if (this.maxProgress <= 0) return 0;
        return this.progress * scale / this.maxProgress;
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return MainRegistry.proxy.getLoopedSound("hbm:block.centrifugeOperate", xCoord, yCoord, zCoord, 1.0F, 10F, 1.0F, 20);
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

    private boolean canProcess(int inputSlot) {
        if(slots[inputSlot] == null) return false;

        ItemStack[] output = CentrifugeRecipes.getOutput(slots[inputSlot]);
        if(output == null) return false;

        if(this.power < this.consumption) return false;

        int outputStart = 5 + (inputSlot * 4);
        for(int i = 0; i < 4; i++) {
            if(output[i] != null) {
                if(!hasSpaceForItem(output[i].copy(), outputStart + i)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean hasSpaceForItem(ItemStack stack, int slot) {
        if(stack == null) return true;
        if(slots[slot] == null) return true;
        if(!slots[slot].isItemEqual(stack)) return false;
        if(slots[slot].stackSize + stack.stackSize <= slots[slot].getMaxStackSize()) return true;
        return false;
    }

    private void process() {
        if(this.power < this.consumption) return;

        this.power -= this.consumption;
        this.progress++;

        this.maxProgress = 200 * this.speed / 100;
        if(this.maxProgress <= 0) this.maxProgress = 1;

        if(this.progress >= this.maxProgress) {
            for(int inputSlot = 0; inputSlot < 4; inputSlot++) {
                if(slots[inputSlot] != null && canProcess(inputSlot)) {
                    ItemStack[] output = CentrifugeRecipes.getOutput(slots[inputSlot]);
                    if(output == null) continue;

                    int outputStart = 5 + (inputSlot * 4);

                    for(int i = 0; i < 4; i++) {
                        if(output[i] != null) {
                            ItemStack out = output[i].copy();
                            if(slots[outputStart + i] == null) {
                                slots[outputStart + i] = out;
                            } else if(slots[outputStart + i].isItemEqual(out)) {
                                slots[outputStart + i].stackSize += out.stackSize;
                            }
                        }
                    }

                    slots[inputSlot].stackSize--;
                    if(slots[inputSlot].stackSize <= 0) {
                        slots[inputSlot] = null;
                    }
                }
            }

            this.progress = 0;
            this.markDirty();
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        if(i >= 0 && i <= 3) {
            return CentrifugeRecipes.getOutput(itemStack) != null;
        }
        if(i == 4) {
            return itemStack != null && (itemStack.getItem() instanceof IBatteryItem || itemStack.getItem() == ModItems.battery_creative);
        }
        if(i >= 21 && i <= 22) {
            return itemStack != null && itemStack.getItem() instanceof ItemMachineUpgrade;
        }
        return false;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        if(slot >= 0 && slot <= 3) return true;
        if(slot == 4) return true;
        if(slot >= 21 && slot <= 22) return true;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot >= 5 && slot <= 20;
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeInt(maxProgress);
        buf.writeBoolean(isProgressing);

        for (int i = 0; i < 4; i++) {
            buf.writeInt(clientInputCounts[i]);
        }
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        maxProgress = buf.readInt();
        isProgressing = buf.readBoolean();

        for (int i = 0; i < 4; i++) {
            clientInputCounts[i] = buf.readInt();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.power = nbt.getLong("power");
        this.progress = nbt.getInteger("progress");
        this.maxProgress = nbt.getInteger("maxProgress");
        this.isProgressing = nbt.getBoolean("isProgressing");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("power", power);
        nbt.setInteger("progress", progress);
        nbt.setInteger("maxProgress", maxProgress);
        nbt.setBoolean("isProgressing", isProgressing);
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
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerMachineAdvancedCentrifuge(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIMachineAdvancedCentrifuge(player.inventory, this);
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<String> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(com.hbm.blocks.ModBlocks.machine_advanced_centrifuge));
        if(type == UpgradeType.SPEED) {
            info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("desc.advancedcentrifuge.speed", "-" + (level * 25) + "%"));
            info.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.advancedcentrifuge.consumption", "+" + (level * 300) + "%"));
        }
        if(type == UpgradeType.POWER) {
            info.add(EnumChatFormatting.GREEN + I18nUtil.resolveKey("desc.advancedcentrifuge.consumption", "-" + (level * 20) + "%"));
            info.add(EnumChatFormatting.RED + I18nUtil.resolveKey("desc.advancedcentrifuge.speed", "+" + (level * 5) + "%"));
        }
        if(type == UpgradeType.OVERDRIVE) {
            info.add((BobMathUtil.getBlink() ? EnumChatFormatting.RED : EnumChatFormatting.DARK_GRAY) + "OVERDRIVE");
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }
}
