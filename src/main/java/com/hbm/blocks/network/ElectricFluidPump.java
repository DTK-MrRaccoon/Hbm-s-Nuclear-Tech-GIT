package com.hbm.blocks.network;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.network.TileEntityElectricFluidPump;
import com.hbm.util.BobMathUtil;
import com.hbm.util.i18n.I18nUtil;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;

public class ElectricFluidPump extends BlockDummyable implements ILookOverlay {

    public ElectricFluidPump(Material mat) {
        super(mat);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntityElectricFluidPump();
        if (meta >= 6) return new TileEntityProxyCombo(false, true, true);
        return null;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {1, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack) {
        super.onBlockPlacedBy(world, x, y, z, player, itemStack);
        int i = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int meta = 2;
        if (i == 0) meta = 2;
        if (i == 1) meta = 5;
        if (i == 2) meta = 3;
        if (i == 3) meta = 4;
        world.setBlockMetadataWithNotify(x, y, z, meta + offset, 2);
        world.markBlockForUpdate(x, y, z);
        world.markBlockForUpdate(x, y + 1, z);
        Block topBlock = world.getBlock(x, y + 1, z);
        if (topBlock != null) {
            world.notifyBlocksOfNeighborChange(x, y + 1, z, topBlock);
        }
        world.markBlockForUpdate(x + 1, y + 1, z);
        world.markBlockForUpdate(x - 1, y + 1, z);
        world.markBlockForUpdate(x, y + 1, z + 1);
        world.markBlockForUpdate(x, y + 1, z - 1);
        world.markBlockForUpdate(x, y + 2, z);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        int[] corePos = findCore(world, x, y, z);
        if (corePos == null) return false;
        if (!player.isSneaking() && player.getHeldItem() != null && player.getHeldItem().getItem() instanceof IItemFluidIdentifier) {
            TileEntity te = world.getTileEntity(corePos[0], corePos[1], corePos[2]);
            if (te instanceof TileEntityElectricFluidPump) {
                if (!world.isRemote) {
                    TileEntityElectricFluidPump pump = (TileEntityElectricFluidPump) te;
                    IItemFluidIdentifier id = (IItemFluidIdentifier) player.getHeldItem().getItem();
                    pump.setFluidType(id.getType(world, x, y, z, player.getHeldItem()));
                    player.addChatComponentMessage(new ChatComponentText("Pump set to: ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.YELLOW)).appendSibling(new ChatComponentTranslation(pump.tanks[0].getTankType().getConditionalName())));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void printHook(Pre event, World world, int x, int y, int z) {
        int[] corePos = findCore(world, x, y, z);
        if (corePos == null) return;
        TileEntity tile = world.getTileEntity(corePos[0], corePos[1], corePos[2]);
        if (!(tile instanceof TileEntityElectricFluidPump)) return;
        TileEntityElectricFluidPump pump = (TileEntityElectricFluidPump) tile;
        List<String> text = new ArrayList<>();
        String blockName = I18nUtil.resolveKey(getUnlocalizedName() + ".name");
        if (y == corePos[1]) {
            ILookOverlay.printGeneric(event, blockName + " (Bottom)", 0xffff00, 0x404000, text);
        } else if (y == corePos[1] + 1) {
            ILookOverlay.printGeneric(event, blockName + " (Top)", 0xffff00, 0x404000, text);
        } else {
            int meta = world.getBlockMetadata(x, y, z);
            if (meta >= 12) {
                ILookOverlay.printGeneric(event, blockName + " (Bottom)", 0xffff00, 0x404000, text);
            } else if (meta >= 6) {
                ILookOverlay.printGeneric(event, blockName + " (Top)", 0xffff00, 0x404000, text);
            } else {
                ILookOverlay.printGeneric(event, blockName, 0xffff00, 0x404000, text);
            }
        }
        text.add(EnumChatFormatting.RED + "Power: " + BobMathUtil.getShortNumber(pump.getPower()) + " / " + BobMathUtil.getShortNumber(pump.getMaxPower()) + " HE");
        text.add(EnumChatFormatting.BLUE + "Input Tank: " + pump.tanks[0].getFill() + "/" + pump.tanks[0].getMaxFill() + " mB " + pump.tanks[0].getTankType().getLocalizedName());
        text.add(EnumChatFormatting.GREEN + "Output Tank: " + pump.tanks[1].getFill() + "/" + pump.tanks[1].getMaxFill() + " mB " + pump.tanks[1].getTankType().getLocalizedName());
        if (pump.isActive) {
            text.add(EnumChatFormatting.GREEN + "Pumping...");
        } else {
            text.add(EnumChatFormatting.RED + "Idle");
        }
        ILookOverlay.printGeneric(event, null, 0xffff00, 0x404000, text);
    }
}
