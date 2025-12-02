package com.hbm.inventory.recipes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.util.Tuple.Triplet;

import net.minecraft.item.ItemStack;

public class AlkylationRecipes extends SerializableRecipe {
	
	private static HashMap<FluidType, Triplet<FluidStack, FluidStack, FluidStack>> recipes = new HashMap<>();

	@Override
	public void registerDefaults() {
		// Useful halogenation recipes
		recipes.put(Fluids.UNSATURATEDS, new Triplet<>( // iron oxide catalyst in the unit + heat produces cyclic chains
			new FluidStack(Fluids.NONE, 0),
			new FluidStack(Fluids.AROMATICS, 70),
			new FluidStack(Fluids.PETROLEUM, 30)
		));
		recipes.put(Fluids.AROMATICS, new Triplet<>( // Dichloromethane triggers a further alkylation process, creating high-octane aromatics!
			new FluidStack(Fluids.RADIOSOLVENT, 40),
			new FluidStack(Fluids.XYLENE, 100),
			new FluidStack(Fluids.CHLORINE, 10) // Chlorine is preserved in all, but you'll need to re-irradiate here 
		));
		
		// Real world alkylation unit recipes
		recipes.put(Fluids.PETROLEUM, new Triplet<>( // Alkys are generally used to turn isobutane into gasoline
			new FluidStack(Fluids.SULFURIC_ACID, 50), // "significant volumes" of sulfuric acid are required
			new FluidStack(Fluids.LIGHTOIL, 50), // but the output is highly useful!
			new FluidStack(Fluids.PEROXIDE, 20) // "dehydrogenated sulfuric acid", aka we don't have that so sulfuric precursor, add water and sulfur to get a full loop
		));
		
		// Naphtha -> catalytic reforming -> aromatics + reformate (gasoline blend stock)
		recipes.put(Fluids.NAPHTHA, new Triplet<>(
			new FluidStack(Fluids.NONE, 0), // reforming: heat + catalyst (represented by NONE)
			new FluidStack(Fluids.AROMATICS, 50), // aromatics (reformer product)
			new FluidStack(Fluids.REFORMATE, 30) // reformate / high-value gasoline fraction
		));
		
		// Naphtha (cracked fraction) -> oxidative/promoted upgrading -> xylene
		recipes.put(Fluids.NAPHTHA_CRACK, new Triplet<>(
			new FluidStack(Fluids.RADIOSOLVENT, 40),
			new FluidStack(Fluids.XYLENE, 15),
			new FluidStack(Fluids.CHLORINE, 10)
		));
		
		// Wood pyrolysis oil -> gasification/reforming -> syngas (CO+H2)
		recipes.put(Fluids.WOODOIL, new Triplet<>(
			new FluidStack(Fluids.NONE, 0), // gasification: heat + catalyst abstraction
			new FluidStack(Fluids.SYNGAS, 25), // syngas usable for methanol
			new FluidStack(Fluids.NONE, 0)
		));
		
		// Syngas -> catalytic synthesis -> methanol + CO2 (real industrial route)
		recipes.put(Fluids.SYNGAS, new Triplet<>(
			new FluidStack(Fluids.NONE, 0), // catalytic synthesis (Cu/Zn style) abstracted as NONE
			new FluidStack(Fluids.METHANOL, 80),
			new FluidStack(Fluids.CARBONDIOXIDE, 20)
		));
		
		// Natural gas -> reforming -> syngas -> methanol route (abstracted)
		recipes.put(Fluids.GAS, new Triplet<>(
			new FluidStack(Fluids.NONE, 0), // steam reforming abstraction
			new FluidStack(Fluids.METHANOL, 70),
			new FluidStack(Fluids.CARBONDIOXIDE, 30)
		));
		
		// Ethanol -> dehydration (acid catalyst) -> unsaturates (ethylene/olefin stream) + water/waste
		recipes.put(Fluids.ETHANOL, new Triplet<>(
			new FluidStack(Fluids.SULFURIC_ACID, 75), // acid dehydration catalyst
			new FluidStack(Fluids.UNSATURATEDS, 40),
			new FluidStack(Fluids.PEROXIDE, 15)
		));
		
		// XYLENE / AROMATICS processing -> detergent/solvent stream + small gas byproduct (cleanup)
		recipes.put(Fluids.AROMATICS, new Triplet<>(
			new FluidStack(Fluids.RADIOSOLVENT, 80),
			new FluidStack(Fluids.XYLENE, 60),
			new FluidStack(Fluids.CHLORINE, 10)
		));

	}
	
	
	
	public static Triplet<FluidStack, FluidStack, FluidStack> getOutput(FluidType type) {
		return recipes.get(type);
	}
	
	public static HashMap<Object, Object[]> getRecipes() {

		HashMap<Object, Object[]> map = new HashMap<Object, Object[]>();
		
		for(Entry<FluidType, Triplet<FluidStack, FluidStack, FluidStack>> recipe : recipes.entrySet()) {
			ItemStack[] inputs = recipe.getValue().getX().type == Fluids.NONE
				? new ItemStack[] { ItemFluidIcon.make(recipe.getKey(), 1000) }
				: new ItemStack[] {
					ItemFluidIcon.make(recipe.getKey(), 1000),
					ItemFluidIcon.make(recipe.getValue().getX().type,	recipe.getValue().getX().fill * 10) }; // this nesting level is bird-behaviour

			map.put(inputs,
				new ItemStack[] {
					ItemFluidIcon.make(recipe.getValue().getY().type,	recipe.getValue().getY().fill * 10),
					ItemFluidIcon.make(recipe.getValue().getZ().type,	recipe.getValue().getZ().fill * 10) });
		}
		
		return map;
	}

	@Override
	public String getFileName() {
		return "hbmAlkylation.json";
	}

	@Override
	public Object getRecipeObject() {
		return recipes;
	}

	@Override
	public void readRecipe(JsonElement recipe) {
		JsonObject obj = (JsonObject) recipe;

		FluidType input = Fluids.fromName(obj.get("input").getAsString());
		FluidStack acid = readFluidStack(obj.get("acid").getAsJsonArray());
		FluidStack output1 = readFluidStack(obj.get("output1").getAsJsonArray());
		FluidStack output2 = readFluidStack(obj.get("output2").getAsJsonArray());
		
		recipes.put(input, new Triplet<>(acid, output1, output2));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void writeRecipe(Object recipe, JsonWriter writer) throws IOException {
		Entry<FluidType, Triplet<FluidStack, FluidStack, FluidStack>> rec = (Entry<FluidType, Triplet<FluidStack, FluidStack, FluidStack>>) recipe;
		
		writer.name("input").value(rec.getKey().getName());
		writer.name("acid"); writeFluidStack(rec.getValue().getX(), writer);
		writer.name("output1"); writeFluidStack(rec.getValue().getY(), writer);
		writer.name("output2"); writeFluidStack(rec.getValue().getZ(), writer);
	}

	@Override
	public void deleteRecipes() {
		recipes.clear();
	}
}