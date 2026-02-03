package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityRadSensor;
import api.hbm.block.IToolable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class RadSensor extends BlockContainer implements IToolable {

    public RadSensor(Material mat) {
        super(mat);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityRadSensor();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntityRadSensor entity = (TileEntityRadSensor) world.getTileEntity(x, y, z);
        if (entity != null) {
            player.addChatMessage(new ChatComponentText("§6===== ☢ Radiation Sensor ☢ =====§r"));
            player.addChatMessage(new ChatComponentText("§eCurrent chunk radiation: §a" + entity.chunkRads + " RAD/s§r"));
            player.addChatMessage(new ChatComponentText("§eRedstone signal output: §c" + entity.redstoneOutput + "§r"));
            player.addChatMessage(new ChatComponentText("§eReceived radiation dose: §a" + entity.recievedDose + " RAD§r"));
            player.addChatMessage(new ChatComponentText("§eComparator signal output: §c" + entity.comparatorOutput + "§r"));
        }
        return true;
    }

    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        TileEntityRadSensor entity = (TileEntityRadSensor) world.getTileEntity(x, y, z);
        return entity != null ? entity.redstoneOutput : 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
        return isProvidingWeakPower(world, x, y, z, side);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntityRadSensor entity = (TileEntityRadSensor) world.getTileEntity(x, y, z);
        return entity != null ? entity.comparatorOutput : 0;
    }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, int side, float fX, float fY, float fZ, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (world.isRemote) return true;
        TileEntityRadSensor te = (TileEntityRadSensor) world.getTileEntity(x, y, z);
        if (te != null) {
            te.recievedDose = 0F;
            te.markDirty();
            world.markBlockForUpdate(x, y, z);
            player.addChatMessage(new ChatComponentText("§aRadiation dose reset.§r"));
        }
        return true;
    }
}
