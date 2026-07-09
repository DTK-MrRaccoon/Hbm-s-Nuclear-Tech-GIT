package com.hbm.inventory.recipes;

import java.util.HashMap;
import java.util.Map.Entry;

import com.hbm.inventory.OreDictManager;
import com.hbm.items.ModItems;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;

public class MortarRecipes {

	private static final HashMap<Object, Object> recipes = new HashMap<Object, Object>();
	private static final HashMap<Object, Object> machines = new HashMap<Object, Object>();
	private static boolean registered;

	public static void registerDefaults() {
		if(registered) return;
		registered = true;
		recipes.clear();
		machines.clear();

		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.CU.ore()) }, new ItemStack(ModItems.powder_copper));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.TIN.ore()) }, new ItemStack(ModItems.powder_tin));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.COAL.ore()) }, new ItemStack(ModItems.powder_coal));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new ItemStack(Items.coal) }, new ItemStack(ModItems.powder_coal));

		setRecipe(new ItemStack(ModItems.mortar, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.IRON.ore()) }, new ItemStack(ModItems.powder_iron));
		setRecipe(new ItemStack(ModItems.mortar, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.PB.ore()) }, new ItemStack(ModItems.powder_lead));

		setRecipe(new ItemStack(ModItems.mortar, 1, 2), 2, new Object[] { new OreDictStack(OreDictManager.TI.ore()) }, new ItemStack(ModItems.powder_titanium));
		setRecipe(new ItemStack(ModItems.mortar, 1, 2), 2, new Object[] { new OreDictStack(OreDictManager.W.ore()) }, new ItemStack(ModItems.powder_tungsten));
	}

	public static HashMap<Object, Object> getRecipesForNEI() {
		registerDefaults();
		return recipes;
	}

	public static HashMap<Object, Object> getMachinesForNEI() {
		registerDefaults();
		return machines;
	}

	public static void setRecipe(ItemStack mortar, ItemStack[] inputs, ItemStack output) {
		setRecipe(mortar, mortar == null ? 0 : mortar.getItemDamage(), inputs, output);
	}

	public static void setRecipe(ItemStack mortar, int tier, Object[] inputs, ItemStack output) {
		if(mortar == null || inputs == null || output == null || inputs.length == 0) return;
		ItemStack tool = mortar.copy();
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
