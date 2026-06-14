package com.hbm.entity.mob;

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

public class EntityGoldenWolf extends EntityWolf {

	public EntityGoldenWolf(World world) {
		super(world);
		this.setSize(0.5F, 0.7F);
		this.isImmuneToFire = true;
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.4D);
	}

	@Override
	public boolean attackEntityAsMob(Entity target) {
		boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), 5.0F);
		if (hit) target.setFire(5);
		return hit;
	}

	@Override
	public EntityWolf createChild(EntityAgeable ageable) {
		EntityGoldenWolf child = new EntityGoldenWolf(this.worldObj);
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