package com.hbm.render.entity.mob;

import org.lwjgl.opengl.GL11;

import com.hbm.entity.mob.EntityTundraWolf;
import com.hbm.lib.RefStrings;

import net.minecraft.client.model.ModelWolf;
import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

public class RenderTundraWolf extends RenderWolf {

	private static final ResourceLocation wolfTextures = new ResourceLocation(RefStrings.MODID, "textures/entity/wolf/tundra_wolf.png");
	private static final ResourceLocation tamedWolfTextures = new ResourceLocation(RefStrings.MODID, "textures/entity/wolf/tundra_wolf_tame.png");
	private static final ResourceLocation angryWolfTextures = new ResourceLocation(RefStrings.MODID, "textures/entity/wolf/tundra_wolf_angry.png");

	public RenderTundraWolf() {
		super(new ModelWolf(), new ModelWolf(), 0.5F);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		EntityTundraWolf wolf = (EntityTundraWolf) entity;
		return wolf.isTamed() ? tamedWolfTextures : (wolf.isAngry() ? angryWolfTextures : wolfTextures);
	}

	@Override
	protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
		GL11.glScalef(1.5F, 1.5F, 1.5F); // Scales the wolf to be larger
	}
}