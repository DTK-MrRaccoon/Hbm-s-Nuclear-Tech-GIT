package com.hbm.items.tool;

public class ItemTieredMortar extends ItemTieredTool {

	public ItemTieredMortar() {
		super(Role.MORTAR, new String[] { "flint", "bronze", "iron", "steel", "ferrouranium" }, new int[] { 0, 1, 1, 2, 3 }, new int[] { 20, 40, 60, 120, 240 });
	}
}
