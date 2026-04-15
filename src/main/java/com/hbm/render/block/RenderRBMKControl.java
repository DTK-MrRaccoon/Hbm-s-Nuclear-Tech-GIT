package com.hbm.render.block;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKPipedBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.ObjUtil;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

public class RenderRBMKControl implements ISimpleBlockRenderingHandler {

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {

		GL11.glPushMatrix();
		Tessellator tessellator = Tessellator.instance;
		RBMKBase.renderLid = RBMKBase.LID_NONE;
		IIcon iicon = block.getIcon(1, 0);
		tessellator.setColorOpaque_F(1, 1, 1);

		if(renderer.hasOverrideBlockTexture()) {
			iicon = renderer.overrideBlockTexture;
		}

		GL11.glTranslated(0, -0.75, 0);
		GL11.glScalef(0.35F, 0.35F, 0.35F);
		
		for(int i = 0; i < 4; i++) {
			tessellator.startDrawingQuads();
			tessellator.setNormal(0F, 1F, 0F);	renderer.renderFaceYPos(block, -0.5, 0, -0.5, block.getIcon(ForgeDirection.UP.ordinal(), 0));
			tessellator.setNormal(0F, -1F, 0F);	renderer.renderFaceYNeg(block, -0.5, 0, -0.5, block.getIcon(ForgeDirection.DOWN.ordinal(), 0));
			tessellator.setNormal(1F, 0F, 0F);	renderer.renderFaceXPos(block, -0.5, 0, -0.5, block.getIcon(ForgeDirection.NORTH.ordinal(), 0));
			tessellator.setNormal(-1F, 0F, 0F);	renderer.renderFaceXNeg(block, -0.5, 0, -0.5, block.getIcon(ForgeDirection.SOUTH.ordinal(), 0));
			tessellator.setNormal(0F, 0F, 1F);	renderer.renderFaceZPos(block, -0.5, 0, -0.5, block.getIcon(ForgeDirection.EAST.ordinal(), 0));
			tessellator.setNormal(0F, 0F, -1F);	renderer.renderFaceZNeg(block, -0.5, 0, -0.5, block.getIcon(ForgeDirection.WEST.ordinal(), 0));
			tessellator.draw();
			if(i < 3) GL11.glTranslated(0, 1, 0);
		}

		RBMKPipedBase.renderPipes = true;
		tessellator.startDrawingQuads();
		renderer.setRenderBounds(0.0625, 0, 0.0625, 0.4375, 0.125, 0.4375);
		tessellator.setNormal(0F, 1F, 0F);	renderer.renderFaceYPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.UP.ordinal(), 0));
		tessellator.setNormal(1F, 0F, 0F);	renderer.renderFaceXPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.NORTH.ordinal(), 0));
		tessellator.setNormal(-1F, 0F, 0F);	renderer.renderFaceXNeg(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.SOUTH.ordinal(), 0));
		tessellator.setNormal(0F, 0F, 1F);	renderer.renderFaceZPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.EAST.ordinal(), 0));
		tessellator.setNormal(0F, 0F, -1F);	renderer.renderFaceZNeg(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.WEST.ordinal(), 0));
		renderer.setRenderBounds(0.0625, 0, 0.5625, 0.4375, 0.125, 0.9375);
		tessellator.setNormal(0F, 1F, 0F);	renderer.renderFaceYPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.UP.ordinal(), 0));
		tessellator.setNormal(1F, 0F, 0F);	renderer.renderFaceXPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.NORTH.ordinal(), 0));
		tessellator.setNormal(-1F, 0F, 0F);	renderer.renderFaceXNeg(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.SOUTH.ordinal(), 0));
		tessellator.setNormal(0F, 0F, 1F);	renderer.renderFaceZPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.EAST.ordinal(), 0));
		tessellator.setNormal(0F, 0F, -1F);	renderer.renderFaceZNeg(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.WEST.ordinal(), 0));
		renderer.setRenderBounds(0.5625, 0, 0.5625, 0.9375, 0.125, 0.9375);
		tessellator.setNormal(0F, 1F, 0F);	renderer.renderFaceYPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.UP.ordinal(), 0));
		tessellator.setNormal(1F, 0F, 0F);	renderer.renderFaceXPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.NORTH.ordinal(), 0));
		tessellator.setNormal(-1F, 0F, 0F);	renderer.renderFaceXNeg(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.SOUTH.ordinal(), 0));
		tessellator.setNormal(0F, 0F, 1F);	renderer.renderFaceZPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.EAST.ordinal(), 0));
		tessellator.setNormal(0F, 0F, -1F);	renderer.renderFaceZNeg(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.WEST.ordinal(), 0));
		renderer.setRenderBounds(0.5625, 0, 0.0625, 0.9375, 0.125, 0.4375);
		tessellator.setNormal(0F, 1F, 0F);	renderer.renderFaceYPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.UP.ordinal(), 0));
		tessellator.setNormal(1F, 0F, 0F);	renderer.renderFaceXPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.NORTH.ordinal(), 0));
		tessellator.setNormal(-1F, 0F, 0F);	renderer.renderFaceXNeg(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.SOUTH.ordinal(), 0));
		tessellator.setNormal(0F, 0F, 1F);	renderer.renderFaceZPos(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.EAST.ordinal(), 0));
		tessellator.setNormal(0F, 0F, -1F);	renderer.renderFaceZNeg(block, -0.5, 1, -0.5, block.getIcon(ForgeDirection.WEST.ordinal(), 0));
		tessellator.draw();
		RBMKPipedBase.renderPipes = false;

		if(block != ModBlocks.rbmk_boiler && block != ModBlocks.rbmk_heater) {
			tessellator.startDrawingQuads();
			renderHFRModelPart(ResourceManager.rbmk_rods, "Lid", iicon, tessellator, 0, true);
			tessellator.draw();
		}

		GL11.glPopMatrix();
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {

		Tessellator tessellator = Tessellator.instance;
		int meta = world.getBlockMetadata(x, y, z);

		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1, 1, 1);
		renderer.renderStandardBlock(block, x, y, z);
		
		if(meta >= 6 && meta < 12) {
			
			boolean hasLid = false;
			if(block == ModBlocks.rbmk_boiler || block == ModBlocks.rbmk_heater) {
				
				int[] pos = ((BlockDummyable) block).findCore(world, x, y, z);
				if(pos != null) {
					int coreMeta = world.getBlockMetadata(pos[0], pos[1], pos[2]);
					int lid = RBMKBase.metaToLid(coreMeta);

					if(lid != RBMKBase.LID_NONE) {
						renderer.setRenderBounds(0, 0, 0, 1, 0.25, 1);
						RBMKBase.renderLid = lid;
						renderer.renderStandardBlock(block, x, y + 1, z);
						RBMKBase.renderLid = RBMKBase.LID_NONE;
						hasLid = true;
					}
				}
			}
			
			if(!hasLid) {
				RBMKPipedBase.renderPipes = true;
				renderer.setRenderBounds(0.0625, 0, 0.0625, 0.4375, 0.125, 0.4375);
				renderer.renderStandardBlock(block, x, y + 1, z);
				renderer.setRenderBounds(0.0625, 0, 0.5625, 0.4375, 0.125, 0.9375);
				renderer.renderStandardBlock(block, x, y + 1, z);
				renderer.setRenderBounds(0.5625, 0, 0.5625, 0.9375, 0.125, 0.9375);
				renderer.renderStandardBlock(block, x, y + 1, z);
				renderer.setRenderBounds(0.5625, 0, 0.0625, 0.9375, 0.125, 0.4375);
				renderer.renderStandardBlock(block, x, y + 1, z);
				RBMKPipedBase.renderPipes = false;
			}
		}

		return true;
	}

	@Override public boolean shouldRender3DInInventory(int modelId) { return true; }
	@Override public int getRenderId() { return RBMKBase.renderIDControl; }

	// Custom helper for HFRWavefrontObject part rendering
	private static void renderHFRModelPart(HFRWavefrontObject model, String partName, IIcon icon, Tessellator tes, float rot, boolean shadow) {
		try {
			java.lang.reflect.Field field = model.getClass().getDeclaredField("groupObjects");
			field.setAccessible(true);
			java.util.List<?> groupObjects = (java.util.List<?>) field.get(model);
			for(Object go : groupObjects) {
				java.lang.reflect.Field nameField = go.getClass().getDeclaredField("name");
				nameField.setAccessible(true);
				if(partName.equals(nameField.get(go))) {
					java.lang.reflect.Field facesField = go.getClass().getDeclaredField("faces");
					facesField.setAccessible(true);
					java.util.List<?> faces = (java.util.List<?>) facesField.get(go);
					for(Object f : faces) {
						renderFace(f, icon, tes, rot, shadow);
					}
					break;
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void renderFace(Object face, IIcon icon, Tessellator tes, float rot, boolean shadow) {
		try {
			Class<?> faceClass = face.getClass();
			java.lang.reflect.Field vertsField = faceClass.getDeclaredField("vertices");
			java.lang.reflect.Field texField = faceClass.getDeclaredField("textureCoordinates");
			java.lang.reflect.Field normalField = faceClass.getDeclaredField("faceNormal");
			vertsField.setAccessible(true);
			texField.setAccessible(true);
			normalField.setAccessible(true);
			Object[] vertices = (Object[]) vertsField.get(face);
			Object[] texCoords = (Object[]) texField.get(face);
			Object normal = normalField.get(face);

			float nx = normal.getClass().getField("x").getFloat(normal);
			float ny = normal.getClass().getField("y").getFloat(normal);
			float nz = normal.getClass().getField("z").getFloat(normal);
			tes.setNormal(nx, ny, nz);

			if(shadow) {
				float brightness = (ny + 0.7F) * 0.9F - Math.abs(nx) * 0.1F + Math.abs(nz) * 0.1F;
				if(brightness < 0.45F) brightness = 0.45F;
				tes.setColorOpaque_F(brightness, brightness, brightness);
			}

			for(int i = 0; i < vertices.length; i++) {
				Object v = vertices[i];
				Object t = texCoords[i];
				float vx = v.getClass().getField("x").getFloat(v);
				float vy = v.getClass().getField("y").getFloat(v);
				float vz = v.getClass().getField("z").getFloat(v);
				float u = t.getClass().getField("u").getFloat(t);
				float vVal = t.getClass().getField("v").getFloat(t);

				if(rot != 0) {
					double cos = Math.cos(rot);
					double sin = Math.sin(rot);
					double x1 = vx * cos - vz * sin;
					double z1 = vx * sin + vz * cos;
					vx = (float)x1;
					vz = (float)z1;
				}

				tes.addVertexWithUV(vx, vy, vz, icon.getInterpolatedU(u * 16), icon.getInterpolatedV(vVal * 16));
				if(vertices.length == 3 && i % 3 == 2) {
					tes.addVertexWithUV(vx, vy, vz, icon.getInterpolatedU(u * 16), icon.getInterpolatedV(vVal * 16));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}