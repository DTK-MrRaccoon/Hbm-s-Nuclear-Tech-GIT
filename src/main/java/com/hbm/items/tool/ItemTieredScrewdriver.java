package com.hbm.items.tool;

public class ItemTieredScrewdriver extends ItemTieredTool {

	public ItemTieredScrewdriver() {
		super(Role.SCREWDRIVER, new String[] { "iron", "bronze", "steel", "desh" }, new int[] { 0, 1, 2, 3 }, new int[] { 120, 240, 480, 0 });
	}
}
