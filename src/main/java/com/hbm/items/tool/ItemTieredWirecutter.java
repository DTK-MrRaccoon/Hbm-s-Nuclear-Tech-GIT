package com.hbm.items.tool;

public class ItemTieredWirecutter extends ItemTieredTool {

	public ItemTieredWirecutter() {
		super(Role.WIRECUTTER, new String[] { "iron", "steel" }, new int[] { 0, 1 }, new int[] { 20, 60 });
	}
}
