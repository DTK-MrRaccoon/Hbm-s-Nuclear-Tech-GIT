package com.hbm.render.entity.elevator;

import com.hbm.tileentity.machine.elevator.EntityElevatorPlatform;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderElevatorPlatform extends Render {

	private static final ResourceLocation TEX_GRATE = new ResourceLocation("hbm", "textures/blocks/grate_top.png");
	private static final ResourceLocation TEX_PIPE = new ResourceLocation("hbm", "textures/blocks/pipe_side.png");

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
		if (!(entity instanceof EntityElevatorPlatform)) return;
		EntityElevatorPlatform platform = (EntityElevatorPlatform) entity;

		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_TEXTURE_2D);

		int size = platform.platformSize;
		float half = size / 2.0F;

		renderGrateFloor(size, half);
		renderCables(platform, size, half);

		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}

	private void renderGrateFloor(int size, float half) {
		float T = 0.1875F;
		Tessellator tess = Tessellator.instance;

		this.renderManager.renderEngine.bindTexture(TEX_GRATE);
		tess.startDrawingQuads();
		tess.setColorOpaque_F(1F, 1F, 1F);

		tess.addVertexWithUV(-half, T,  half, 0,	size);
		tess.addVertexWithUV( half, T,  half, size,  size);
		tess.addVertexWithUV( half, T, -half, size,  0);
		tess.addVertexWithUV(-half, T, -half, 0,	 0);

		tess.addVertexWithUV(-half, 0, -half, 0,	0);
		tess.addVertexWithUV( half, 0, -half, size,  0);
		tess.addVertexWithUV( half, 0,  half, size,  size);
		tess.addVertexWithUV(-half, 0,  half, 0,	 size);

		tess.addVertexWithUV(-half, 0, -half, 0,   T);
		tess.addVertexWithUV(-half, T, -half, 0,   0);
		tess.addVertexWithUV( half, T, -half, size, 0);
		tess.addVertexWithUV( half, 0, -half, size, T);

		tess.addVertexWithUV( half, 0, -half, 0,   T);
		tess.addVertexWithUV( half, T, -half, 0,   0);
		tess.addVertexWithUV( half, T,  half, size, 0);
		tess.addVertexWithUV( half, 0,  half, size, T);

		tess.addVertexWithUV( half, 0,  half, 0,   T);
		tess.addVertexWithUV( half, T,  half, 0,   0);
		tess.addVertexWithUV(-half, T,  half, size, 0);
		tess.addVertexWithUV(-half, 0,  half, size, T);

		tess.addVertexWithUV(-half, 0,  half, 0,   T);
		tess.addVertexWithUV(-half, T,  half, 0,   0);
		tess.addVertexWithUV(-half, T, -half, size, 0);
		tess.addVertexWithUV(-half, 0, -half, size, T);

		tess.draw();
	}

	private void renderCables(EntityElevatorPlatform platform, int size, float half) {
		double cableTopWorld = platform.highestFloorY + 4.0;
		double cableTopLocal = cableTopWorld - platform.posY;
		float cableBottom = 0.1875F;

		if (cableTopLocal <= cableBottom) return;

		this.renderManager.renderEngine.bindTexture(TEX_PIPE);
		Tessellator tess = Tessellator.instance;
		tess.startDrawingQuads();
		tess.setColorOpaque_F(0.8F, 0.8F, 0.8F);

		if (size == 1) {
			renderCablePipe(tess, 0f, 0f, cableBottom, (float) cableTopLocal);
		} else {
			float inset = half - 0.35f;
			renderCablePipe(tess, -inset, -inset, cableBottom, (float) cableTopLocal);
			renderCablePipe(tess,  inset, -inset, cableBottom, (float) cableTopLocal);
			renderCablePipe(tess, -inset,  inset, cableBottom, (float) cableTopLocal);
			renderCablePipe(tess,  inset,  inset, cableBottom, (float) cableTopLocal);
		}

		tess.draw();
	}

	private void renderCablePipe(Tessellator tess, float cx, float cz, float y0, float y1) {
		float w = 0.15F;
		float len = y1 - y0;
		if (len <= 0) return;

		tess.addVertexWithUV(cx - w, y0, cz - w, 0, len);
		tess.addVertexWithUV(cx - w, y1, cz - w, 0, 0);
		tess.addVertexWithUV(cx + w, y1, cz - w, 1, 0);
		tess.addVertexWithUV(cx + w, y0, cz - w, 1, len);

		tess.addVertexWithUV(cx + w, y0, cz + w, 0, len);
		tess.addVertexWithUV(cx + w, y1, cz + w, 0, 0);
		tess.addVertexWithUV(cx - w, y1, cz + w, 1, 0);
		tess.addVertexWithUV(cx - w, y0, cz + w, 1, len);

		tess.addVertexWithUV(cx - w, y0, cz + w, 0, len);
		tess.addVertexWithUV(cx - w, y1, cz + w, 0, 0);
		tess.addVertexWithUV(cx - w, y1, cz - w, 1, 0);
		tess.addVertexWithUV(cx - w, y0, cz - w, 1, len);

		tess.addVertexWithUV(cx + w, y0, cz - w, 0, len);
		tess.addVertexWithUV(cx + w, y1, cz - w, 0, 0);
		tess.addVertexWithUV(cx + w, y1, cz + w, 1, 0);
		tess.addVertexWithUV(cx + w, y0, cz + w, 1, len);

		tess.addVertexWithUV(cx - w, y1, cz - w, 0, 0);
		tess.addVertexWithUV(cx - w, y1, cz + w, 0, 1);
		tess.addVertexWithUV(cx + w, y1, cz + w, 1, 1);
		tess.addVertexWithUV(cx + w, y1, cz - w, 1, 0);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return TEX_GRATE;
	}
}
