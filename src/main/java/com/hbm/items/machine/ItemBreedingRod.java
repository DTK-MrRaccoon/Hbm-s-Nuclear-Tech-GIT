package com.hbm.items.machine;

import java.util.List;

import com.hbm.items.ItemEnumMulti;
import com.hbm.items.ModItems;
import com.hbm.util.EnumUtil;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;

public class ItemBreedingRod extends ItemEnumMulti {

	public ItemBreedingRod() {
		super(BreedingRodType.class, true, true);
		this.canRepair = false;
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for (BreedingRodType type : BreedingRodType.values()) {
			ItemStack stack = new ItemStack(item, 1, type.ordinal());
			initRodNBT(stack);
			list.add(stack);
		}
	}

	private static void initRodNBT(ItemStack stack) {
		if (stack == null) return;
		if (!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		BreedingRodType type = EnumUtil.grabEnumSafely(BreedingRodType.class, stack.getItemDamage());
		stack.stackTagCompound.setInteger("maxLife", type.maxLife);
		stack.stackTagCompound.setInteger("life", type.maxLife);
	}

	public static int getLifeTime(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemBreedingRod)) return 0;
		if (!stack.hasTagCompound()) initRodNBT(stack);
		return stack.stackTagCompound.getInteger("life");
	}

	public static void setLifeTime(ItemStack stack, int life) {
		if (stack == null || !(stack.getItem() instanceof ItemBreedingRod)) return;
		if (!stack.hasTagCompound()) stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setInteger("life", life);
	}

	public static int getMaxLife(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemBreedingRod)) return 0;
		if (!stack.hasTagCompound()) initRodNBT(stack);
		return stack.stackTagCompound.getInteger("maxLife");
	}

	public static int getHeatPerTick(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemBreedingRod)) return 0;
		BreedingRodType type = EnumUtil.grabEnumSafely(BreedingRodType.class, stack.getItemDamage());
		int baseHeat = type.baseHeat;
		Item item = stack.getItem();
		if (item == ModItems.rod_dual) baseHeat *= 2;
		else if (item == ModItems.rod_quad) baseHeat *= 4;
		return baseHeat;
	}

	public static boolean isFuelRod(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemBreedingRod)) return false;
		BreedingRodType type = EnumUtil.grabEnumSafely(BreedingRodType.class, stack.getItemDamage());
		return type.isFuel;
	}

	public static boolean isBreedingRod(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemBreedingRod)) return false;
		BreedingRodType type = EnumUtil.grabEnumSafely(BreedingRodType.class, stack.getItemDamage());
		return type.isBreeding;
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		int max = getMaxLife(stack);
		return max > 0 && getLifeTime(stack) < max;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		int max = getMaxLife(stack);
		if (max == 0) return 0;
		return 1.0 - (double) getLifeTime(stack) / (double) max;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		super.addInformation(stack, player, list, bool);
		int life = getLifeTime(stack);
		int max = getMaxLife(stack);
		if (max > 0) {
			int percent = (int) ((double) life / max * 100);
			list.add(EnumChatFormatting.YELLOW + "Remaining: " + life + " / " + max + " (" + percent + "%)");
		}
		BreedingRodType type = EnumUtil.grabEnumSafely(BreedingRodType.class, stack.getItemDamage());
		if (type.isFuel) {
			list.add(EnumChatFormatting.DARK_GREEN + "Fuel Rod");
		} else if (type.isBreeding) {
			list.add(EnumChatFormatting.DARK_GREEN + "Breeding Rod");
		}
		int heat = getHeatPerTick(stack);
		if (heat > 0) {
			list.add(EnumChatFormatting.GOLD + "Heat: " + heat + " per tick");
		}
	}

	public enum BreedingRodType {
		LITHIUM(2000 * 20, 0, false, true),
		TRITIUM(0, 0, false, false),
		CO(1000 * 20, 0, false, true),
		CO60(0, 5, false, false),
		TH232(5000 * 20, 0, false, true),
		THF(2500 * 20, 60, true, false),
		U235(3000 * 20, 75, true, false),
		NP237(2000 * 20, 0, false, true),
		PU238(0, 80, false, false),
		U238(3000 * 20, 0, false, true),
		PU239(10000 * 20, 100, true, false),
		RGP(2000 * 20, 90, true, false),
		WASTE(0, 5, false, false),
		LEAD(0, 0, false, false),
		URANIUM(2000 * 20, 40, true, false),
		RA226(3000 * 20, 0, false, true),
		AC227(2500 * 20, 0, false, true);

		public final int maxLife;
		public final int baseHeat;
		public final boolean isFuel;
		public final boolean isBreeding;
		
		public BreedingRodType outputType;
		public float outputChance = 1.0f;
		public BreedingRodType alternateOutput;

		BreedingRodType(int maxLife, int baseHeat, boolean isFuel, boolean isBreeding) {
			this.maxLife = maxLife;
			this.baseHeat = baseHeat;
			this.isFuel = isFuel;
			this.isBreeding = isBreeding;
		}

		static {
			LITHIUM.outputType = TRITIUM;
			CO.outputType = CO60;
			TH232.outputType = THF;
			THF.outputType = WASTE;
			U235.outputType = WASTE;
			NP237.outputType = PU238; NP237.outputChance = 0.5f; NP237.alternateOutput = WASTE;
			U238.outputType = PU239;
			PU239.outputType = WASTE;
			RGP.outputType = WASTE;
			URANIUM.outputType = WASTE;
			RA226.outputType = AC227;
			AC227.outputType = WASTE;
		}
	}
}