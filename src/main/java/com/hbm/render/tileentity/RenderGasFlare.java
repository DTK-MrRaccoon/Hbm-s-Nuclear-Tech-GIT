package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import com.hbm.render.item.ItemRenderBase;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

public class RenderGasFlare extends TileEntitySpecialRenderer implements IItemRendererProvider {

	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float f) {

		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y, z + 0.5D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glRotatef(180, 0F, 1F, 0F);

		TileEntityMachineGasFlare flare = (TileEntityMachineGasFlare) tileEntity;

		if(flare.tilted) {
			GL11.glTranslated(0, -0.25, 0);
			GL11.glRotated(10, 0, 0, 1);
			GL11.glRotated(5, 0, 1, 0);
		}

		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		bindTexture(ResourceManager.oilflare_tex);
		ResourceManager.oilflare.renderAll();
		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glEnable(GL11.GL_CULL_FACE);

		GL11.glPopMatrix();
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.machine_flare);
	}

	@Override
	public IItemRenderer getRenderer() {
		
		return new ItemRenderBase() {
			
			@Override
			public void renderInventory() {
				GL11.glTranslated(0, -5, 0);
				GL11.glScaled(1.35, 1.35, 1.35);
			}

			@Override
			public void renderCommonWithStack(ItemStack item) {
				GL11.glRotated(90, 0, 1, 0);
				GL11.glScaled(0.75, 0.75, 0.75);
				GL11.glShadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.oilflare_tex);
				ResourceManager.oilflare.renderAll();
				GL11.glShadeModel(GL11.GL_FLAT);
			}
		};
	}
}