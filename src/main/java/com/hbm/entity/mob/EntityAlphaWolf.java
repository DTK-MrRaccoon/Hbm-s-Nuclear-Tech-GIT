package com.hbm.entity.mob;

import com.hbm.entity.mob.ai.EntityAIAlphaHowl;
import api.hbm.entity.IRadiationImmune;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityAlphaWolf extends EntityWolf implements IRadiationImmune {

	private EntityAIAlphaHowl howlAI;

	public EntityAlphaWolf(World world) {
		super(world);
		this.setSize(1.1F, 1.5F);
		this.howlAI = new EntityAIAlphaHowl(this);
		this.tasks.addTask(3, this.howlAI);
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.4D);
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		boolean damaged = super.attackEntityFrom(source, amount);
		if (damaged && !worldObj.isRemote && source.getEntity() != null && this.howlAI != null) {
			this.howlAI.forcePackCall();
		}
		return damaged;
	}

	@Override
	public boolean attackEntityAsMob(Entity target) {
		return target.attackEntityFrom(DamageSource.causeMobDamage(this), 12.0F);
	}

	@Override
	public EntityWolf createChild(EntityAgeable ageable) {
		EntityAlphaWolf child = new EntityAlphaWolf(this.worldObj);
		String owner = this.func_152113_b();
		if (owner != null && owner.trim().length() > 0) {
			child.func_152115_b(owner);
			child.setTamed(true);
		}
		return child;
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack != null && stack.getItem() instanceof ItemFood && ((ItemFood)stack.getItem()).isWolfsFavoriteMeat();
	}

	@Override
	protected Item getDropItem() {
		return Items.bone;
	}
}