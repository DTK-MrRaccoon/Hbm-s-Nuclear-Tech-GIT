package com.hbm.render.block;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKRod;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

public class RenderRBMKRod implements ISimpleBlockRenderingHandler {

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {

		GL11.glPushMatrix();
		RBMKRod rod = (RBMKRod) block;
		Tessellator tessellator = Tessellator.instance;
		RBMKBase.renderLid = RBMKBase.LID_NONE;
		IIcon iicon = block.getIcon(0, 0);
		IIcon sideIcon = block.getIcon(2, 0);
		tessellator.setColorOpaque_F(1, 1, 1);

		if(renderer.hasOverrideBlockTexture()) {
			iicon = renderer.overrideBlockTexture;
		}

		GL11.glTranslated(0, -0.675, 0);
		GL11.glScalef(0.35F, 0.35F, 0.35F);
		
		for(int i = 0; i < 4; i++) {
			tessellator.startDrawingQuads();
			renderHFRModelPart(ResourceManager.rbmk_element, "Inner", rod.inner, tessellator, 0, false);
			renderHFRModelPart(ResourceManager.rbmk_element, "Cap", iicon, tessellator, 0, false);
			tessellator.setNormal(-1F, 0F, 0F);	renderer.renderFaceXNeg(block, -0.5, 0, -0.5, sideIcon);
			tessellator.setNormal(1F, 0F, 0F);	renderer.renderFaceXPos(block, -0.5, 0, -0.5, sideIcon);
			tessellator.setNormal(0F, 0F, -1F);	renderer.renderFaceZNeg(block, -0.5, 0, -0.5, sideIcon);
			tessellator.setNormal(0F, 0F, 1F);	renderer.renderFaceZPos(block, -0.5, 0, -0.5, sideIcon);
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setColorOpaque_I(0x304825);
			renderHFRModelPart(ResourceManager.rbmk_element_rods, "Rods", rod.fuel, tessellator, 0, false);
			tessellator.draw();
			GL11.glTranslated(0, 1, 0);
		}

		GL11.glPopMatrix();
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {

		RBMKRod rod = (RBMKRod) block;
		Tessellator tessellator = Tessellator.instance;
		int meta = world.getBlockMetadata(x, y, z);
		IIcon iicon = block.getIcon(0, meta);

		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1, 1, 1);

		if(renderer.hasOverrideBlockTexture()) {
			iicon = renderer.overrideBlockTexture;
		}
		
		renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
		rod.overrideOnlyRenderSides = true;
		renderer.renderStandardBlock(block, x, y, z);
		rod.overrideOnlyRenderSides = false;

		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.addTranslation(x + 0.5F, y, z + 0.5F);
		renderHFRModelPart(ResourceManager.rbmk_element, "Cap", iicon, tessellator, 0, true);
		renderHFRModelPart(ResourceManager.rbmk_element, "Inner", rod.inner, tessellator, 0, true);
		tessellator.addTranslation(-x - 0.5F, -y, -z - 0.5F);
		
		if(meta >= 6 && meta < 12) {
			int[] pos = ((BlockDummyable) block).findCore(world, x, y, z);
			if(pos != null) {
				int coreMeta = world.getBlockMetadata(pos[0], pos[1], pos[2]);
				int lid = RBMKBase.metaToLid(coreMeta);

				if(lid != RBMKBase.LID_NONE) {
					renderer.setRenderBounds(0, 0, 0, 1, 0.25, 1);
					RBMKBase.renderLid = lid;
					renderer.renderStandardBlock(block, x, y + 1, z);
					RBMKBase.renderLid = RBMKBase.LID_NONE;
				}
			}
		}

		return true;
	}

	@Override public boolean shouldRender3DInInventory(int modelId) { return true; }
	@Override public int getRenderId() { return RBMKBase.renderIDRods; }

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