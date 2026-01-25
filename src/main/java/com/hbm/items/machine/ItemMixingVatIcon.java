package com.hbm.items.machine;

import java.util.List;

import com.hbm.inventory.recipes.MixingVatRecipes;
import com.hbm.inventory.recipes.MixingVatRecipes.MixingRecipe;
import com.hbm.items.ModItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

public class ItemMixingVatIcon extends Item {

	@SideOnly(Side.CLIENT)
	private IIcon[] icons;

	public ItemMixingVatIcon() {
		this.setHasSubtypes(true);
		this.setMaxDamage(0);
	}

	public String getItemStackDisplayName(ItemStack stack) {
		
		MixingRecipe recipe = MixingVatRecipes.indexMapping.get(stack.getItemDamage());
		
		if(recipe == null) {
			return super.getItemStackDisplayName(stack);
		}
		
		String s = ("" + StatCollector.translateToLocal(ModItems.mixing_vat_template.getUnlocalizedName() + ".name")).trim();
		String s1 = ("" + StatCollector.translateToLocal("mix." + recipe.name)).trim();

		if(s1 != null) {
			s = s + " " + s1;
		}

		return s;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tabs, List list) {
		for(int i = 0; i < MixingVatRecipes.recipes.size(); i++) {
			list.add(new ItemStack(item, 1, MixingVatRecipes.recipes.get(i).getId()));
		}
	}

	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		this.icons = new IIcon[MixingVatRecipes.recipes.size()];

		for(int i = 0; i < icons.length; ++i) {
			this.icons[i] = reg.registerIcon("hbm:mix_icon_" + MixingVatRecipes.recipes.get(i).name);
		}
	}

	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int i) {
		MixingRecipe rec = MixingVatRecipes.indexMapping.get(i);
		
		if(rec != null) {
			return this.icons[rec.listing % this.icons.length];
		} else {
			return ModItems.nothing.getIconFromDamage(i);
		}
	}
}