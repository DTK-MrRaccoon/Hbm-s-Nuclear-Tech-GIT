package com.hbm.render.block;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.machine.rbmk.RBMKDebris;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.ObjUtil;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

public class RenderPribris implements ISimpleBlockRenderingHandler {

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {

		GL11.glPushMatrix();
		Tessellator tessellator = Tessellator.instance;
		IIcon iicon = block.getIcon(0, 0);
		tessellator.setColorOpaque_F(1, 1, 1);

		if(renderer.hasOverrideBlockTexture()) {
			iicon = renderer.overrideBlockTexture;
		}

		GL11.glTranslated(0, -0.5, 0);
		tessellator.startDrawingQuads();
		renderHFRModel(ResourceManager.rbmk_debris, iicon, tessellator, 0, false);
		tessellator.draw();

		GL11.glPopMatrix();
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {

		Tessellator tessellator = Tessellator.instance;
		IIcon iicon = block.getIcon(0, world.getBlockMetadata(x, y, z));

		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1, 1, 1);

		if(renderer.hasOverrideBlockTexture()) {
			iicon = renderer.overrideBlockTexture;
		}

		tessellator.addTranslation(x + 0.5F, y, z + 0.5F);
		renderHFRModel(ResourceManager.rbmk_debris, iicon, tessellator, 0, true);
		tessellator.addTranslation(-x - 0.5F, -y, -z - 0.5F);

		return true;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return true;
	}

	@Override
	public int getRenderId() {
		return RBMKDebris.renderID;
	}

	// Custom tessellation for HFRWavefrontObject
	private static void renderHFRModel(HFRWavefrontObject model, IIcon icon, Tessellator tes, float rot, boolean shadow) {
		try {
			java.lang.reflect.Field field = model.getClass().getDeclaredField("groupObjects");
			field.setAccessible(true);
			java.util.List<?> groupObjects = (java.util.List<?>) field.get(model);
			for(Object go : groupObjects) {
				java.lang.reflect.Field facesField = go.getClass().getDeclaredField("faces");
				facesField.setAccessible(true);
				java.util.List<?> faces = (java.util.List<?>) facesField.get(go);
				for(Object f : faces) {
					// Use reflection to get face data and call ObjUtil's internal logic adapted
					// Since ObjUtil is final, we inline the rendering code:
					renderFace(f, icon, tes, rot, shadow);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	// Simplified face renderer using reflection to access Vertex and TextureCoordinate
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

				// Apply rotation (simplified for Y rotation only)
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