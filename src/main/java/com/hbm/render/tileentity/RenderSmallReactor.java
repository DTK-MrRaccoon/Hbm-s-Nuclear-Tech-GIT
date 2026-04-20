package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityMachineReactorSmall;
import com.hbm.tileentity.machine.TileEntityReactorResearch;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderSmallReactor extends TileEntitySpecialRenderer {

	@Override
		public void renderTileEntityAt(TileEntity te, double x, double y, double z, float f) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y, z + 0.5D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glRotatef(180, 0F, 1F, 0F);

		bindTexture(ResourceManager.reactor_small_base_tex);
		ResourceManager.reactor_small_base.renderAll();

		double level = 0;
		float glow = 0f;
		boolean submerged = false;
		boolean active = false;

		if(te instanceof TileEntityReactorResearch) {
			TileEntityReactorResearch r = (TileEntityReactorResearch) te;
			level = (r.lastLevel + (r.level - r.lastLevel) * f);
			glow = Math.min(1f, r.totalFlux / 100f);
			submerged = r.isSubmerged();
			active = r.level > 0;
		} else if(te instanceof TileEntityMachineReactorSmall) {
			TileEntityMachineReactorSmall r = (TileEntityMachineReactorSmall) te;
			level = r.rods / 100D;
			int totalHeat = 0;
			for(int i = 0; i < 12; i++) {
				if(r.slots[i] != null && r.slots[i].getItem() instanceof com.hbm.items.machine.ItemBreedingRod) {
					totalHeat += com.hbm.items.machine.ItemBreedingRod.getHeatPerTick(r.slots[i]);
				}
			}
			glow = Math.min(1f, totalHeat / 500f);
			submerged = r.isSubmerged();
			active = r.rods >= r.rodsMax;
		}

		GL11.glPushMatrix();
		GL11.glTranslated(0, level, 0);

		bindTexture(ResourceManager.reactor_small_rods_tex);
		ResourceManager.reactor_small_rods.renderAll();

		GL11.glPopMatrix();

		if(active && glow > 0.01f && submerged) {

			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			GL11.glDisable(GL11.GL_ALPHA_TEST);

			Tessellator tess = Tessellator.instance;

			for(double d = 0.285; d < 0.7; d += 0.025) {

				tess.startDrawingQuads();
				float alpha = 0.025f + (float)(Math.random() * 0.015f) + 0.125f * glow;
				tess.setColorRGBA_F(0.4F, 0.9F, 1.0F, alpha);

				double top = 1.375;
				double bottom = 1.375;

				tess.addVertex(d, bottom - d, -d);
				tess.addVertex(d, top + d, -d);
				tess.addVertex(d, top + d, d);
				tess.addVertex(d, bottom - d, d);

				tess.addVertex(-d, bottom - d, -d);
				tess.addVertex(-d, top + d, -d);
				tess.addVertex(-d, top + d, d);
				tess.addVertex(-d, bottom - d, d);

				tess.addVertex(-d, bottom - d, d);
				tess.addVertex(-d, top + d, d);
				tess.addVertex(d, top + d, d);
				tess.addVertex(d, bottom - d, d);

				tess.addVertex(-d, bottom - d, -d);
				tess.addVertex(-d, top + d, -d);
				tess.addVertex(d, top + d, -d);
				tess.addVertex(d, bottom - d, -d);

				tess.addVertex(-d, top + d, -d);
				tess.addVertex(-d, top + d, d);
				tess.addVertex(d, top + d, d);
				tess.addVertex(d, top + d, -d);

				tess.addVertex(-d, bottom - d, -d);
				tess.addVertex(-d, bottom - d, d);
				tess.addVertex(d, bottom - d, d);
				tess.addVertex(d, bottom - d, -d);

				tess.draw();
			}

			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
		}

		GL11.glEnable(GL11.GL_CULL_FACE);

		GL11.glPopMatrix();
	}
}
