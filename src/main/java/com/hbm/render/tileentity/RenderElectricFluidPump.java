package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.network.TileEntityElectricFluidPump;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

public class RenderElectricFluidPump extends TileEntitySpecialRenderer implements IItemRendererProvider {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float interp) {
        if(!(te instanceof TileEntityElectricFluidPump)) return;
        TileEntityElectricFluidPump elepump = (TileEntityElectricFluidPump) te;

        GL11.glPushMatrix();
        {
            GL11.glTranslated(x + 0.5D, y, z + 0.5D);
            GL11.glEnable(GL11.GL_LIGHTING);

            // Initial rotation for the model standard
            GL11.glRotatef(180, 0F, 1F, 0F);

            // Metadata based rotation (NSEW)
            // Subtract offset because it's a Dummyable block
            int meta = te.getBlockMetadata() - BlockDummyable.offset;
            
            switch(meta) {
            case 2: GL11.glRotatef(0, 0F, 1F, 0F); break;
            case 4: GL11.glRotatef(90, 0F, 1F, 0F); break;
            case 3: GL11.glRotatef(180, 0F, 1F, 0F); break;
            case 5: GL11.glRotatef(270, 0F, 1F, 0F); break;
            }

            bindTexture(ResourceManager.electric_fluid_pump_tex);

            GL11.glShadeModel(GL11.GL_SMOOTH);
            
            // Render the static base
            ResourceManager.electric_fluid_pump.renderPart("Base");

            // Calculate smooth rotation for the fan
            float rotation = elepump.prevRot + (elepump.rot - elepump.prevRot) * interp;

            // Render the fan with rotation
            GL11.glPushMatrix();
            GL11.glRotatef(rotation, 0, 1, 0);
            ResourceManager.electric_fluid_pump.renderPart("Fan");
            GL11.glPopMatrix();

            GL11.glShadeModel(GL11.GL_FLAT);

        }
        GL11.glPopMatrix();
    }

    @Override
    public IItemRenderer getRenderer() {
        return new ItemRenderBase() {
            public void renderInventory() {
                GL11.glTranslated(0, -2, 0); // Adjust Y to center the 2-block tall model in inventory
                GL11.glScaled(4, 4, 4); // Adjust scale
            }
            public void renderCommon() {
                GL11.glScaled(1.0, 1.0, 1.0); // Normal scale
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glShadeModel(GL11.GL_SMOOTH);
                bindTexture(ResourceManager.electric_fluid_pump_tex);
                ResourceManager.electric_fluid_pump.renderAll();
                GL11.glShadeModel(GL11.GL_FLAT);
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
        };
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.electric_fluid_pump);
    }
}