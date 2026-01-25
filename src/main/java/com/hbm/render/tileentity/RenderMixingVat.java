package com.hbm.render.tileentity;

import java.awt.Color;
import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityMachineMixingVat;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

public class RenderMixingVat extends TileEntitySpecialRenderer implements IItemRendererProvider {

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float interp) {
		
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y, z + 0.5);
		
		switch(tile.getBlockMetadata() - BlockDummyable.offset) {
			case 2: GL11.glRotatef(90, 0F, 1F, 0F); break;
			case 4: GL11.glRotatef(180, 0F, 1F, 0F); break;
			case 3: GL11.glRotatef(270, 0F, 1F, 0F); break;
			case 5: GL11.glRotatef(0, 0F, 1F, 0F); break;
		}
		
		GL11.glTranslated(-0.5, 0, 0);
		
		TileEntityMachineMixingVat te = (TileEntityMachineMixingVat) tile;
		
		bindTexture(ResourceManager.mixing_vat_tex);
		ResourceManager.mixingvat.renderPart("Base");

		float rot = te.prevRot + (te.mixerRot - te.prevRot) * interp;

		GL11.glPushMatrix();
		GL11.glTranslated(0, 0, -0.5);
		GL11.glRotatef(-rot, 0, 1, 0);
		GL11.glTranslated(0, 0, 0.5);
		ResourceManager.mixingvat.renderPart("MixingBlade");
		GL11.glPopMatrix();

		ResourceManager.mixingvat.renderPart("VatGlass");
		GL11.glShadeModel(GL11.GL_SMOOTH);
		ResourceManager.mixingvat.renderPart("Vat");
		ResourceManager.mixingvat.renderPart("Tanks");
		
		float level = 0.0F; 
		int mixedColor = 0xFFFFFF;
		
		int fill0 = te.tanks[0].getFill();
		int fill1 = te.tanks[1].getFill();
		int maxFill0 = te.tanks[0].getMaxFill();
		int maxFill1 = te.tanks[1].getMaxFill();
		
		int color0 = te.tanks[0].getTankType() != Fluids.NONE ? te.tanks[0].getTankType().getColor() : 0;
		int color1 = te.tanks[1].getTankType() != Fluids.NONE ? te.tanks[1].getTankType().getColor() : 0;
		
		int totalFill = fill0 + fill1;
		int totalMax = Math.max(maxFill0, maxFill1);
		
		if(totalFill > 0) {
			level = Math.min((float)totalFill / (float)totalMax, 1.0F);
			
			if(fill0 > 0 && fill1 > 0) {
				float ratio0 = (float)fill0 / (float)totalFill;
				float ratio1 = (float)fill1 / (float)totalFill;
				
				int r0 = (color0 >> 16) & 0xFF;
				int g0 = (color0 >> 8) & 0xFF;
				int b0 = color0 & 0xFF;
				
				int r1 = (color1 >> 16) & 0xFF;
				int g1 = (color1 >> 8) & 0xFF;
				int b1 = color1 & 0xFF;
				
				int r = (int)(r0 * ratio0 + r1 * ratio1);
				int g = (int)(g0 * ratio0 + g1 * ratio1);
				int b = (int)(b0 * ratio0 + b1 * ratio1);
				
				mixedColor = (r << 16) | (g << 8) | b;
			} else if(fill0 > 0) {
				mixedColor = color0;
			} else if(fill1 > 0) {
				mixedColor = color1;
			}
			
			Color c = new Color(mixedColor);
			bindTexture(ResourceManager.mixing_vat_fluid_tex);
			
			GL11.glPushMatrix();
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_LIGHTING);
			OpenGlHelper.glBlendFunc(770, 771, 1, 0);
			
			GL11.glColor4f(c.getRed() / 255F, c.getGreen() / 255F, c.getBlue() / 255F, 0.75F);
			
			GL11.glTranslated(0, -0.65 + (0.65 * level), 0);
			
			GL11.glPushMatrix();
			GL11.glTranslated(0, 0, -0.5);
			GL11.glRotatef(-rot, 0, 1, 0);
			GL11.glTranslated(0, 0, 0.5);
			ResourceManager.mixingvat.renderPart("VatLiquid");
			GL11.glPopMatrix();
			
			if(fill0 > 0 && fill1 > 0) {
				GL11.glPushMatrix();
				GL11.glColor4f(c.getRed() / 255F * 0.8F, c.getGreen() / 255F * 0.8F, c.getBlue() / 255F * 0.8F, 0.5F);
				GL11.glTranslated(0, 0.02F, 0);
				
				GL11.glTranslated(0, 0, -0.5);
				GL11.glRotatef(-rot, 0, 1, 0);
				GL11.glTranslated(0, 0, 0.5);
				
				ResourceManager.mixingvat.renderPart("VatLiquid");
				GL11.glPopMatrix();
			}
			
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glColor4f(1, 1, 1, 1);
			GL11.glPopMatrix();
		}

		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glPopMatrix();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.machine_mixing_vat);
	}

	@Override
	public IItemRenderer getRenderer() {
		return new ItemRenderBase() {
			public void renderInventory() {
				GL11.glTranslated(0, -4, 0);
				GL11.glScaled(3.5, 3.5, 3.5);
			}
			public void renderCommon() {
				GL11.glRotatef(90, 0, 1, 0);
				GL11.glTranslated(-0.5, 0, 0);
				GL11.glShadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.mixing_vat_tex);
				ResourceManager.mixingvat.renderPart("Base");
				ResourceManager.mixingvat.renderPart("Vat");
				ResourceManager.mixingvat.renderPart("Tanks");
				ResourceManager.mixingvat.renderPart("MixingBlade");
				ResourceManager.mixingvat.renderPart("VatGlass");
				GL11.glShadeModel(GL11.GL_FLAT);
			}
		};
	}
}