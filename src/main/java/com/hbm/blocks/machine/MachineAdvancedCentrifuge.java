package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineAdvancedCentrifuge;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineAdvancedCentrifuge extends BlockDummyable implements ILookOverlay {

    public MachineAdvancedCentrifuge(Material mat) {
        super(mat);
        // optional bounding like the simpler centrifuge — uncomment if you want the same bounds
        // this.bounding.add(AxisAlignedBB.getBoundingBox(-0.5D, 0D, -0.5D, 0.5D, 1D, 0.5D));
        // this.bounding.add(AxisAlignedBB.getBoundingBox(-0.375D, 1D, -0.375D, 0.375D, 4D, 0.375D));
        // this.maxY = 0.999D;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12)
            return new TileEntityMachineAdvancedCentrifuge();
        if (meta >= 6)
            return new TileEntityProxyCombo(false, true, true);
        return null;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        } else if (!player.isSneaking()) {
            int[] pos = this.findCore(world, x, y, z);

            if (pos == null)
                return false;

            FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, pos[0], pos[1], pos[2]);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int[] getDimensions() {
        return new int[] {3, 0, 1, 0, 0, 1}; // 4 blocks tall, 2x2 base (kept from your original)
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        x = x + dir.offsetX * o;
        z = z + dir.offsetZ * o;

        ForgeDirection dr2 = dir.getRotation(ForgeDirection.UP);

        // Create the other 3 proxy blocks for the 2x2 base
        this.makeExtra(world, x, y, z - dir.offsetZ - dr2.offsetZ);
        this.makeExtra(world, x - dir.offsetX - dr2.offsetX, y, z);
        this.makeExtra(world, x - dir.offsetX - dr2.offsetX, y, z - dir.offsetZ - dr2.offsetZ);
    }

    @Override
    public void printHook(Pre event, World world, int x, int y, int z) {
        int[] pos = this.findCore(world, x, y, z);

        if (pos == null)
            return;

        TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

        if (!(te instanceof TileEntityMachineAdvancedCentrifuge))
            return;

        TileEntityMachineAdvancedCentrifuge centrifuge = (TileEntityMachineAdvancedCentrifuge) te;

        List<String> text = new ArrayList<String>();
        String powerColor = (centrifuge.power < centrifuge.getMaxPower() / 20 ? EnumChatFormatting.RED : EnumChatFormatting.GREEN).toString();
        text.add(powerColor + "Power: " + BobMathUtil.getShortNumber(centrifuge.power) + " / " + BobMathUtil.getShortNumber(centrifuge.getMaxPower()) + "HE");

        try {
            for (int i = 0; i < 4; i++) {
                ItemStack s = centrifuge.slots[i];
                if (s == null) {
                    text.add(EnumChatFormatting.GRAY + "Slot " + (i + 1) + ": " + EnumChatFormatting.RESET + "empty");
                } else {
                    text.add(EnumChatFormatting.YELLOW + "Slot " + (i + 1) + ": " + EnumChatFormatting.RESET + s.stackSize + " x " + s.getDisplayName());
                }
            }
        } catch (Exception e) {
            text.add(EnumChatFormatting.RED + "Slot info unavailable");
        }
        int percent = 0;
        if (centrifuge.maxProgress > 0) {
            percent = (int) (centrifuge.progress * 100L / (long) centrifuge.maxProgress);
            if (percent < 0) percent = 0;
            if (percent > 100) percent = 100;
        }
        text.add(EnumChatFormatting.AQUA + "Progress: " + EnumChatFormatting.RESET + percent + "% of current ore");

        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
    }
}
