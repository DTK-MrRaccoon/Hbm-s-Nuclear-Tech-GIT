package com.hbm.inventory.recipes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.FluidStack;
import static com.hbm.inventory.OreDictManager.*;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.ItemEnums;
import com.hbm.main.MainRegistry;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class MixingVatRecipes extends SerializableRecipe {

	public static HashMap<Integer, MixingRecipe> indexMapping = new HashMap<>();
	public static List<MixingRecipe> recipes = new ArrayList<>();

	@Override
	public void registerDefaults() {
	
		//acids 1-10
		recipes.add(new MixingRecipe(1, "SULFURIC_ACID", 600)
				.inputItems(new OreDictStack(S.dust()))
				.inputFluids(new FluidStack(Fluids.PEROXIDE, 1200), new FluidStack(Fluids.WATER, 2000))
				.outputFluids(new FluidStack(Fluids.SULFURIC_ACID, 2000)));
	
		recipes.add(new MixingRecipe(2, "NITRIC_ACID", 600)
				.inputItems(new OreDictStack(KNO.dust()))
				.inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 1000))
				.outputFluids(new FluidStack(Fluids.NITRIC_ACID, 2000)));
	
		recipes.add(new MixingRecipe(3, "PHOSPHORIC_ACID", 300)
				.inputItems(new ComparableStack(ModItems.powder_fire, 2))
				.inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 750))
				.outputFluids(new FluidStack(Fluids.PHOSPHORIC_ACID, 1000)));
	
		recipes.add(new MixingRecipe(4, "HYDROFLUORIC_ACID", 200)
				.inputFluids(new FluidStack(Fluids.LIQUID_FLUORITE, 200), new FluidStack(Fluids.SULFURIC_ACID, 800))
				.inputItems(new OreDictStack(KNO.dust(), 1))
				.outputFluids(new FluidStack(Fluids.HYDROFLUORIC_ACID, 1000)));

		//fuels 11-30
		recipes.add(new MixingRecipe(11, "PETROIL", 300)
				.inputFluids(new FluidStack(Fluids.RECLAIMED, 800), new FluidStack(Fluids.LUBRICANT, 200))
				.outputFluids(new FluidStack(Fluids.PETROIL, 1000)));
	
		recipes.add(new MixingRecipe(12, "PETROIL_LEADED", 600)
				.inputItems(new ComparableStack(ModItems.fuel_additive, 1))
				.inputFluids(new FluidStack(Fluids.PETROIL, 10000))
				.outputFluids(new FluidStack(Fluids.PETROIL_LEADED, 12000)));
	
		recipes.add(new MixingRecipe(13, "GASOLINE_LEADED", 600)
				.inputItems(new ComparableStack(ModItems.fuel_additive, 1))
				.inputFluids(new FluidStack(Fluids.GASOLINE, 10000))
				.outputFluids(new FluidStack(Fluids.GASOLINE_LEADED, 12000)));
	
		recipes.add(new MixingRecipe(14, "COALGAS_LEADED", 600)
				.inputItems(new ComparableStack(ModItems.fuel_additive, 1))
				.inputFluids(new FluidStack(Fluids.COALGAS, 10000))
				.outputFluids(new FluidStack(Fluids.COALGAS_LEADED, 12000)));
	
		recipes.add(new MixingRecipe(15, "BIOFUEL", 150)
				.inputFluids(new FluidStack(Fluids.BIOGAS, 1500), new FluidStack(Fluids.ETHANOL, 250))
				.outputFluids(new FluidStack(Fluids.BIOFUEL, 1250)));
	
		//nuclear stuff 31-50
		recipes.add(new MixingRecipe(31, "LIQUIDFLUORITE", 300)
				.inputItems(new OreDictStack(F.dust(), 8))
				.inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 2500), new FluidStack(Fluids.PHOSPHORIC_ACID, 1500))
				.outputFluids(new FluidStack(Fluids.LIQUID_FLUORITE, 4000)));
	
		recipes.add(new MixingRecipe(32, "YELLOWCAKE", 250)
				.inputItems(new OreDictStack(U.billet(), 2))
				.inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 1000), new FluidStack(Fluids.PEROXIDE, 500))
				.outputItems(new ItemStack(ModItems.powder_yellowcake)));
	
		recipes.add(new MixingRecipe(33, "URANIUM_OXIDE_SLURRY", 150)
				.inputItems(new ComparableStack(ModItems.powder_yellowcake))
				.inputFluids(new FluidStack(Fluids.WATER, 1000))
				.outputFluids(new FluidStack(Fluids.URANIUM_OXIDE_SLURRY, 1000)));

		recipes.add(new MixingRecipe(34, "GREEN_SALT", 300)
				.inputFluids(new FluidStack(Fluids.URANIUM_OXIDE_SLURRY, 1000), new FluidStack(Fluids.FLUORINE_GAS, 1000))
				.outputItems(new ItemStack(ModItems.powder_green_salt)));

		recipes.add(new MixingRecipe(35, "UF6", 400)
				.inputItems(new ComparableStack(ModItems.powder_green_salt))
				.inputFluids(new FluidStack(Fluids.FLUORINE_GAS, 1000))
				.outputFluids(new FluidStack(Fluids.UF6, 1200)));
	
		recipes.add(new MixingRecipe(36, "PUF6", 150)
				.inputItems(new OreDictStack(PU.dust()), new OreDictStack(F.dust(), 3))
				.inputFluids(new FluidStack(Fluids.WATER, 1000))
				.outputFluids(new FluidStack(Fluids.PUF6, 900)));
	
		//normal mixing 51-100
		recipes.add(new MixingRecipe(51, "COOLANT", 150)
				.inputItems(new OreDictStack(KNO.dust()))
				.inputFluids(new FluidStack(Fluids.WATER, 1800))
				.outputFluids(new FluidStack(Fluids.COOLANT, 2000)));
	
		recipes.add(new MixingRecipe(52, "CRYOGEL", 150)
				.inputItems(new ComparableStack(ModItems.powder_ice))
				.inputFluids(new FluidStack(Fluids.COOLANT, 1800))
				.outputFluids(new FluidStack(Fluids.CRYOGEL, 2000)));
	
		recipes.add(new MixingRecipe(53, "NUTRIENTPASTE", 3000)
				.inputItems(new ComparableStack(Items.rotten_flesh, 8))
				.inputFluids(new FluidStack(Fluids.SULFURIC_ACID, 2000))
				.outputFluids(new FluidStack(Fluids.NUTRIENTPASTE, 8000)));
	
		//processing 101-200
		recipes.add(new MixingRecipe(101, "AQUEOUS_COPPER", 300)
				.inputItems(new ComparableStack(ModItems.chunk_ore, 1, ItemEnums.EnumChunkType.MALACHITE.ordinal()))
				.inputFluids(new FluidStack(Fluids.SODIUM_HYDROXIDE, 250))
				.outputFluids(new FluidStack(Fluids.AQUEOUS_COPPER, 1000)));
	}

	public static class MixingRecipe {
		public int listing;
		public int id;
		public String name;
		public AStack[] inputs;
		public FluidStack[] inputFluids;
		public ItemStack[] outputs;
		public FluidStack[] outputFluids;
		public int duration;

		public MixingRecipe(int index, String name, int duration) {
			this.id = index;
			this.name = name;
			this.duration = duration;
			this.listing = recipes.size();

			this.inputs = new AStack[4];
			this.outputs = new ItemStack[4];
			this.inputFluids = new FluidStack[2];
			this.outputFluids = new FluidStack[2];

			if(!indexMapping.containsKey(id)) {
				indexMapping.put(id, this);
			} else {
				throw new IllegalStateException("Mixing vat recipe " + name + " has been registered with duplicate id " + id + " used by " + indexMapping.get(id).name + "!");
			}
		}

		public MixingRecipe inputItems(AStack... in) {
			for(int i = 0; i < in.length && i < 4; i++) this.inputs[i] = in[i];
			return this;
		}

		public MixingRecipe inputFluids(FluidStack... in) {
			for(int i = 0; i < in.length && i < 2; i++) this.inputFluids[i] = in[i];
			return this;
		}

		public MixingRecipe outputItems(ItemStack... out) {
			for(int i = 0; i < out.length && i < 4; i++) this.outputs[i] = out[i];
			return this;
		}

		public MixingRecipe outputFluids(FluidStack... out) {
			for(int i = 0; i < out.length && i < 2; i++) this.outputFluids[i] = out[i];
			return this;
		}

		public int getId() {
			return this.id;
		}

		public int getDuration() {
			return this.duration;
		}
	}

	@Override
	public String getFileName() {
		return "hbmMixingVat.json";
	}

	@Override
	public Object getRecipeObject() {
		return recipes;
	}

	@Override
	public void readRecipe(JsonElement recipe) {
		JsonObject obj = (JsonObject) recipe;
		int id = obj.get("id").getAsInt();
		String name = obj.get("name").getAsString();
		int duration = obj.get("duration").getAsInt();

		recipes.add(new MixingRecipe(id, name, duration)
				.inputFluids(this.readFluidArray((JsonArray) obj.get("fluidInput")))
				.inputItems(this.readAStackArray((JsonArray) obj.get("itemInput")))
				.outputFluids(this.readFluidArray((JsonArray) obj.get("fluidOutput")))
				.outputItems(this.readItemStackArray((JsonArray) obj.get("itemOutput"))));
	}

	@Override
	public void writeRecipe(Object recipe, JsonWriter writer) throws IOException {
		try {
			MixingRecipe mix = (MixingRecipe) recipe;
			writer.name("id").value(mix.id);
			writer.name("name").value(mix.name);
			writer.name("duration").value(mix.duration);
			
			writer.name("fluidInput").beginArray();
			for(FluidStack input : mix.inputFluids) { 
				if(input != null) this.writeFluidStack(input, writer); 
			}
			writer.endArray();
			
			writer.name("itemInput").beginArray();
			for(AStack input : mix.inputs) { 
				if(input != null) this.writeAStack(input, writer); 
			}
			writer.endArray();
			
			writer.name("fluidOutput").beginArray();
			for(FluidStack output : mix.outputFluids) { 
				if(output != null) this.writeFluidStack(output, writer); 
			}
			writer.endArray();
			
			writer.name("itemOutput").beginArray();
			for(ItemStack output : mix.outputs) { 
				if(output != null) this.writeItemStack(output, writer); 
			}
			writer.endArray();
		} catch(Exception ex) {
			MainRegistry.logger.error(ex);
			ex.printStackTrace();
		}
	}

	@Override
	public void deleteRecipes() {
		indexMapping.clear();
		recipes.clear();
	}
}