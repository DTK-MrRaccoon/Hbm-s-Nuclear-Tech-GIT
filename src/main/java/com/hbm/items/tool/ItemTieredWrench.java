package com.hbm.items.tool;

public class ItemTieredWrench extends ItemTieredTool {

	public ItemTieredWrench() {
		super(Role.WRENCH, new String[] { "bronze", "iron", "steel" }, new int[] { 0, 1, 2 }, new int[] { 60, 100, 300 });
	}
}
