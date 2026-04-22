package com.hbm.items.machine;

import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.util.ChatBuilder;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class ItemReactorSensor extends Item {

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		Block b = world.getBlock(x, y, z);
		String type = null;

		if(b == ModBlocks.machine_reactor_small || b == ModBlocks.dummy_block_reactor_small || b == ModBlocks.dummy_port_reactor_small) {
			type = "Small Reactor";
		}

		else if(b == ModBlocks.reactor_research) {
			type = "Research Reactor";
		}

		if(type != null) {
			setPosition(stack, x, y, z, type, player, world);
			return true;
		}

		return false;
	}

	private void setPosition(ItemStack stack, int x, int y, int z, String type, EntityPlayer player, World world) {
		if(stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
		stack.stackTagCompound.setInteger("x", x);
		stack.stackTagCompound.setInteger("y", y);
		stack.stackTagCompound.setInteger("z", z);
		stack.stackTagCompound.setString("type", type);

		if(!world.isRemote) {
			player.addChatMessage(ChatBuilder.start("[").color(EnumChatFormatting.DARK_AQUA)
					.nextTranslation(this.getUnlocalizedName() + ".name").color(EnumChatFormatting.DARK_AQUA)
					.next("] ").color(EnumChatFormatting.DARK_AQUA)
					.next("Position set! (" + type + ")").color(EnumChatFormatting.GREEN).flush());
		}
		world.playSoundAtEntity(player, "hbm:item.techBoop", 1.0F, 1.0F);
		player.swingItem();
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		if(stack.stackTagCompound != null) {
			String type = stack.stackTagCompound.getString("type");
			if(!type.isEmpty()) {
				list.add(EnumChatFormatting.YELLOW + "Type: " + type);
			}
			list.add("x: " + stack.stackTagCompound.getInteger("x"));
			list.add("y: " + stack.stackTagCompound.getInteger("y"));
			list.add("z: " + stack.stackTagCompound.getInteger("z"));
		} else {
			list.add("No reactor selected!");
		}
	}
}
