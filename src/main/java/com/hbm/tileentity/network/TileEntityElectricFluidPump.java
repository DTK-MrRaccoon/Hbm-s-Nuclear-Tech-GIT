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
    private final float spinUpAccel = 0.0625f;
    private float spinDownAccel = 0.0078125f;

    public boolean isActive = false;

    private AudioWrapper runningAudio;
    private boolean runningSoundPlaying = false;
    private int soundCooldown = 0;

    private final int baseTransferRate = 8000; // mB/tick max
    private final long powerAtFullLoad = 2500L;

    public boolean redstoneDisable = false;
    private int idleFlashCooldown = 0;
    private int updateCooldown = 0;

    private final float soundStartThreshold = 3f;
    private float soundCapSpeed = 90f;
    private final float lowSpeedThreshold = 10f;

    private boolean ocForcedOn = false;
    private boolean ocHasTargetOverride = false;
    private float ocTargetSpeed = 0f;
    private float redstoneBrakeFactor = 8.0f;

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

        if (ocForcedOn) {
            if (ocHasTargetOverride) {
                targetSpeed = clamp(ocTargetSpeed, 0f, maxRotSpeed);
            } else if (power > 0) {
                float pf = Math.min(1f, (float) power / (float) maxPower);
                targetSpeed = maxRotSpeed * pf;
            } else {
                targetSpeed = Math.max(rotSpeed * 0.995f, 1f);
            }
        } else if (ocHasTargetOverride) {
            targetSpeed = clamp(ocTargetSpeed, 0f, maxRotSpeed);
        } else {
            if (redstoneDisable || !hasLiquidToMove) targetSpeed = 0f;
            else if (power > 0) {
                float pf = Math.min(1f, (float) power / (float) maxPower);
                targetSpeed = maxRotSpeed * pf;
            } else {
                targetSpeed = Math.max(rotSpeed * 0.995f, 1f);
            }
            if (power >= maxPower && hasLiquidToMove && !redstoneDisable) targetSpeed = maxRotSpeed;
        }

        float accel = 0f;
        if (targetSpeed > rotSpeed && !(redstoneDisable && !ocForcedOn)) {
            if (power > 0) {
                float powerAccelFactor = Math.min(1.0f, (float) power / 4000f);
                accel = spinUpAccel * powerAccelFactor;
                long accelCost = Math.max(0L, (long) (accel * 2L));
                if (accelCost > power) accelCost = power;
                power -= accelCost;
            } else if (hasLiquidToMove) accel = 0.002f;
        }

        if (accel > 0) {
            float ap = Math.min(accel, targetSpeed - rotSpeed);
            rotSpeed += ap;
        } else if (rotSpeed > targetSpeed) {
            float decel = Math.min(adjustedSpinDown(), rotSpeed - targetSpeed);
            rotSpeed -= decel;
        }

        if (rotSpeed > 0) rotSpeed = Math.max(0f, rotSpeed - 0.00125f);
        rotSpeed = clamp(rotSpeed, 0f, maxRotSpeed);
        if (rotSpeed > maxRotSpeed - 0.1f && rotSpeed < maxRotSpeed) rotSpeed = maxRotSpeed;

        float speedRatio = rotSpeed / maxRotSpeed;
        float mappedRatio;
        if (rotSpeed <= lowSpeedThreshold) mappedRatio = speedRatio * speedRatio;
        else mappedRatio = speedRatio;

        int transferCapacity = Math.round(baseTransferRate * mappedRatio);
        int availableOutputSpace = tanks[1].getMaxFill() - tanks[1].getFill();
        int toTransfer = Math.min(Math.min(tanks[0].getFill(), availableOutputSpace), transferCapacity);

        long moveCost = 0L;
        if (baseTransferRate > 0 && toTransfer > 0) {
            double baseCostRatio = (double) toTransfer / (double) baseTransferRate;
            double speedFactor = 0.5 + 0.5 * Math.max(0.0, Math.min(1.0, speedRatio));
            moveCost = Math.round(baseCostRatio * powerAtFullLoad * speedFactor);
        }

        if (idleFlashCooldown > 0) idleFlashCooldown--;

        boolean wasActiveLastTick = isActive;

        if (toTransfer > 0 && rotSpeed > 3f) {
            tanks[0].setFill(tanks[0].getFill() - toTransfer);
            tanks[1].setFill(tanks[1].getFill() + toTransfer);
            if (tanks[1].getFill() > 0 && tanks[1].getTankType() == Fluids.NONE) tanks[1].setTankType(tanks[0].getTankType());

            isActive = true;
            idleFlashCooldown = 5;

            float loadDecel = toTransfer * 0.00002f;
            if (power > 0 && moveCost > 0 && !redstoneDisable) {
                if (moveCost <= power) {
                    power -= moveCost;
                    loadDecel *= 0.05f;
                } else {
                    float payRatio = (float) power / (float) moveCost;
                    power = 0;
                    loadDecel *= (1.1f - payRatio);
                }
            } else {
                loadDecel *= 1.1f;
            }

            rotSpeed = Math.max(0f, rotSpeed - loadDecel);
        } else {
            if (idleFlashCooldown <= 0) isActive = false;
        }

        if (rotSpeed > 0 && (power <= 0 || redstoneDisable) && !(ocForcedOn && ocHasTargetOverride)) {
            float naturalDecay = adjustedSpinDown() * 0.04f;
            if (toTransfer > 0) naturalDecay += toTransfer * 0.000005f;
            rotSpeed = Math.max(0f, rotSpeed - naturalDecay);
        }

        prevRot = rot;
        rot += rotSpeed;
        if (rot >= 360f) { rot -= 360f; prevRot -= 360f; }

        if (isActive && updateCooldown <= 0) {
            this.networkPackNT(10);
            updateCooldown = 2;
        } else if (updateCooldown <= 0) {
            this.networkPackNT(20);
            updateCooldown = 4;
        }

        if (isActive || wasActiveLastTick || updateCooldown == 0) {
            worldObj.markBlockForUpdate(xCoord, yCoord + 1, zCoord);
            notifyTopNeighbors();
        }
    }

    private float adjustedSpinDown() {
        if (redstoneDisable && !ocForcedOn) return spinDownAccel * redstoneBrakeFactor;
        return spinDownAccel;
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
            if (runningAudio != null) { runningAudio.stopSound(); runningAudio = null; }
            runningSoundPlaying = false;
        } else if (runningNow && runningSoundPlaying && runningAudio != null) {
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

        prevRot = rot;
        rot += rotSpeed;
        if (rot >= 360f) { rot -= 360f; prevRot -= 360f; }
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
        tanks[0].readFromNBT(nbt, "tankIn");
        tanks[1].readFromNBT(nbt, "tankOut");
        ocForcedOn = nbt.getBoolean("ocForcedOn");
        ocHasTargetOverride = nbt.getBoolean("ocHasTargetOverride");
        ocTargetSpeed = nbt.getFloat("ocTargetSpeed");
        redstoneBrakeFactor = nbt.getFloat("redstoneBrakeFactor");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("power", power);
        nbt.setFloat("rotSpeed", rotSpeed);
        nbt.setFloat("rot", rot);
        nbt.setBoolean("rsDisable", redstoneDisable);
        tanks[0].writeToNBT(nbt, "tankIn");
        tanks[1].writeToNBT(nbt, "tankOut");
        nbt.setBoolean("ocForcedOn", ocForcedOn);
        nbt.setBoolean("ocHasTargetOverride", ocHasTargetOverride);
        nbt.setFloat("ocTargetSpeed", ocTargetSpeed);
        nbt.setFloat("redstoneBrakeFactor", redstoneBrakeFactor);
    }

    @Override
    public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }

    protected ForgeDirection rotationFromMeta(int meta) {
        int r = meta >= 12 ? meta - 12 : (meta >= 6 ? meta - 6 : meta);
        if (r < 0 || r > 5) r = 2;
        return ForgeDirection.getOrientation(r);
    }

    private boolean isHorizontal(ForgeDirection d) {
        return d == ForgeDirection.NORTH || d == ForgeDirection.SOUTH || d == ForgeDirection.EAST || d == ForgeDirection.WEST;
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
            bb = AxisAlignedBB.getBoundingBox(xCoord - 0.5, yCoord, zCoord - 0.5, xCoord + 1.5, yCoord + 2, zCoord + 1.5);
        }
        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() { return 65536.0D; }

    @Override
    public void onChunkUnload() { super.onChunkUnload(); stopSoundIfPlaying(); }

    @Override
    public void invalidate() { super.invalidate(); stopSoundIfPlaying(); }

    private void stopSoundIfPlaying() {
        if (runningAudio != null) { runningAudio.stopSound(); runningAudio = null; }
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

    private float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }

    // ----------------------------
    // OpenComputers integration
    // ----------------------------

    @Override
    @Optional.Method(modid = "OpenComputers")
    public String getComponentName() {
        return "electric_fluid_pump";
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getSpeed(Context context, Arguments args) {
        return new Object[] { rotSpeed };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getTargetSpeed(Context context, Arguments args) {
        return new Object[] { targetSpeed, ocHasTargetOverride ? true : false };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] setTargetSpeed(Context context, Arguments args) {
        double val = args.checkDouble(0);
        ocTargetSpeed = clamp((float) val, 0f, maxRotSpeed);
        ocHasTargetOverride = true;
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] clearTargetSpeed(Context context, Arguments args) {
        ocHasTargetOverride = false;
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] start(Context context, Arguments args) {
        ocForcedOn = true;
        if (args.count() > 0) {
            double v = args.checkDouble(0);
            ocTargetSpeed = clamp((float) v, 0f, maxRotSpeed);
            ocHasTargetOverride = true;
        }
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] stop(Context context, Arguments args) {
        ocForcedOn = false;
        ocHasTargetOverride = false;
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] isActive(Context context, Arguments args) {
        return new Object[] { isActive };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getInputFill(Context context, Arguments args) {
        return new Object[] { tanks[0].getFill(), tanks[0].getMaxFill(), tanks[0].getTankType().getLocalizedName() };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getOutputFill(Context context, Arguments args) {
        return new Object[] { tanks[1].getFill(), tanks[1].getMaxFill(), tanks[1].getTankType().getLocalizedName() };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getPowerInfo(Context context, Arguments args) {
        return new Object[] { power, maxPower };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getTransferCapacity(Context context, Arguments args) {
        float ratio = rotSpeed / maxRotSpeed;
        float mapped = (rotSpeed <= lowSpeedThreshold) ? ratio * ratio : ratio;
        int cap = Math.round(baseTransferRate * mapped);
        return new Object[] { cap, baseTransferRate };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getSpinUpTime(Context context, Arguments args) {
        float desired = args.count() > 0 ? (float) args.checkDouble(0) : targetSpeed;
        desired = clamp(desired, 0f, maxRotSpeed);
        if (desired <= rotSpeed) return new Object[] { 0 };
        float powerFactor = Math.min(1f, (float) power / 4000f);
        float effectiveAccel = (power > 0) ? spinUpAccel * powerFactor : 0.002f;
        if (effectiveAccel <= 0) return new Object[] { Float.POSITIVE_INFINITY };
        float ticks = (desired - rotSpeed) / effectiveAccel;
        return new Object[] { Math.max(0, Math.round(ticks)) };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getSpinDownTime(Context context, Arguments args) {
        float effectiveDecel = adjustedSpinDown();
        if (effectiveDecel <= 0) return new Object[] { Float.POSITIVE_INFINITY };
        float ticks = rotSpeed / effectiveDecel;
        return new Object[] { Math.max(0, Math.round(ticks)) };
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] setRedstoneBrakeFactor(Context context, Arguments args) {
        double f = args.checkDouble(0);
        redstoneBrakeFactor = Math.max(1.0f, (float) f);
        return new Object[] {};
    }

    @Optional.Method(modid = "OpenComputers")
    @Callback(direct = true)
    public Object[] getInfo(Context context, Arguments args) {
        return new Object[] {
            rotSpeed,
            targetSpeed,
            tanks[0].getFill(), tanks[0].getMaxFill(), tanks[0].getTankType().getLocalizedName(),
            tanks[1].getFill(), tanks[1].getMaxFill(), tanks[1].getTankType().getLocalizedName(),
            power, maxPower, isActive
        };
    }
}