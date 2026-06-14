package com.hbm.render.block;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.network.HeatPipe;
import com.hbm.lib.Library;
import com.hbm.render.util.RenderBlocksNT;
import com.hbm.tileentity.network.TileEntityHeatPipe;
import com.hbm.util.BobMathUtil;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

public class RenderHeatPipe implements ISimpleBlockRenderingHandler {

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
		Tessellator tessellator = Tessellator.instance;
		HeatPipe pipe = (HeatPipe) block;
		float lower = 0.125F;
		float upper = 0.875F;

		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
		renderer.setRenderBounds(lower, lower, 0.0F, upper, upper, 1.0F);
		renderer.uvRotateNorth = 1;
		renderer.uvRotateSouth = 2;

		tessellator.startDrawingQuads();
		tessellator.setNormal(0F, 1F, 0F);
		renderer.renderFaceYPos(block, 0, 0, 0, pipe.iconStraight);
		tessellator.setNormal(0F, -1F, 0F);
		renderer.renderFaceYNeg(block, 0, 0, 0, pipe.iconStraight);
		tessellator.setNormal(1F, 0F, 0F);
		renderer.renderFaceXPos(block, 0, 0, 0, pipe.iconStraight);
		tessellator.setNormal(-1F, 0F, 0F);
		renderer.renderFaceXNeg(block, 0, 0, 0, pipe.iconStraight);
		tessellator.setNormal(0F, 0F, 1F);
		renderer.renderFaceZPos(block, 0, 0, 0, pipe.iconEnd);
		tessellator.setNormal(0F, 0F, -1F);
		renderer.renderFaceZNeg(block, 0, 0, 0, pipe.iconEnd);
		tessellator.draw();

		renderer.uvRotateNorth = 0;
		renderer.uvRotateSouth = 0;
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		renderer = RenderBlocksNT.INSTANCE.setWorld(world);

