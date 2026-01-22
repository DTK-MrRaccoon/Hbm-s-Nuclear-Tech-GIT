package com.hbm.blocks.generic;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class BlockConcreteDamagable extends Block {
    
    private static final Random RANDOM = new Random();
    
    private static final float CRACK_THRESHOLD = 0.25f;
    private static final float ALWAYS_CRACK_THRESHOLD = 0.50f;
    private static final float DESTROY_THRESHOLD = 1.0f;
    
    private static final float CRACK_CHANCE = 0.65f;
    private static final float GRAVEL_CHANCE = 0.15f;
    
    private float customResistance;
    
    public BlockConcreteDamagable(Material material) {
        super(material);
        this.customResistance = 10.0F; // Default value
        super.setResistance(10.0F);
    }
    
    @Override
    public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ) {
        return this.customResistance;
    }
    
    @Override
    public void onBlockExploded(World world, int x, int y, int z, net.minecraft.world.Explosion explosion) {
        if (world.isRemote) return; // Only run on server
        
        float explosionPower = explosion.explosionSize;
        float blockResistance = this.customResistance;
        
        float resistanceRatio = explosionPower / blockResistance;
        
        Block currentBlock = world.getBlock(x, y, z);
        
        if (resistanceRatio >= DESTROY_THRESHOLD) {
            world.setBlockToAir(x, y, z);
            return;
        }
        
        if (resistanceRatio >= ALWAYS_CRACK_THRESHOLD) {
            handleDamage(world, x, y, z, currentBlock, true);
            return;
        }
        
        if (resistanceRatio >= CRACK_THRESHOLD) {
            if (RANDOM.nextFloat() < CRACK_CHANCE) {
                handleDamage(world, x, y, z, currentBlock, false);
            } else {
                super.onBlockExploded(world, x, y, z, explosion);
            }
            return;
        }
        
        super.onBlockExploded(world, x, y, z, explosion);
    }
    
    private void handleDamage(World world, int x, int y, int z, Block currentBlock, boolean alwaysDamage) {
        if (RANDOM.nextFloat() < GRAVEL_CHANCE) {
            world.setBlock(x, y, z, net.minecraft.init.Blocks.gravel, 0, 3);
            return;
        }
        
        Block nextStage = getNextDamageStage(currentBlock);
        
        if (nextStage != null && nextStage != currentBlock) {
            world.setBlock(x, y, z, nextStage, 0, 3);
        } else if (alwaysDamage) {
            world.setBlockToAir(x, y, z);
        } else {
            super.onBlockExploded(world, x, y, z, new net.minecraft.world.Explosion(world, null, x, y, z, 0));
        }
    }
    
    private Block getNextDamageStage(Block currentBlock) {
        String blockName = currentBlock.getUnlocalizedName();
        
        if (currentBlock == com.hbm.blocks.ModBlocks.concrete_smooth) {
            return com.hbm.blocks.ModBlocks.concrete_smooth_cracked;
        }
        
        if (currentBlock == com.hbm.blocks.ModBlocks.brick_concrete) {
            return com.hbm.blocks.ModBlocks.brick_concrete_cracked;
        }
        
        if (currentBlock == com.hbm.blocks.ModBlocks.brick_concrete_mossy) {
            return com.hbm.blocks.ModBlocks.brick_concrete_cracked;
        }
        
        if (currentBlock == com.hbm.blocks.ModBlocks.brick_concrete_cracked) {
            return com.hbm.blocks.ModBlocks.brick_concrete_broken;
        }
        
        return null;
    }
    
    @Override
    public boolean canDropFromExplosion(net.minecraft.world.Explosion explosion) {
        return true;
    }
    
    public BlockConcreteDamagable setResistance(float resistance) {
        this.customResistance = resistance;
        super.setResistance(resistance);
        return this;
    }
    
    public float getCustomResistance() {
        return this.customResistance;
    }
}