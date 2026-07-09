package com.hbm.items.tool;

public class ItemTieredMortar extends ItemTieredTool {

	public ItemTieredMortar() {
		super(Role.MORTAR, new String[] { "flint", "iron", "tbronze", "steel" }, new int[] { 0, 1, 1, 2 }, new int[] { 64, 250, 350, 500 });
	}
}
