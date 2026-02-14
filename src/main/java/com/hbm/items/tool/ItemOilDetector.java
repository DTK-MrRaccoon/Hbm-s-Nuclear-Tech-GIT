package com.hbm.items.tool;

import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.PlayerInformPacket;
import com.hbm.util.ChatBuilder;
import com.hbm.util.i18n.I18nUtil;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class ItemOilDetector extends Item {

	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer player, List list, boolean bool) {
		list.add(I18nUtil.format(this.getUnlocalizedName() + ".desc1"));
		list.add(I18nUtil.format(this.getUnlocalizedName() + ".desc2"));
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {

		boolean oil = false;
		boolean direct = false;
		boolean highGrade = false;
		boolean nearHighGrade = false;

		int x = (int)player.posX;
		int y = (int)player.posY;
		int z = (int)player.posZ;

		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x, i, z);
			if(b == ModBlocks.ore_oil) direct = true;
			if(b == ModBlocks.ore_oil_rus) { direct = true; highGrade = true; }
		}

		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x + 5, i, z);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x - 5, i, z);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x, i, z + 5);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x, i, z - 5);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 10; i--) {
			Block b = world.getBlock(x + 10, i, z);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 10; i--) {
			Block b = world.getBlock(x - 10, i, z);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 10; i--) {
			Block b = world.getBlock(x, i, z + 10);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 10; i--) {
			Block b = world.getBlock(x, i, z - 10);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x + 5, i, z + 5);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}
		
		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x - 5, i, z + 5);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x + 5, i, z - 5);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		for(int i = y + 15; i > 5; i--) {
			Block b = world.getBlock(x - 5, i, z - 5);
			if(b == ModBlocks.ore_oil) oil = true;
			if(b == ModBlocks.ore_oil_rus) { oil = true; nearHighGrade = true; }
		}

		if(direct)
			oil = true;

		if(!world.isRemote) {
			if(highGrade) {
				PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start("").nextTranslation(this.getUnlocalizedName() + ".highGrade").color(EnumChatFormatting.AQUA).flush(), MainRegistry.proxy.ID_DETONATOR), (EntityPlayerMP) player);
			} else if(nearHighGrade) {
				PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start("").nextTranslation(this.getUnlocalizedName() + ".nearHighGrade").color(EnumChatFormatting.LIGHT_PURPLE).flush(), MainRegistry.proxy.ID_DETONATOR), (EntityPlayerMP) player);
			} else if(direct) {
				PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start("").nextTranslation(this.getUnlocalizedName() + ".bullseye").color(EnumChatFormatting.DARK_GREEN).flush(), MainRegistry.proxy.ID_DETONATOR), (EntityPlayerMP) player);
			} else if(oil) {
				PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start("").nextTranslation(this.getUnlocalizedName() + ".detected").color(EnumChatFormatting.GOLD).flush(), MainRegistry.proxy.ID_DETONATOR), (EntityPlayerMP) player);
			} else {
				PacketDispatcher.wrapper.sendTo(new PlayerInformPacket(ChatBuilder.start("").nextTranslation(this.getUnlocalizedName() + ".noOil").color(EnumChatFormatting.RED).flush(), MainRegistry.proxy.ID_DETONATOR), (EntityPlayerMP) player);
			}
		}

		world.playSoundAtEntity(player, "hbm:item.techBleep", 1.0F, 1.0F);
		
		player.swingItem();
		
		return stack;
		
	}
}
