package com.hbm.items.tool;

public class ItemTieredWrench extends ItemTieredTool {

	public ItemTieredWrench() {
		super(Role.WRENCH, new String[] { "iron", "bronze", "steel" }, new int[] { 0, 1, 2 }, new int[] { 250, 500, 1000 });
	}
}