		TileEntity te = world.getTileEntity(x, y, z);
		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));

		if (!(te instanceof TileEntityHeatPipe)) return true;

		TileEntityHeatPipe pipe = (TileEntityHeatPipe) te;

		boolean pX = pipe.connections[Library.POS_X.ordinal()];
		boolean nX = pipe.connections[Library.NEG_X.ordinal()];
		boolean pY = pipe.connections[Library.POS_Y.ordinal()];
		boolean nY = pipe.connections[Library.NEG_Y.ordinal()];
		boolean pZ = pipe.connections[Library.POS_Z.ordinal()];
		boolean nZ = pipe.connections[Library.NEG_Z.ordinal()];

		int mask = (pX ? 32 : 0) | (nX ? 16 : 0) | (pY ? 8 : 0) | (nY ? 4 : 0) | (pZ ? 2 : 0) | (nZ ? 1 : 0);
		int count = (pX ? 1 : 0) + (nX ? 1 : 0) + (pY ? 1 : 0) + (nY ? 1 : 0) + (pZ ? 1 : 0) + (nZ ? 1 : 0);

		double lower = 0.125D;
		double upper = 0.875D;
		double jLower = 0.0625D;
		double jUpper = 0.9375D;

		if ((mask & 0b001111) == 0 && mask > 0) {
			renderer.uvRotateTop = 1;
			renderer.uvRotateBottom = 1;
			renderer.uvRotateEast = 2;
			renderer.uvRotateWest = 1;
			renderer.setRenderBounds(0.0D, lower, lower, 1.0D, upper, upper);
			renderer.renderStandardBlock(block, x, y, z);

		} else if ((mask & 0b111100) == 0 && mask > 0) {
			renderer.uvRotateNorth = 1;
			renderer.uvRotateSouth = 2;
			renderer.setRenderBounds(lower, lower, 0.0D, upper, upper, 1.0D);
			renderer.renderStandardBlock(block, x, y, z);

		} else if ((mask & 0b110011) == 0 && mask > 0) {
			renderer.setRenderBounds(lower, 0.0D, lower, upper, 1.0D, upper);
			renderer.renderStandardBlock(block, x, y, z);

		} else if (count == 2) {
			if (nY && (pX || nX)) {
				renderer.uvRotateTop = 1;
				renderer.uvRotateBottom = 1;
			}
			if (pY && (pX || nX)) {
				renderer.uvRotateTop = 1;
				renderer.uvRotateBottom = 1;
			}
			if (!nY && !pY) {
				renderer.uvRotateNorth = 1;
				renderer.uvRotateSouth = 2;
				renderer.uvRotateEast = 2;
				renderer.uvRotateWest = 1;
			}
			renderer.setRenderBounds(lower, lower, lower, upper, upper, upper);
			renderer.renderStandardBlock(block, x, y, z);

			if (nY) {
				renderer.setRenderBounds(lower, 0.0D, lower, upper, lower, upper);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (pY) {
				renderer.setRenderBounds(lower, upper, lower, upper, 1.0D, upper);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (nX) {
				renderer.setRenderBounds(0.0D, lower, lower, lower, upper, upper);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (pX) {
				renderer.setRenderBounds(upper, lower, lower, 1.0D, upper, upper);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (nZ) {
				renderer.setRenderBounds(lower, lower, 0.0D, upper, upper, lower);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (pZ) {
				renderer.setRenderBounds(lower, lower, upper, upper, upper, 1.0D);
				renderer.renderStandardBlock(block, x, y, z);
			}

		} else {
			renderer.setRenderBounds(jLower, jLower, jLower, jUpper, jUpper, jUpper);
			renderer.renderStandardBlock(block, x, y, z);

			if (nY) {
				renderer.setRenderBounds(lower, 0.0D, lower, upper, jLower, upper);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (pY) {
				renderer.setRenderBounds(lower, jUpper, lower, upper, 1.0D, upper);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (nX) {
				renderer.setRenderBounds(0.0D, lower, lower, jLower, upper, upper);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (pX) {
				renderer.setRenderBounds(jUpper, lower, lower, 1.0D, upper, upper);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (nZ) {
				renderer.setRenderBounds(lower, lower, 0.0D, upper, upper, jLower);
				renderer.renderStandardBlock(block, x, y, z);
			}
			if (pZ) {
				renderer.setRenderBounds(lower, lower, jUpper, upper, upper, 1.0D);
				renderer.renderStandardBlock(block, x, y, z);
			}
		}

		renderer.uvRotateTop = 0;
		renderer.uvRotateBottom = 0;
		renderer.uvRotateNorth = 0;
		renderer.uvRotateSouth = 0;
		renderer.uvRotateEast = 0;
		renderer.uvRotateWest = 0;

		return true;
	}

	public static void renderHeatLabel(TileEntityHeatPipe pipe, double x, double y, double z) {
		if (pipe == null) return;
		if (pipe.heat <= 0) return;

		Minecraft mc = Minecraft.getMinecraft();
		if (mc.objectMouseOver == null) return;
		if (mc.objectMouseOver.blockX != pipe.xCoord) return;
		if (mc.objectMouseOver.blockY != pipe.yCoord) return;
		if (mc.objectMouseOver.blockZ != pipe.zCoord) return;

		FontRenderer font = mc.fontRenderer;
		String text = BobMathUtil.getShortNumber(pipe.heat) + " TU";
		int width = font.getStringWidth(text);

		double distance = mc.renderViewEntity.getDistance(x + 0.5D, y + 0.5D, z + 0.5D);
		if (distance > 8.0D) return;

		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GL11.glTranslated(x + 0.5, y + 1.2, z + 0.5);
		GL11.glNormal3f(0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
		GL11.glScalef(0.025F, 0.025F, 0.025F);

		int bgWidth = width + 4;
		int bgHeight = 10;

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.75F);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2d(-bgWidth / 2, 0);
		GL11.glVertex2d(bgWidth / 2, 0);
		GL11.glVertex2d(bgWidth / 2, bgHeight);
		GL11.glVertex2d(-bgWidth / 2, bgHeight);
		GL11.glEnd();

		GL11.glColor4f(1.0F, 0.3F, 0.1F, 1.0F);
		font.drawString(text, -width / 2, 2, 0xFFFFFF);

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_BLEND);

		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return true;
	}

	@Override
	public int getRenderId() {
		return HeatPipe.renderID;
	}
}
