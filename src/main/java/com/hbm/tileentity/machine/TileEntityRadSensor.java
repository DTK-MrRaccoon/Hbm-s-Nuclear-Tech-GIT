package com.hbm.tileentity.machine;

import com.hbm.handler.CompatHandler;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.util.CompatEnergyControl;
import api.hbm.tile.IInfoProviderEC;
import cpw.mods.fml.common.Optional;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

@Optional.InterfaceList({@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers")})
public class TileEntityRadSensor extends TileEntity implements SimpleComponent, IInfoProviderEC, CompatHandler.OCComponent {

    public float chunkRads = 0F;
    public float recievedDose = 0F;
    public float lastChunkRads = 0F;

    public int redstoneOutput = 0;
    public int comparatorOutput = 0;

    public int lastRedstoneOutput = 0;
    public int lastComparatorOutput = 0;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        chunkRads = nbt.getFloat("chunkRads");
        recievedDose = nbt.getFloat("recievedDose");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setFloat("chunkRads", chunkRads);
        nbt.setFloat("recievedDose", recievedDose);
    }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote) {
            chunkRads = (ChunkRadiationManager.proxy.getRadiation(worldObj, xCoord, yCoord, zCoord) + lastChunkRads) / 2F;
            recievedDose += chunkRads / 20F;
            redstoneOutput = getRestoneOutput();
            comparatorOutput = getComparatorOutput();
            if (redstoneOutput != lastRedstoneOutput || comparatorOutput != lastComparatorOutput) {
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, this.getBlockType());
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            lastChunkRads = chunkRads;
            lastRedstoneOutput = redstoneOutput;
            lastComparatorOutput = comparatorOutput;
        }
    }

    private int getRestoneOutput() {
        if (chunkRads < 0.01F) return 0;
        if (chunkRads < 0.5F) return 1;
        if (chunkRads < 1F) return 2;
        if (chunkRads < 2F) return 3;
        if (chunkRads < 4F) return 4;
        if (chunkRads < 8F) return 5;
        if (chunkRads < 16F) return 6;
        if (chunkRads < 32F) return 7;
        if (chunkRads < 64F) return 8;
        if (chunkRads < 128F) return 9;
        if (chunkRads < 256F) return 10;
        if (chunkRads < 512F) return 11;
        if (chunkRads < 1024F) return 12;
        if (chunkRads < 2048F) return 13;
        if (chunkRads < 4096F) return 14;
        return 15;
    }

    private int getComparatorOutput() {
        if (recievedDose < 0.01F) return 0;
        if (recievedDose < 8F) return 1;
        if (recievedDose < 16F) return 2;
        if (recievedDose < 32F) return 3;
        if (recievedDose < 64F) return 4;
        if (recievedDose < 128F) return 5;
        if (recievedDose < 256F) return 6;
        if (recievedDose < 512F) return 7;
        if (recievedDose < 1024F) return 8;
        if (recievedDose < 2048F) return 9;
        if (recievedDose < 4096F) return 10;
        if (recievedDose < 8192F) return 11;
        if (recievedDose < 16384F) return 12;
        if (recievedDose < 32768F) return 13;
        if (recievedDose < 65536F) return 14;
        return 15;
    }

    @Override
    @Optional.Method(modid = "OpenComputers")
    public String getComponentName() {
        return "radsensor";
    }

    @Callback(direct = true)
    @Optional.Method(modid = "OpenComputers")
    public Object[] getRads(Context context, Arguments args) {
        return new Object[] { chunkRads };
    }

    @Callback(direct = true)
    @Optional.Method(modid = "OpenComputers")
    public Object[] getDose(Context context, Arguments args) {
        return new Object[] { recievedDose };
    }

    @Callback(direct = true)
    @Optional.Method(modid = "OpenComputers")
    public Object[] resetDose(Context context, Arguments args) {
        recievedDose = 0F;
        return new Object[] { true };
    }

    @Override
    public void provideExtraInfo(NBTTagCompound data) {
        data.setString(CompatEnergyControl.S_CHUNKRAD, (int)chunkRads + " RAD/s");
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }
}
