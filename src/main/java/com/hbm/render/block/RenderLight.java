package com.hbm.render.block;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.machine.SpotlightBase;
import com.hbm.blocks.machine.SpotlightModular;
import com.hbm.blocks.machine.SpotlightModularPowered;
import com.hbm.render.util.ObjUtil;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

public class RenderLight implements ISimpleBlockRenderingHandler {

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
		if(!(block instanceof SpotlightBase)) return;
		SpotlightBase spot = (SpotlightBase) block;

		GL11.glPushMatrix();
		Tessellator tessellator = Tessellator.instance;
		IIcon iicon = block.getIcon(0, 0);
		tessellator.setColorOpaque_F(1, 1, 1);

		if(renderer.hasOverrideBlockTexture()) {
			iicon = renderer.overrideBlockTexture;
		}
		
		GL11.glRotated(-90, 0, 1, 0);
		GL11.glScaled(1.5D, 1.5D, 1.5D);
		tessellator.startDrawingQuads();
		ObjUtil.renderPartWithIcon(spot.getModel(), spot.getPartName(0), iicon, tessellator, 0, false);
		tessellator.draw();

		GL11.glPopMatrix();
    }

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		if(!(block instanceof SpotlightBase)) return true;
		SpotlightBase spot = (SpotlightBase) block;

		Tessellator tessellator = Tessellator.instance;
		ForgeDirection dir = spot.getDirection(world, x, y, z);

		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		tessellator.setColorOpaque_F(1, 1, 1);

		float ox = 0.5F - dir.offsetX * 0.5F, oy = 0.5F - dir.offsetY * 0.5F, oz = 0.5F - dir.offsetZ * 0.5F;
		float rot = ObjUtil.getYaw(dir);
		float pitch = ObjUtil.getPitch(dir);
		float roll = 0;

		int connectionCount = 0;
		// Check modular variants (both powered and non-powered)
		if(spot instanceof SpotlightModular) {
			SpotlightModular modular = (SpotlightModular) spot;
			ForgeDirection connectionDirection = null;

			// Searching through only adjacent blocks is simple, iterate through all directions ignoring the root and its opposite
			// Once we have found any valid connection, we'll only connect in that one axis
			for (ForgeDirection availableDir : ForgeDirection.VALID_DIRECTIONS) {
				if(availableDir == dir || availableDir == dir.getOpposite()) continue;
				if(modular.canConnectTo(world, x + availableDir.offsetX, y + availableDir.offsetY, z + availableDir.offsetZ)) {
					connectionCount++;
					connectionDirection = availableDir;
					break;
				}
			}
			if(connectionDirection != null) {
				// Check if we're sandwiched between two lights
				if(modular.canConnectTo(world, x - connectionDirection.offsetX, y - connectionDirection.offsetY, z - connectionDirection.offsetZ)) {
					connectionCount++;
				}
				roll = getRotation(connectionDirection, dir);
			}
		} else if(spot instanceof SpotlightModularPowered) {
			SpotlightModularPowered modular = (SpotlightModularPowered) spot;
			ForgeDirection connectionDirection = null;

			for (ForgeDirection availableDir : ForgeDirection.VALID_DIRECTIONS) {
				if(availableDir == dir || availableDir == dir.getOpposite()) continue;
				if(modular.canConnectTo(world, x + availableDir.offsetX, y + availableDir.offsetY, z + availableDir.offsetZ, x, y, z)) {
					connectionCount++;
					connectionDirection = availableDir;
					break;
				}
			}
			if(connectionDirection != null) {
				if(modular.canConnectTo(world, x - connectionDirection.offsetX, y - connectionDirection.offsetY, z - connectionDirection.offsetZ, x, y, z)) {
					connectionCount++;
				}
				roll = getRotation(connectionDirection, dir);
			}
		}

		tessellator.addTranslation(x + ox, y + oy, z + oz);
		ObjUtil.renderPartWithIcon(spot.getModel(), spot.getPartName(connectionCount), block.getIcon(0, 0), tessellator, rot, pitch, roll, false);
		tessellator.addTranslation(-x - ox, -y - oy, -z - oz);

		return true;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return true;
	}

	@Override
	public int getRenderId() {
		return SpotlightBase.renderID;
	}

	// This is very ad-hoc, which isn't ideal
	private float getRotation(ForgeDirection dir, ForgeDirection axis) {
		float flipX = axis == ForgeDirection.DOWN || axis == ForgeDirection.NORTH || axis == ForgeDirection.WEST ? -0.5F : 0.5F;
		float addX = axis == ForgeDirection.NORTH || axis == ForgeDirection.SOUTH ? -0.5F : 0;
		boolean flipNS = axis == ForgeDirection.WEST;
		switch (dir) {
			case NORTH: return flipNS ? (float)Math.PI : 0;
			case SOUTH: return !flipNS ? (float)Math.PI : 0;
			case EAST: return (float)Math.PI * (flipX + addX);
			case WEST: return (float)Math.PI * (-flipX + addX);
			case UP: return (float)Math.PI * -0.5F;
			case DOWN: return (float)Math.PI * 0.5F;
			default: return 0;
		}
	}
}
