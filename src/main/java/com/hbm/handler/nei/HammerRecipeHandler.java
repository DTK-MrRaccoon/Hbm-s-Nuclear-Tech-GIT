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

	private boolean isTierVisible(ItemStack ingredient, ItemStack req) {
		if(ingredient == null || req == null || ingredient.getItem() != req.getItem()) return false;
		return HammerRecipes.getToolTier(ingredient) >= HammerRecipes.getToolTier(req);
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		if(result == null) return;
		super.loadCraftingRecipes(result);
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
