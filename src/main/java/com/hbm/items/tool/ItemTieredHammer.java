package com.hbm.items.tool;

public class ItemTieredHammer extends ItemTieredTool {

	public ItemTieredHammer() {
		super(Role.HAMMER, new String[] { "bronze", "iron", "steel", "ferrouranium" }, new int[] { 0, 1, 2, 3 }, new int[] { 80, 120, 240, 480 });
	}
}
