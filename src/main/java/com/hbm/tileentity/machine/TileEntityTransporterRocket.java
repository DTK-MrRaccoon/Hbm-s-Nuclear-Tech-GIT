package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.inventory.container.ContainerTransporterRocket;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.gui.GUITransporterRocket;
import com.hbm.util.ParticleUtil;
import com.hbm.util.fauxpointtwelve.DirPos;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityTransporterRocket extends TileEntityTransporterBase {

    public boolean hasRocket = true;
    public int launchTicks = 0;
    public int threshold = 0;
    private final int MASS_MULT = 25;

    public TileEntityTransporterRocket() {
        super(16, 8, 128_000, 0, 2, 64_000);
        tanks[8].setTankType(Fluids.KEROSENE);
        tanks[9].setTankType(Fluids.OXYGEN);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if(!worldObj.isRemote && linkedTransporter instanceof TileEntityTransporterRocket) {
            if(hasRocket == ((TileEntityTransporterRocket)linkedTransporter).hasRocket) {
                hasRocket = !hasRocket;
            }
        }

        FluidType fuel = tanks[8].getTankType();
        if(fuel != Fluids.KEROSENE && fuel != Fluids.ETHANOL && fuel != Fluids.HYDROGEN) {
            tanks[8].setTankType(Fluids.KEROSENE);
        }

        FluidType oxidizer = tanks[9].getTankType();
        if(oxidizer != Fluids.OXYGEN && oxidizer != Fluids.PEROXIDE) {
            tanks[9].setTankType(Fluids.OXYGEN);
        }

        launchTicks = MathHelper.clamp_int(launchTicks + (hasRocket ? -1 : 1), hasRocket ? -20 : 0, 100);

        if(worldObj.isRemote && launchTicks > 0 && launchTicks < 100) {
            ParticleUtil.spawnGasFlame(worldObj, xCoord + 0.5, yCoord + 0.5 + launchTicks, zCoord + 0.5, 0.0, -1.0, 0.0);
            if(launchTicks < 10) {
                ExplosionLarge.spawnShock(worldObj, xCoord + 0.5, yCoord, zCoord + 0.5, 1 + worldObj.rand.nextInt(3), 1 + worldObj.rand.nextGaussian());
            }
        }
    }

    public int getThreshold() {
        return threshold == 0 ? 0 : (int)Math.pow(2, threshold - 1);
    }

    public int getTotalMass() {
        return itemCount();
    }

    public double getEfficiency() {
        FluidType fuel = tanks[8].getTankType();
        if(fuel == Fluids.KEROSENE) return 1.0;
        if(fuel == Fluids.ETHANOL) return 1.5;
        if(fuel == Fluids.HYDROGEN) return 2.0;
        return 1.0;
    }

    public int getSendCost() {
        int mass = getTotalMass();
        double eff = getEfficiency();
        int effectiveMass = Math.max(1, mass);
        int cost = (int)(effectiveMass * MASS_MULT * eff);
        return cost;
    }

    private int getFuelCost() {
        return getSendCost();
    }

    private int getOxidizerCost() {
        return (int)Math.ceil(getSendCost() / 2.0);
    }

    @Override
    protected boolean canSend(TileEntityTransporterBase linkedTransporter) {
        if(launchTicks > -20) return false;
        if(((TileEntityTransporterRocket)linkedTransporter).launchTicks < 100) return false;
        if(!hasRocket) return false;
        if(itemCount() < getThreshold()) return false;

        int fuelCost = getFuelCost();
        int oxidizerCost = getOxidizerCost();

        return tanks[8].getFill() >= fuelCost && tanks[9].getFill() >= oxidizerCost;
    }

    @Override
    protected void hasSent(TileEntityTransporterBase linkedTransporter, int quantitySent) {
        int fuelCost = getFuelCost();
        int oxidizerCost = getOxidizerCost();

        tanks[8].setFill(Math.max(0, tanks[8].getFill() - fuelCost));
        tanks[9].setFill(Math.max(0, tanks[9].getFill() - oxidizerCost));

        hasRocket = false;
        ((TileEntityTransporterRocket)linkedTransporter).hasRocket = true;
    }

    @Override
    protected void hasConnected(TileEntityTransporterBase linkedTransporter) {
        hasRocket = true;
        ((TileEntityTransporterRocket)linkedTransporter).hasRocket = false;
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerTransporterRocket(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUITransporterRocket(player.inventory, this);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeBoolean(hasRocket);
        buf.writeInt(threshold);
        super.serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        hasRocket = buf.readBoolean();
        threshold = buf.readInt();
        super.deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        hasRocket = nbt.getBoolean("rocket");
        threshold = nbt.getInteger("threshold");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("rocket", hasRocket);
        nbt.setInteger("threshold", threshold);
    }

    @Override
    protected DirPos[] getConPos() {
        ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return new DirPos[] {
            new DirPos(xCoord + dir.offsetX * 2 - rot.offsetX, yCoord, zCoord + dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite()),
            new DirPos(xCoord + dir.offsetX * 2 + rot.offsetX, yCoord, zCoord + dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
            new DirPos(xCoord - dir.offsetX * 2 - rot.offsetX, yCoord, zCoord - dir.offsetZ * 2 - rot.offsetZ, dir),
            new DirPos(xCoord - dir.offsetX * 2 + rot.offsetX, yCoord, zCoord - dir.offsetZ * 2 + rot.offsetZ, dir),
            new DirPos(xCoord + dir.offsetX - rot.offsetX, yCoord + 1, zCoord + dir.offsetZ - rot.offsetZ, ForgeDirection.UP),
            new DirPos(xCoord + dir.offsetX + rot.offsetX, yCoord + 1, zCoord + dir.offsetZ + rot.offsetZ, ForgeDirection.UP),
            new DirPos(xCoord - dir.offsetX - rot.offsetX, yCoord + 1, zCoord - dir.offsetZ - rot.offsetZ, ForgeDirection.UP),
            new DirPos(xCoord - dir.offsetX + rot.offsetX, yCoord + 1, zCoord - dir.offsetZ + rot.offsetZ, ForgeDirection.UP)
        };
    }

    @Override
    protected DirPos[] getTankPos() {
        ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        return new DirPos[] {
            new DirPos(xCoord + dir.offsetX - rot.offsetX * 3, yCoord, zCoord + dir.offsetZ - rot.offsetZ * 3, rot),
            new DirPos(xCoord - dir.offsetX - rot.offsetX * 3, yCoord, zCoord - dir.offsetZ - rot.offsetZ * 3, rot)
        };
    }

    @Override
    protected DirPos[] getInsertPos() {
        ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
        return new DirPos[] { new DirPos(xCoord - dir.offsetX * 2, yCoord, zCoord - dir.offsetZ * 2, dir) };
    }

    @Override
    protected DirPos[] getExtractPos() {
        ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
        return new DirPos[] { new DirPos(xCoord + dir.offsetX * 2, yCoord, zCoord + dir.offsetZ * 2, dir.getOpposite()) };
    }

    @Override
    public void receiveControl(NBTTagCompound nbt) {
        super.receiveControl(nbt);
        if(nbt.hasKey("threshold")) threshold = nbt.getInteger("threshold");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }
}
