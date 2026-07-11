package com.hbm.inventory.recipes;

import java.util.HashMap;
import java.util.Map.Entry;

import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemTieredWirecutter;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class WirecutterRecipes {

	private static final HashMap<Object, Object> recipes = new HashMap<Object, Object>();
	private static final HashMap<Object, Object> machines = new HashMap<Object, Object>();
	private static boolean registered;
	private static boolean craftingRegistered;

	public static void registerDefaults() {
		if(registered) return;
		registered = true;
		recipes.clear();
		machines.clear();

		setRecipe(new ItemStack(ModItems.wirecutter, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.CU.plate()) }, Mats.MAT_COPPER.make(ModItems.wire_fine, 3));
		setRecipe(new ItemStack(ModItems.wirecutter, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.TIN.plate()) }, Mats.MAT_TIN.make(ModItems.wire_fine, 3));
		setRecipe(new ItemStack(ModItems.wirecutter, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.GOLD.plate()) }, Mats.MAT_GOLD.make(ModItems.wire_fine, 3));
		setRecipe(new ItemStack(ModItems.wirecutter, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.AL.plate()) }, Mats.MAT_ALUMINIUM.make(ModItems.wire_fine, 3));

		setRecipe(new ItemStack(ModItems.wirecutter, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.STEEL.plate()) }, Mats.MAT_STEEL.make(ModItems.wire_fine, 3));
		setRecipe(new ItemStack(ModItems.wirecutter, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.W.ingot()) }, Mats.MAT_TUNGSTEN.make(ModItems.wire_fine, 3));
		setRecipe(new ItemStack(ModItems.wirecutter, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.ALLOY.plate()) }, Mats.MAT_ALLOY.make(ModItems.wire_fine, 3));

		registerCraftingRecipes();
	}

	private static void registerCraftingRecipes() {
		if(craftingRegistered) return;
		craftingRegistered = true;

		for(Entry<Object, Object> entry : recipes.entrySet()) {
			Object toolObject = machines.get(entry.getKey());
			if(!(toolObject instanceof ItemStack)) continue;

			ItemStack requiredTool = (ItemStack) toolObject;
			Object[] inputs = (Object[]) entry.getKey();
			ItemStack output = ((ItemStack) entry.getValue()).copy();

			GameRegistry.addRecipe(new CraftingRecipe(output, requiredTool, getToolTier(requiredTool), inputs));
		}
	}

	private static class CraftingRecipe implements IRecipe {

		private final ItemStack output;
		private final Object[] inputs;
		private final ItemStack tool;
		private final int requiredTier;

		private CraftingRecipe(ItemStack output, ItemStack tool, int requiredTier, Object[] inputs) {
			this.output = output == null ? null : output.copy();
			this.tool = tool == null ? null : tool.copy();
			this.requiredTier = requiredTier;
			this.inputs = inputs == null ? new Object[0] : inputs.clone();
		}

		@Override
		public boolean matches(InventoryCrafting inv, World world) {
			boolean foundTool = false;
			ItemStack ingredient = null;
			int ingredientSlots = 0;

			for(int i = 0; i < inv.getSizeInventory(); i++) {
				ItemStack stack = inv.getStackInSlot(i);
				if(stack == null) continue;

				if(isValidTool(stack)) {
					if(foundTool) return false;
					foundTool = true;
				} else {
					if(ingredientSlots > 0) return false;
					ingredient = stack;
					ingredientSlots++;
				}
			}

			if(!foundTool || ingredient == null) return false;
			return matchesSingleInput(this.inputs, ingredient);
		}

		@Override
		public ItemStack getCraftingResult(InventoryCrafting inv) {
			return this.output == null ? null : this.output.copy();
		}

		@Override
		public int getRecipeSize() {
			return this.inputs.length + 1;
		}

		@Override
		public ItemStack getRecipeOutput() {
			return this.output == null ? null : this.output.copy();
		}

		private boolean isValidTool(ItemStack stack) {
			if(stack == null || this.tool == null) return false;
			return stack.getItem() == this.tool.getItem() && getToolTier(stack) >= this.requiredTier;
		}
	}

	public static HashMap<Object, Object> getRecipesForNEI() {
		registerDefaults();
		return recipes;
	}

	public static HashMap<Object, Object> getMachinesForNEI() {
		registerDefaults();
		return machines;
	}

	public static void setRecipe(ItemStack wirecutter, Object[] inputs, ItemStack output) {
		setRecipe(wirecutter, wirecutter == null ? 0 : wirecutter.getItemDamage(), inputs, output);
	}

	public static void setRecipe(ItemStack wirecutter, int tier, Object[] inputs, ItemStack output) {
		if(wirecutter == null || inputs == null || output == null || inputs.length == 0) return;
		ItemStack tool = wirecutter.copy();
		tool.setItemDamage(Math.max(0, tier));
		Object[] key = new Object[inputs.length];
		for(int i = 0; i < inputs.length; i++) {
			key[i] = inputs[i];
		}
		recipes.put(key, output.copy());
		machines.put(key, tool);
	}

	public static ItemStack getOutput(ItemStack ingredient, ItemStack tool) {
		registerDefaults();
		if(ingredient == null || tool == null || ingredient.getItem() == null || tool.getItem() == null) return null;
		for(Entry<Object, Object> entry : recipes.entrySet()) {
			Object[] inputs = (Object[]) entry.getKey();
			Object toolObject = machines.get(entry.getKey());
			if(!(toolObject instanceof ItemStack)) continue;

			ItemStack reqTool = (ItemStack) toolObject;
			if(tool.getItem() != reqTool.getItem() || getToolTier(tool) < getToolTier(reqTool)) continue;

			if(matchesSingleInput(inputs, ingredient)) {
				return ((ItemStack) entry.getValue()).copy();
			}
		}
		return null;
	}

	private static boolean matchesSingleInput(Object[] inputs, ItemStack stack) {
		if(inputs == null || inputs.length == 0 || stack == null) return false;
		for(int i = 0; i < inputs.length; i++) {
			Object input = inputs[i];
			if(input instanceof OreDictStack && ((OreDictStack) input).matchesRecipe(stack, false)) return true;
			if(input instanceof ComparableStack && ((ComparableStack) input).matchesRecipe(stack, false)) return true;
			if(input instanceof AStack && ((AStack) input).matchesRecipe(stack, false)) return true;
			if(input instanceof ItemStack) {
				ItemStack req = (ItemStack) input;
				if(OreDictionary.itemMatches(req, stack, false) && stack.stackSize >= Math.max(1, req.stackSize)) return true;
			}
		}
		return false;
	}

	public static int getToolTier(ItemStack stack) {
		if(stack == null || stack.getItem() == null) return -1;

		if(stack.getItem() instanceof ItemTieredWirecutter) {
			return ((ItemTieredWirecutter) stack.getItem()).getTierLevel(stack.getItemDamage());
		}

		return stack.getItemDamage();
	}
}
