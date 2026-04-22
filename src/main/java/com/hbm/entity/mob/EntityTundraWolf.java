package com.hbm.entity.mob;

import com.hbm.entity.mob.ai.EntityAIHowlAtNight;
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

public class EntityTundraWolf extends EntityWolf {

	public EntityTundraWolf(World world) {
		super(world);
		this.setSize(0.9F, 1.3F);
		this.tasks.addTask(3, new EntityAIHowlAtNight(this));
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.35D);
	}

	@Override
	public boolean attackEntityAsMob(Entity target) {
		return target.attackEntityFrom(DamageSource.causeMobDamage(this), 8.0F);
	}

	@Override
	public EntityWolf createChild(EntityAgeable ageable) {
		EntityTundraWolf child = new EntityTundraWolf(this.worldObj);
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