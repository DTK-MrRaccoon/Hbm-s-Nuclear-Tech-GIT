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
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.Optional.Method;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;

@Optional.InterfaceList({@Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")})
public class TileEntityElectricFluidPump extends TileEntityMachineBase implements IEnergyReceiverMK2, IFluidStandardTransceiver, SimpleComponent {

    public long power;
    public final long maxPower = 10000L;
    public final FluidTank[] tanks = new FluidTank[2];

    public float rot;
    public float prevRot;
    public float rotSpeed = 0f;
    public float targetSpeed = 0f;
    public final float maxRotSpeed = 120f;
    public float maxSpeedLimit = 120f;
    private final float spinUpAccel = 0.0625f;
    private float spinDownAccel = 0.0078125f;

    public boolean isActive = false;

    private AudioWrapper runningAudio;
    private boolean runningSoundPlaying = false;
    private int soundCooldown = 0;

    private final int baseTransferRate = 8000;
    private final long powerAtFullLoad = 2500L;

    public boolean redstoneDisable = false;
    private int idleFlashCooldown = 0;
    private int updateCooldown = 0;

    private final float soundStartThreshold = 3f;
    private float soundCapSpeed = 90f;
    private final float lowSpeedThreshold = 10f;

    public boolean ocForcedOn = false;
    public boolean ocForcedOff = false;
    public boolean ocHasTargetOverride = false;
    public float ocTargetSpeed = 0f;
    private float redstoneBrakeFactor = 16.0f;

    public int ocDebugInfo = 0;

    private final long idlePowerAtMax = 500L;

    private final float noPowerBrakeFactor = 1.0f;

    public TileEntityElectricFluidPump() {
        super(0);
        tanks[0] = new FluidTank(Fluids.NONE, 8000);
        tanks[1] = new FluidTank(Fluids.NONE, 8000);
    }

    @Override
    public String getName() { return "container.electricFluidPump"; }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote) serverTick();
        else clientTick();
    }

    private void serverTick() {
        int meta = getBlockMetadata();
        boolean isMain = meta >= 12;
        if (!isMain) return;

        if (updateCooldown > 0) updateCooldown--;

        for (DirPos pos : getConPos()) trySubscribe(worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());

        for (DirPos pos : getInputPos()) trySubscribe(tanks[0].getTankType(), worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
        for (DirPos pos : getOutputPos()) tryProvide(tanks[1], worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());

        boolean rs = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) ||
                     worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord + 1, zCoord);
        redstoneDisable = rs;

        boolean hasLiquidToMove = tanks[0].getFill() > 0;

        calculateTargetSpeed(hasLiquidToMove);

        updateRotationSpeed(hasLiquidToMove);

        if (rotSpeed > 0f) {
            long idleCost = Math.round(idlePowerAtMax * (rotSpeed / maxRotSpeed));
            if (idleCost > 0) {
                if (power >= idleCost) {
                    power -= idleCost;
                } else {
                    power = 0;
                }
            }
        }

        transferFluid();

        updateVisuals();

        if (updateCooldown <= 0) {
            this.networkPackNT(isActive ? 10 : 20);
            updateCooldown = isActive ? 2 : 4;
        }

        worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord);
        notifyTopNeighbors();
    }

    private void calculateTargetSpeed(boolean hasLiquidToMove) {

        if (ocForcedOff || redstoneDisable) {
            targetSpeed = 0f;
            return;
        }

        if (power <= 0L) {
            targetSpeed = 0f;
            return;
        }

        if (ocForcedOn) {
            if (ocHasTargetOverride) {
                targetSpeed = Math.min(ocTargetSpeed, maxSpeedLimit);
            } else {
                float pf = Math.min(1f, (float) power / (float) maxPower);
                targetSpeed = maxSpeedLimit * pf;
            }
        } else if (ocHasTargetOverride) {
            targetSpeed = Math.min(ocTargetSpeed, maxSpeedLimit);
        } else {
            if (redstoneDisable) {
                targetSpeed = 0f;
            } else if (!hasLiquidToMove) {
                float pf = Math.min(1f, (float) power / (float) maxPower);
                targetSpeed = maxSpeedLimit * pf;
            } else {
                float pf = Math.min(1f, (float) power / (float) maxPower);
                targetSpeed = maxSpeedLimit * pf;
            }
        }

        targetSpeed = Math.max(0f, Math.min(targetSpeed, maxSpeedLimit));
    }

    private void updateRotationSpeed(boolean hasLiquidToMove) {
        float accel = 0f;

        if (targetSpeed > rotSpeed && !ocForcedOff && power > 0) {
            float powerAccelFactor = Math.min(1.0f, (float) power / 4000f);
            accel = spinUpAccel * powerAccelFactor;
            long accelCost = Math.max(0L, (long) (accel * 2L));
            if (accelCost > power) accelCost = power;
            power -= accelCost;
        }

        if (accel > 0) {
            float ap = Math.min(accel, targetSpeed - rotSpeed);
            rotSpeed += ap;
        } else if (rotSpeed > targetSpeed) {
            float brakeMultiplier = 1.0f;
            if (redstoneDisable || ocForcedOff) {
                brakeMultiplier = redstoneBrakeFactor;
            } else if (power <= 0L) {
                brakeMultiplier = noPowerBrakeFactor;
            }

            float decelBase = spinDownAccel * brakeMultiplier;
            float decel = Math.min(decelBase, rotSpeed - targetSpeed);
            rotSpeed -= decel;
        }

        rotSpeed = Math.max(0f, Math.min(rotSpeed, maxSpeedLimit));
    }

    private void transferFluid() {
        float speedRatio = rotSpeed / maxRotSpeed;
        float mappedRatio = (rotSpeed <= lowSpeedThreshold) ? speedRatio * speedRatio : speedRatio;
        int transferCapacity = Math.round(baseTransferRate * mappedRatio);
        int availableOutputSpace = tanks[1].getMaxFill() - tanks[1].getFill();
        int toTransfer = Math.min(Math.min(tanks[0].getFill(), availableOutputSpace), transferCapacity);

        if (idleFlashCooldown > 0) idleFlashCooldown--;

        if (toTransfer > 0 && rotSpeed > 3f) {
            tanks[0].setFill(tanks[0].getFill() - toTransfer);
            tanks[1].setFill(tanks[1].getFill() + toTransfer);
            if (tanks[1].getFill() > 0 && tanks[1].getTankType() == Fluids.NONE) {
                tanks[1].setTankType(tanks[0].getTankType());
            }

            isActive = true;
            idleFlashCooldown = 5;

            if (power > 0) {
                float loadDecel = toTransfer * 0.00002f;
                long moveCost = Math.round((double)toTransfer / (double)baseTransferRate * powerAtFullLoad * (0.5 + 0.5 * Math.min(1.0, speedRatio)));

                if (moveCost <= power) {
                    power -= moveCost;
                    loadDecel *= 0.05f;
                } else {
                    float payRatio = (float) power / (float) moveCost;
                    power = 0;
                    loadDecel *= (1.1f - payRatio);
                }

                rotSpeed = Math.max(0f, rotSpeed - loadDecel);
            }
        } else {
            if (idleFlashCooldown <= 0) isActive = false;
        }
    }

    private void updateVisuals() {
        prevRot = rot;
        rot += rotSpeed;
        if (rot >= 360f) {
            rot -= 360f;
            prevRot -= 360f;
        }
    }


    @SideOnly(Side.CLIENT)
    private void clientTick() {
        boolean runningNow = rotSpeed > soundStartThreshold;
        if (soundCooldown > 0) soundCooldown--;

        if (runningNow && !runningSoundPlaying) {
            if (runningAudio == null) {
                runningAudio = MainRegistry.proxy.getLoopedSound("hbm:block.turbinegasRunning", xCoord, yCoord, zCoord, 0.002F, 16F, 0.4F);
                runningAudio.startSound();
            }
            runningSoundPlaying = true;
        } else if (!runningNow && runningSoundPlaying) {
            stopSound();
        } else if (runningNow && runningSoundPlaying && runningAudio != null) {
            updateSound();
        }

        prevRot = rot;
        rot += rotSpeed;
        if (rot >= 360f) {
            rot -= 360f;
            prevRot -= 360f;
        }
    }

    private void updateSound() {
        float capped = Math.max(0f, Math.min(soundCapSpeed, rotSpeed));
        float norm = capped / soundCapSpeed;
        float curve = (float) Math.pow(norm, 1.8);
        float minVol = 0.002f;
        float maxVol = 0.22f;
        float volume = minVol + curve * (maxVol - minVol);
        float minPitch = 0.4f;
        float maxPitch = 1.2f;
        float pitch = minPitch + norm * (maxPitch - minPitch);
        runningAudio.updatePitch(pitch);
        runningAudio.updateVolume(volume);
        runningAudio.keepAlive();
    }

    public void setFluidType(FluidType type) {
        tanks[0].setTankType(type);
        tanks[1].setTankType(type);
        this.markDirty();
        if (!worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord);
            notifyTopNeighbors();
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isActive);
        buf.writeFloat(rotSpeed);
        buf.writeFloat(rot);
        buf.writeBoolean(redstoneDisable);
        buf.writeFloat(maxSpeedLimit);
        buf.writeBoolean(ocForcedOn);
        buf.writeBoolean(ocForcedOff);
        buf.writeBoolean(ocHasTargetOverride);
        buf.writeFloat(ocTargetSpeed);
        buf.writeInt(ocDebugInfo);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        isActive = buf.readBoolean();
        rotSpeed = buf.readFloat();
        rot = buf.readFloat();
        redstoneDisable = buf.readBoolean();
        maxSpeedLimit = buf.readFloat();
        ocForcedOn = buf.readBoolean();
        ocForcedOff = buf.readBoolean();
        ocHasTargetOverride = buf.readBoolean();
        ocTargetSpeed = buf.readFloat();
        ocDebugInfo = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        power = nbt.getLong("power");
        rotSpeed = nbt.getFloat("rotSpeed");
        rot = nbt.getFloat("rot");
        redstoneDisable = nbt.getBoolean("rsDisable");
        maxSpeedLimit = nbt.getFloat("maxSpeedLimit");
        ocForcedOn = nbt.getBoolean("ocForcedOn");
        ocForcedOff = nbt.getBoolean("ocForcedOff");
        ocHasTargetOverride = nbt.getBoolean("ocHasTargetOverride");
        ocTargetSpeed = nbt.getFloat("ocTargetSpeed");
        redstoneBrakeFactor = nbt.getFloat("redstoneBrakeFactor");
        if (nbt.hasKey("ocDebugInfo")) ocDebugInfo = nbt.getInteger("ocDebugInfo");
        tanks[0].readFromNBT(nbt, "tankIn");
        tanks[1].readFromNBT(nbt, "tankOut");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("power", power);
        nbt.setFloat("rotSpeed", rotSpeed);
        nbt.setFloat("rot", rot);
        nbt.setBoolean("rsDisable", redstoneDisable);
        nbt.setFloat("maxSpeedLimit", maxSpeedLimit);
        nbt.setBoolean("ocForcedOn", ocForcedOn);
        nbt.setBoolean("ocForcedOff", ocForcedOff);
        nbt.setBoolean("ocHasTargetOverride", ocHasTargetOverride);
        nbt.setFloat("ocTargetSpeed", ocTargetSpeed);
        nbt.setFloat("redstoneBrakeFactor", redstoneBrakeFactor);
        nbt.setInteger("ocDebugInfo", ocDebugInfo);
        tanks[0].writeToNBT(nbt, "tankIn");
        tanks[1].writeToNBT(nbt, "tankOut");
    }

    @Override
    public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }

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
            new DirPos(xCoord, yCoord + 2, zCoord, ForgeDirection.DOWN)
        };
    }

    protected DirPos[] getInputPos() {
        return new DirPos[] {
            new DirPos(xCoord, yCoord - 1, zCoord, ForgeDirection.UP),
            new DirPos(xCoord + 1, yCoord, zCoord, ForgeDirection.WEST),
            new DirPos(xCoord - 1, yCoord, zCoord, ForgeDirection.EAST),
            new DirPos(xCoord, yCoord, zCoord + 1, ForgeDirection.NORTH),
            new DirPos(xCoord, yCoord, zCoord - 1, ForgeDirection.SOUTH),
            new DirPos(xCoord, yCoord + 1, zCoord, ForgeDirection.UP),
            new DirPos(xCoord + 1, yCoord + 1, zCoord, ForgeDirection.WEST),
            new DirPos(xCoord - 1, yCoord + 1, zCoord, ForgeDirection.EAST),
            new DirPos(xCoord, yCoord + 1, zCoord + 1, ForgeDirection.NORTH),
            new DirPos(xCoord, yCoord + 1, zCoord - 1, ForgeDirection.SOUTH)
        };
    }

    protected DirPos[] getOutputPos() {
        return new DirPos[] {
            new DirPos(xCoord, yCoord + 2, zCoord, ForgeDirection.DOWN),
            new DirPos(xCoord + 1, yCoord + 1, zCoord, ForgeDirection.WEST),
            new DirPos(xCoord - 1, yCoord + 1, zCoord, ForgeDirection.EAST),
            new DirPos(xCoord, yCoord + 1, zCoord + 1, ForgeDirection.NORTH),
            new DirPos(xCoord, yCoord + 1, zCoord - 1, ForgeDirection.SOUTH),
            new DirPos(xCoord + 1, yCoord + 2, zCoord, ForgeDirection.WEST),
            new DirPos(xCoord - 1, yCoord + 2, zCoord, ForgeDirection.EAST),
            new DirPos(xCoord, yCoord + 2, zCoord + 1, ForgeDirection.NORTH),
            new DirPos(xCoord, yCoord + 2, zCoord - 1, ForgeDirection.SOUTH)
        };
    }

    AxisAlignedBB bb = null;
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) {
            bb = AxisAlignedBB.getBoundingBox(xCoord - 0.5, yCoord, zCoord - 0.5, xCoord + 1.5, yCoord + 2, zCoord + 1.5);
        }
        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() { return 65536.0D; }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        stopSound();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        stopSound();
    }

    private void stopSound() {
        if (runningAudio != null) {
            runningAudio.stopSound();
            runningAudio = null;
        }
        runningSoundPlaying = false;
    }

    private void notifyTopNeighbors() {
        Block topBlock = worldObj.getBlock(xCoord, yCoord + 1, zCoord);
        if (topBlock != null) {
            worldObj.notifyBlocksOfNeighborChange(xCoord + 1, yCoord + 1, zCoord, topBlock);
            worldObj.notifyBlocksOfNeighborChange(xCoord - 1, yCoord + 1, zCoord, topBlock);
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord + 1, zCoord + 1, topBlock);
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord + 1, zCoord - 1, topBlock);
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord + 2, zCoord, topBlock);
        }
    }

    @Override
    @Optional.Method(modid = "OpenComputers")
    public String getComponentName() {
        return "electric_fluid_pump";
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] setDebugInfo(Context context, Arguments args) {
        if (args.count() > 0) ocDebugInfo = Math.max(0, (int) args.checkDouble(0));
        else ocDebugInfo = 1;
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] start(Context context, Arguments args) {
        ocForcedOff = false;
        ocForcedOn = true;
        if (args.count() > 0) {
            float v = (float) args.checkDouble(0);
            ocTargetSpeed = Math.max(0f, Math.min(v, maxRotSpeed));
            ocHasTargetOverride = true;
        } else ocHasTargetOverride = false;
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] stop(Context context, Arguments args) {
        ocForcedOn = false;
        ocForcedOff = true;
        ocHasTargetOverride = false;
        ocTargetSpeed = 0f;
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] clear(Context context, Arguments args) {
        ocForcedOn = false;
        ocForcedOff = false;
        ocHasTargetOverride = false;
        ocTargetSpeed = 0f;
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] setSpeed(Context context, Arguments args) {
        float val = (float) args.checkDouble(0);
        ocTargetSpeed = Math.max(0f, Math.min(val, maxSpeedLimit));
        ocHasTargetOverride = true;
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getSpeed(Context context, Arguments args) {
        return new Object[] { rotSpeed, targetSpeed };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getTankIn(Context context, Arguments args) {
        return new Object[] { tanks[0].getFill(), tanks[0].getMaxFill(), tanks[0].getTankType().getLocalizedName() };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getTankOut(Context context, Arguments args) {
        return new Object[] { tanks[1].getFill(), tanks[1].getMaxFill(), tanks[1].getTankType().getLocalizedName() };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getPowerInfo(Context context, Arguments args) {
        return new Object[] { power, maxPower };
    }

    @Optional.Method(modid = "OpenComputers")
    public String[] methods() {
        return new String[] {
            "setDebugInfo",
            "start",
            "stop",
            "clear",
            "setSpeed",
            "getSpeed",
            "getTankIn",
            "getTankOut",
            "getPowerInfo"
        };
    }

    @Optional.Method(modid = "OpenComputers")
    public Object[] invoke(String method, Context context, Arguments args) throws Exception {
        switch(method) {
            case "setDebugInfo": return setDebugInfo(context, args);
            case "start": return start(context, args);
            case "stop": return stop(context, args);
            case "clear": return clear(context, args);
            case "setSpeed": return setSpeed(context, args);
            case "getSpeed": return getSpeed(context, args);
            case "getTankIn": return getTankIn(context, args);
            case "getTankOut": return getTankOut(context, args);
            case "getPowerInfo": return getPowerInfo(context, args);
            default: throw new NoSuchMethodException();
        }
    }
}
