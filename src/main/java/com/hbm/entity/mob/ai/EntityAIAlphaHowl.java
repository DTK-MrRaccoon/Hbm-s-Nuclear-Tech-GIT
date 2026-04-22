package com.hbm.entity.mob.ai;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityWolf;

public class EntityAIAlphaHowl extends EntityAIBase {

	private EntityWolf alpha;
	private int howlTimer;
	private boolean forcePackCall;

	public EntityAIAlphaHowl(EntityWolf alpha) {
		this.alpha = alpha;
		this.setMutexBits(3);
	}

	public void forcePackCall() {
		this.forcePackCall = true;
	}

	@Override
	public boolean shouldExecute() {
		if (alpha.isTamed()) return false;
		
		long time = alpha.worldObj.getWorldTime() % 24000;
		boolean isNight = time > 13000 && time < 23000;
		
		if (isNight && alpha.getRNG().nextInt(150) == 0) return true;
		
		EntityLivingBase target = alpha.getAttackTarget();
		if (target != null && (target instanceof IMob || isHostileMob(target))) {
			if (alpha.getRNG().nextInt(80) == 0) return true;
		}
		
		return false;
	}

	private boolean isHostileMob(EntityLivingBase entity) {
		String name = entity.getClass().getSimpleName().toLowerCase();
		return name.contains("creeper") || name.contains("zombie") || name.contains("skeleton") || 
		       name.contains("spider") || name.contains("cave") || name.contains("enderman");
	}

	@Override
	public void startExecuting() {
		this.howlTimer = 25;
		alpha.worldObj.playSoundAtEntity(alpha, "mob.wolf.howl", 2.0F, 1.0F);
		
		boolean callPack = forcePackCall || alpha.getRNG().nextInt(4) == 0;
		forcePackCall = false;
		
		if (callPack) {
			double range = 48.0D;
			List<EntityWolf> pack = alpha.worldObj.getEntitiesWithinAABB(EntityWolf.class, 
					alpha.boundingBox.expand(range, range, range));
			
			for (EntityWolf wolf : pack) {
				if (!wolf.isTamed() && wolf != alpha) {
					EntityLivingBase target = alpha.getAttackTarget();
					if (target != null) {
						wolf.setAttackTarget(target);
					}
					if (wolf.getRNG().nextInt(2) == 0) {
						wolf.worldObj.playSoundAtEntity(wolf, "mob.wolf.howl", 1.5F, 1.0F);
					}
				}
			}
		}
	}

	@Override
	public boolean continueExecuting() {
		return howlTimer-- > 0;
	}
}