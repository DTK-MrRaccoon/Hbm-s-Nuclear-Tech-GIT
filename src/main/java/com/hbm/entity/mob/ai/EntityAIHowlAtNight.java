package com.hbm.entity.mob.ai;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityWolf;

public class EntityAIHowlAtNight extends EntityAIBase {

	private EntityWolf wolf;
	private int howlTimer;

	public EntityAIHowlAtNight(EntityWolf wolf) {
		this.wolf = wolf;
		this.setMutexBits(3);
	}

	@Override
	public boolean shouldExecute() {
		long time = wolf.worldObj.getWorldTime() % 24000;
		boolean isNight = time > 13000 && time < 23000;
		return isNight && wolf.getRNG().nextInt(200) == 0;
	}

	@Override
	public void startExecuting() {
		this.howlTimer = 20;
		wolf.worldObj.playSoundAtEntity(wolf, "mob.wolf.howl", 2.0F, 1.0F);
	}

	@Override
	public boolean continueExecuting() {
		return howlTimer-- > 0;
	}
}