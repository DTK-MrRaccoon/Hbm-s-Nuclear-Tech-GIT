package com.hbm.handler.nei;

import java.util.Map.Entry;

import com.hbm.inventory.recipes.WirecutterRecipes;
import com.hbm.items.ModItems;

import net.minecraft.item.ItemStack;

public class WirecutterRecipeHandler extends NEIUniversalHandler {

	public WirecutterRecipeHandler() {
		super("Wire Cutter", WirecutterRecipes.getRecipesForNEI(), WirecutterRecipes.getMachinesForNEI());
	}

	@Override
	public ItemStack[] getMachinesForRecipe() {
		return new ItemStack[] {
				new ItemStack(ModItems.wirecutter, 1, 0),
				new ItemStack(ModItems.wirecutter, 1, 1)
		};
	}

	@Override
	public String getKey() {
		return "ntmWirecutter";
	}

	private boolean isTierVisible(ItemStack ingredient, ItemStack req) {
		if(ingredient == null || req == null || ingredient.getItem() != req.getItem()) return false;
		return WirecutterRecipes.getToolTier(ingredient) >= WirecutterRecipes.getToolTier(req);
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		this.arecipes.clear();
		if(ingredient == null) return;
		for(Entry<Object, Object> recipe : this.recipes.entrySet()) {
			Object tool = this.machineOverrides == null ? null : this.machineOverrides.get(recipe.getKey());
			if(!(tool instanceof ItemStack)) continue;
			ItemStack req = (ItemStack) tool;
			if(isTierVisible(ingredient, req)) {
				ItemStack[][] ins = com.hbm.util.InventoryUtil.extractObject(recipe.getKey());
				ItemStack[][] outs = com.hbm.util.InventoryUtil.extractObject(recipe.getValue());
				this.arecipes.add(new RecipeSet(ins, outs, recipe.getKey()));
			}
		}
	}
}
