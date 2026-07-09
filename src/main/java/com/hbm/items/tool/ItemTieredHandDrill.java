package com.hbm.items.tool;

public class ItemTieredHandDrill extends ItemTieredTool {

	public ItemTieredHandDrill() {
		super(Role.DRILL, new String[] { "iron", "bronze", "steel", "desh" }, new int[] { 0, 1, 2, 3 }, new int[] { 180, 360, 720, 0 });
	}
}
