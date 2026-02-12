package com.hbm.items.armor;

import java.util.List;

import com.hbm.handler.ArmorModHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class ItemModGloves extends ItemArmorMod {
	
	public static final double RAD_RESISTANCE = 0.05;
	
	public ItemModGloves() {
		super(ArmorModHandler.legs_only, false, true, false, false);
	}
	
	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean bool) {
		list.add(EnumChatFormatting.YELLOW + "+" + RAD_RESISTANCE + " rad-resistance");
		list.add("");
		list.add(EnumChatFormatting.DARK_PURPLE + "Protects against hot items; use with reacher");
		list.add("");
		super.addInformation(itemstack, player, list, bool);
	}

	@Override
	public boolean isValidArmor(ItemStack stack, int armorType, Entity entity) {
		return armorType == 1;
	}
}