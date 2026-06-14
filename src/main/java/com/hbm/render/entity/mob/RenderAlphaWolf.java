package com.hbm.render.entity.mob;

import org.lwjgl.opengl.GL11;

import com.hbm.entity.mob.EntityAlphaWolf;
import com.hbm.lib.RefStrings;

import net.minecraft.client.model.ModelWolf;
import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

public class RenderAlphaWolf extends RenderWolf {

	private static final ResourceLocation wolfTextures = new ResourceLocation(RefStrings.MODID, "textures/entity/wolf/alpha_wolf.png");
	private static final ResourceLocation tamedWolfTextures = new ResourceLocation(RefStrings.MODID, "textures/entity/wolf/alpha_wolf_tame.png");
	private static final ResourceLocation angryWolfTextures = new ResourceLocation(RefStrings.MODID, "textures/entity/wolf/alpha_wolf_angry.png");

	public RenderAlphaWolf() {
		super(new ModelWolf(), new ModelWolf(), 0.5F);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		EntityAlphaWolf wolf = (EntityAlphaWolf) entity;
		return wolf.isTamed() ? tamedWolfTextures : (wolf.isAngry() ? angryWolfTextures : wolfTextures);
	}

	@Override
	protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
		GL11.glScalef(1.8F, 1.8F, 1.8F);
	}
}