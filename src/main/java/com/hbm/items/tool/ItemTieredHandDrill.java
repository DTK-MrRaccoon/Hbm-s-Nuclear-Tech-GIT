package com.hbm.items.tool;

public class ItemTieredHandDrill extends ItemTieredTool {

	public ItemTieredHandDrill() {
		super(Role.DRILL, new String[] { "bronze", "iron", "steel", "desh" }, new int[] { 0, 1, 2, 3 }, new int[] { 80, 160, 320, 0 });
	}
}
