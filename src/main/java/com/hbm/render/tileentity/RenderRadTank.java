package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;
import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.storage.TileEntityMachineRadTank;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderRadTank extends TileEntitySpecialRenderer {

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float interp) {
		
		TileEntityMachineRadTank tank = (TileEntityMachineRadTank) te;
		
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y, z + 0.5);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_CULL_FACE);
		
		// Rotation based on metadata
		switch(te.getBlockMetadata() - BlockDummyable.offset) {
			case 2: GL11.glRotatef(180, 0F, 1F, 0F); break;
			case 4: GL11.glRotatef(270, 0F, 1F, 0F); break;
			case 3: GL11.glRotatef(0, 0F, 1F, 0F); break;
			case 5: GL11.glRotatef(90, 0F, 1F, 0F); break;
		}
		
		bindTexture(ResourceManager.radtank_tex);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		
		ResourceManager.radtank.renderPart("Body");
		
		// Frame render logic
		if(tank.frame) {
			ResourceManager.radtank.renderPart("Frame");
		}
		
		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glPopMatrix();
	}
}