package com.hbm.inventory.recipes;

import java.util.HashMap;
import java.util.Map.Entry;

import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemTieredMortar;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class MortarRecipes {

	private static final HashMap<Object, Object> recipes = new HashMap<Object, Object>();
	private static final HashMap<Object, Object> machines = new HashMap<Object, Object>();
	private static boolean registered;
	private static boolean craftingRegistered;

	public static void registerDefaults() {
		if(registered) return;
		registered = true;
		recipes.clear();
		machines.clear();

		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.CU.ore()) }, new ItemStack(ModItems.powder_copper));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.CU.ingot()) }, new ItemStack(ModItems.powder_copper));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.CU.plate()) }, new ItemStack(ModItems.powder_copper));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.TIN.ore()) }, new ItemStack(ModItems.powder_tin));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.TIN.ingot()) }, new ItemStack(ModItems.powder_tin));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.TIN.plate()) }, new ItemStack(ModItems.powder_tin));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.COAL.ore()) }, new ItemStack(ModItems.powder_coal));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new OreDictStack(OreDictManager.COAL.gem()) }, new ItemStack(ModItems.powder_coal));
		setRecipe(new ItemStack(ModItems.mortar, 1, 0), 0, new Object[] { new ItemStack(Items.wheat) }, new ItemStack(ModItems.flour));

		setRecipe(new ItemStack(ModItems.mortar, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.IRON.ore()) }, new ItemStack(ModItems.powder_iron));
		setRecipe(new ItemStack(ModItems.mortar, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.IRON.ingot()) }, new ItemStack(ModItems.powder_iron));
		setRecipe(new ItemStack(ModItems.mortar, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.IRON.plate()) }, new ItemStack(ModItems.powder_iron));
		setRecipe(new ItemStack(ModItems.mortar, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.PB.ore()) }, new ItemStack(ModItems.powder_lead));
		setRecipe(new ItemStack(ModItems.mortar, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.PB.ingot()) }, new ItemStack(ModItems.powder_lead));
		setRecipe(new ItemStack(ModItems.mortar, 1, 1), 1, new Object[] { new OreDictStack(OreDictManager.PB.plate()) }, new ItemStack(ModItems.powder_lead));

		setRecipe(new ItemStack(ModItems.mortar, 1, 2), 2, new Object[] { new OreDictStack(OreDictManager.TI.ore()) }, new ItemStack(ModItems.powder_titanium));
		setRecipe(new ItemStack(ModItems.mortar, 1, 2), 2, new Object[] { new OreDictStack(OreDictManager.TI.ingot()) }, new ItemStack(ModItems.powder_titanium));
		setRecipe(new ItemStack(ModItems.mortar, 1, 2), 2, new Object[] { new OreDictStack(OreDictManager.TI.plate()) }, new ItemStack(ModItems.powder_titanium));

		setRecipe(new ItemStack(ModItems.mortar, 1, 4), 4, new Object[] { new OreDictStack(OreDictManager.W.ore()) }, new ItemStack(ModItems.powder_tungsten));
		setRecipe(new ItemStack(ModItems.mortar, 1, 4), 4, new Object[] { new OreDictStack(OreDictManager.W.ingot()) }, new ItemStack(ModItems.powder_tungsten));

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

	public static void setRecipe(ItemStack mortar, Object[] inputs, ItemStack output) {
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
				if(req.getItem() == stack.getItem() && getToolTier(stack) >= getToolTier(req)) {
					return ((ItemStack) entry.getValue()).copy();
				}
			}
		}
		return null;
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

		if(stack.getItem() instanceof ItemTieredMortar) {
			return ((ItemTieredMortar) stack.getItem()).getTierLevel(stack.getItemDamage());
		}

		return stack.getItemDamage();
	}
}
