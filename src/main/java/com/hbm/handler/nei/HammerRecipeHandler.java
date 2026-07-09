package com.hbm.handler.nei;

import java.util.Map.Entry;

import com.hbm.inventory.recipes.HammerRecipes;
import com.hbm.items.ModItems;

import codechicken.nei.NEIServerUtils;
import net.minecraft.item.ItemStack;

public class HammerRecipeHandler extends NEIUniversalHandler {

	public HammerRecipeHandler() {
		super("Hammer", HammerRecipes.getRecipesForNEI(), HammerRecipes.getMachinesForNEI());
	}

	@Override
	public ItemStack[] getMachinesForRecipe() {
		return new ItemStack[] {
			new ItemStack(ModItems.hammer, 1, 0),
			new ItemStack(ModItems.hammer, 1, 1),
			new ItemStack(ModItems.hammer, 1, 2)
		};
	}

	@Override
	public String getKey() {
		return "ntmHammer";
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		if(ingredient == null) return;
		for(Entry<Object, Object> recipe : this.recipes.entrySet()) {
			Object tool = this.machineOverrides == null ? null : this.machineOverrides.get(recipe.getKey());
			if(!(tool instanceof ItemStack)) continue;
			ItemStack req = (ItemStack) tool;
			if(req.getItem() == ingredient.getItem() && ingredient.getItemDamage() >= req.getItemDamage()) {
				ItemStack[][] ins = com.hbm.util.InventoryUtil.extractObject(recipe.getKey());
				ItemStack[][] outs = com.hbm.util.InventoryUtil.extractObject(recipe.getValue());
				this.arecipes.add(new RecipeSet(ins, outs, recipe.getKey()));
			}
		}
	}
}
