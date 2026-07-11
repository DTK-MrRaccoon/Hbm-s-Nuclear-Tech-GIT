package com.hbm.items.tool;

public class ItemTieredFile extends ItemTieredTool {

	public ItemTieredFile() {
		super(Role.FILE, new String[] { "iron", "steel" }, new int[] { 0, 1 }, new int[] { 20, 60 });
	}
}
