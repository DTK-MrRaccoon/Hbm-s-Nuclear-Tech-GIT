package com.hbm.items.tool;

public class ItemTieredHammer extends ItemTieredTool {

	public ItemTieredHammer() {
		super(Role.HAMMER, new String[] { "bronze", "iron", "steel" }, new int[] { 0, 1, 2 }, new int[] { 250, 500, 1000 });
	}
}
