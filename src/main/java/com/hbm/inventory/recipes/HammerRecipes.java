package com.hbm.inventory.recipes;

import java.util.HashMap;
import java.util.Map.Entry;

import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.ModItems;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class HammerRecipes {

	private static final HashMap<Object, Object> recipes = new HashMap<Object, Object>();
	private static final HashMap<Object, Object> machines = new HashMap<Object, Object>();
	private static boolean registered;

	public static void registerDefaults() {
		if(registered) return;
		registered = true;
		recipes.clear();
		machines.clear();

		setRecipe(new ItemStack(ModItems.hammer, 1, 1), 0, new Object[] { new OreDictStack(OreDictManager.TBRONZE.ingot()) }, new ItemStack(ModItems.plate_tin_bronze));
		setRecipe(new ItemStack(ModItems.hammer, 1, 1), 0, new Object[] { new OreDictStack(OreDictManager.TBRONZE.plate()) }, new ItemStack(ModItems.bronze_parts));

		setRecipe(new ItemStack(ModItems.hammer, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.CU.ingot()) }, new ItemStack(ModItems.plate_copper));
		setRecipe(new ItemStack(ModItems.hammer, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.TIN.ingot()) }, new ItemStack(ModItems.plate_tin));

		setRecipe(new ItemStack(ModItems.hammer, 1, 2), 2, new Object[] { new OreDictStack(OreDictManager.IRON.ingot()) }, new ItemStack(ModItems.plate_iron));
		setRecipe(new ItemStack(ModItems.hammer, 1, 2), 2, new Object[] { new OreDictStack(OreDictManager.STEEL.ingot()) }, new ItemStack(ModItems.plate_steel));
	}

	public static HashMap<Object, Object> getRecipesForNEI() {
		registerDefaults();
		return recipes;
	}

	public static HashMap<Object, Object> getMachinesForNEI() {
		registerDefaults();
		return machines;
	}

	public static void setRecipe(ItemStack hammer, Object[] inputs, ItemStack output) {
		setRecipe(hammer, hammer == null ? 0 : hammer.getItemDamage(), inputs, output);
	}

	public static void setRecipe(ItemStack hammer, int tier, Object[] inputs, ItemStack output) {
		if(hammer == null || inputs == null || output == null || inputs.length == 0) return;
		ItemStack tool = hammer.copy();
		tool.setItemDamage(Math.max(0, tier));
		Object[] key = new Object[inputs.length];
		for(int i = 0; i < inputs.length; i++) {
			key[i] = inputs[i];
		}
		recipes.put(key, output.copy());
		machines.put(key, tool);
	}

	public static ItemStack getOutput(ItemStack stack) {
		registerDefaults();
		if(stack == null || stack.getItem() == null) return null;
		for(Entry<Object, Object> entry : recipes.entrySet()) {
			Object tool = machines.get(entry.getKey());
			if(tool instanceof ItemStack) {
				ItemStack req = (ItemStack) tool;
				if(req.getItem() == stack.getItem() && stack.getItemDamage() >= req.getItemDamage()) {
					return ((ItemStack) entry.getValue()).copy();
				}
			}
		}
		return null;
	}
}
