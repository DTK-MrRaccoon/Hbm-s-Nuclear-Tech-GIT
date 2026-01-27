package com.hbm.tileentity.network;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.fauxpointtwelve.DirPos;
import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.fluid.IFluidStandardTransceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityElectricFluidPump extends TileEntityMachineBase implements IEnergyReceiverMK2, IFluidStandardTransceiver {

    public long power;
    public final long maxPower = 2000;
    public FluidTank[] tanks;
    public float rot;
    public float prevRot;
    public float rotSpeed = 0;
    public boolean isActive = false;
    private AudioWrapper audio;

    public TileEntityElectricFluidPump() {
        super(0);
        tanks = new FluidTank[2];
        tanks[0] = new FluidTank(Fluids.NONE, 8000);
        tanks[1] = new FluidTank(Fluids.NONE, 8000);
    }

    @Override
    public String getName() {
        return "container.electricFluidPump";
    }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote) {
            int meta = getBlockMetadata();
            boolean isMain = meta >= 12;
            if (isMain) {
                for (DirPos pos : getConPos()) {
                    this.trySubscribe(worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
                }
                for (DirPos pos : getInputPos()) {
                    this.trySubscribe(tanks[0].getTankType(), worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
                }
                if (tanks[1].getFill() > 0) {
                    for (DirPos pos : getOutputPos()) {
                        this.tryProvide(tanks[1], worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
                    }
                }
                isActive = false;
                int transferRate = 1000;
                int toTransfer = Math.min(tanks[0].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());
                toTransfer = Math.min(toTransfer, transferRate);
                if (toTransfer > 0) {
                    long cost = 50 + (long) (toTransfer * 0.5D);
                    if (power >= cost) {
                        power -= cost;
                        tanks[0].setFill(tanks[0].getFill() - toTransfer);
                        tanks[1].setFill(tanks[1].getFill() + toTransfer);
                        if (tanks[1].getFill() > 0 && tanks[1].getTankType() == Fluids.NONE) {
                            tanks[1].setTankType(tanks[0].getTankType());
                        }
                        isActive = true;
                    }
                }
                if (isActive) {
                    if (rotSpeed < 30) rotSpeed += 2f;
                } else {
                    if (rotSpeed > 0) rotSpeed -= 0.5f;
                }
                if (rotSpeed < 0) rotSpeed = 0;
                this.networkPackNT(20);

                Block topBlock = worldObj.getBlock(xCoord, yCoord + 1, zCoord);
                worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord);
                if (topBlock != null) {
                    worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord + 1, zCoord, topBlock);
                }
                worldObj.markBlockForUpdate(xCoord + 1, yCoord + 1, zCoord);
                worldObj.markBlockForUpdate(xCoord - 1, yCoord + 1, zCoord);
                worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord + 1);
                worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord - 1);
                worldObj.markBlockForUpdate(xCoord, yCoord + 2, zCoord);
            }
        } else {
            prevRot = rot;
            rot += rotSpeed;
            if (rot >= 360) {
                rot -= 360;
                prevRot -= 360;
            }
            if (isActive) {
                if (audio == null) {
                    audio = MainRegistry.proxy.getLoopedSound("hbm:block.turbinegasRunning", xCoord, yCoord, zCoord, 0.5F, 10F, 1.0F);
                    audio.startSound();
                } else if (!audio.isPlaying()) {
                    audio = MainRegistry.proxy.getLoopedSound("hbm:block.turbinegasRunning", xCoord, yCoord, zCoord, 0.5F, 10F, 1.0F);
                    audio.startSound();
                }
                audio.updateVolume(0.5F);
            } else {
                if (audio != null) {
                    audio.stopSound();
                    audio = null;
                }
            }
        }
    }

    public void setFluidType(FluidType type) {
        tanks[0].setTankType(type);
        tanks[1].setTankType(type);
        this.markDirty();
        if (!worldObj.isRemote) {
            Block topBlock = worldObj.getBlock(xCoord, yCoord + 1, zCoord);
            worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord);
            if (topBlock != null) {
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord + 1, zCoord, topBlock);
            }
            worldObj.markBlockForUpdate(xCoord + 1, yCoord + 1, zCoord);
            worldObj.markBlockForUpdate(xCoord - 1, yCoord + 1, zCoord);
            worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord + 1);
            worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord - 1);
            worldObj.markBlockForUpdate(xCoord, yCoord + 2, zCoord);
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isActive);
        buf.writeFloat(rotSpeed);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        isActive = buf.readBoolean();
        rotSpeed = buf.readFloat();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        power = nbt.getLong("power");
        tanks[0].readFromNBT(nbt, "tankIn");
        tanks[1].readFromNBT(nbt, "tankOut");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("power", power);
        tanks[0].writeToNBT(nbt, "tankIn");
        tanks[1].writeToNBT(nbt, "tankOut");
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[0] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[1] };
    }

    protected ForgeDirection rotationFromMeta(int meta) {
        int rot = meta;
        if (meta >= 12) rot = meta - 12;
        else if (meta >= 6) rot = meta - 6;
        if (rot < 0 || rot > 5) rot = 2;
        return ForgeDirection.getOrientation(rot);
    }

    private boolean isHorizontal(ForgeDirection d) {
        return d == ForgeDirection.NORTH || d == ForgeDirection.SOUTH || d == ForgeDirection.EAST || d == ForgeDirection.WEST;
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        int meta = getBlockMetadata();
        ForgeDirection facing = rotationFromMeta(meta);
        ForgeDirection inputSide = facing.getRotation(ForgeDirection.UP);
        ForgeDirection[] checkDirs = new ForgeDirection[] { dir, dir == null ? null : dir.getOpposite() };
        if (meta >= 12) {
            for (ForgeDirection d : checkDirs) {
                if (d == null) continue;
                if (d == ForgeDirection.DOWN) return true;
                if (d == inputSide) return true;
                if (isHorizontal(d)) return true;
            }
            return false;
        }
        for (ForgeDirection d : checkDirs) {
            if (d == null) continue;
            if (d == ForgeDirection.UP) return true;
            if (isHorizontal(d)) return true;
        }
        return false;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return true;
    }

    @Override public void setPower(long i) { this.power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }

    protected DirPos[] getConPos() {
        return new DirPos[] {
            new DirPos(xCoord, yCoord - 1, zCoord, ForgeDirection.UP),
            new DirPos(xCoord, yCoord + 1, zCoord, ForgeDirection.DOWN),
            new DirPos(xCoord + 1, yCoord, zCoord, ForgeDirection.WEST),
            new DirPos(xCoord - 1, yCoord, zCoord, ForgeDirection.EAST),
            new DirPos(xCoord, yCoord, zCoord + 1, ForgeDirection.NORTH),
            new DirPos(xCoord, yCoord, zCoord - 1, ForgeDirection.SOUTH),
            new DirPos(xCoord, yCoord + 2, zCoord, ForgeDirection.DOWN),
            new DirPos(xCoord + 1, yCoord + 1, zCoord, ForgeDirection.WEST),
            new DirPos(xCoord - 1, yCoord + 1, zCoord, ForgeDirection.EAST),
            new DirPos(xCoord, yCoord + 1, zCoord + 1, ForgeDirection.NORTH),
            new DirPos(xCoord, yCoord + 1, zCoord - 1, ForgeDirection.SOUTH)
        };
    }

    protected DirPos[] getInputPos() {
        return new DirPos[] {
            new DirPos(xCoord, yCoord - 1, zCoord, ForgeDirection.UP),
            new DirPos(xCoord + 1, yCoord, zCoord, ForgeDirection.WEST),
            new DirPos(xCoord - 1, yCoord, zCoord, ForgeDirection.EAST),
            new DirPos(xCoord, yCoord, zCoord + 1, ForgeDirection.NORTH),
            new DirPos(xCoord, yCoord, zCoord - 1, ForgeDirection.SOUTH)
        };
    }

    protected DirPos[] getOutputPos() {
        return new DirPos[] {
            new DirPos(xCoord, yCoord + 2, zCoord, ForgeDirection.DOWN),
            new DirPos(xCoord + 1, yCoord + 1, zCoord, ForgeDirection.WEST),
            new DirPos(xCoord - 1, yCoord + 1, zCoord, ForgeDirection.EAST),
            new DirPos(xCoord, yCoord + 1, zCoord + 1, ForgeDirection.NORTH),
            new DirPos(xCoord, yCoord + 1, zCoord - 1, ForgeDirection.SOUTH)
        };
    }

    AxisAlignedBB bb = null;
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) {
            bb = AxisAlignedBB.getBoundingBox(
                xCoord - 0.5, yCoord, zCoord - 0.5,
                xCoord + 1.5, yCoord + 2, zCoord + 1.5
            );
        }
        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }

    @Override
    public void onChunkUnload() {
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }
}
