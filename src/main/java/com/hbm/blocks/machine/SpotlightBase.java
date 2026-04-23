package com.hbm.blocks.machine;

import com.hbm.blocks.BlockEnums.LightType;
import com.hbm.blocks.ISpotlight;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.world.gen.nbt.INBTBlockTransformable;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.obj.WavefrontObject;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class SpotlightBase extends Block implements ISpotlight, INBTBlockTransformable {

	public static final int META_YELLOW = 0;
	public static final int META_GREEN = 1;
	public static final int META_BLUE = 2;

	public static boolean disableOnGeneration = true;

	public boolean isOn;
	public int beamLength;
	public LightType type;

	public static int renderID = RenderingRegistry.getNextAvailableRenderId();

	public SpotlightBase(Material mat, int beamLength, LightType type, boolean isOn) {
		super(mat);
		this.beamLength = beamLength;
		this.type = type;
		this.isOn = isOn;
		this.setHardness(0.5F);
		if(isOn) setLightLevel(1.0F);
	}

	@Override
	public int getRenderType() {
		return renderID;
	}

	public WavefrontObject getModel() {
		switch(type) {
		case FLUORESCENT: return (WavefrontObject) ResourceManager.fluorescent_lamp;
		case HALOGEN: return (WavefrontObject) ResourceManager.flood_lamp;
		default: return (WavefrontObject) ResourceManager.cage_lamp;
		}
	}

	public String getPartName(int connectionCount) {
		switch(type) {
		case HALOGEN: return "FloodLamp";
		default: return "CageLamp";
		}
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
		return true;
	}

	@Override
	public MapColor getMapColor(int meta) {
		return MapColor.airColor;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World p_149668_1_, int p_149668_2_, int p_149668_3_, int p_149668_4_) {
		return null;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		ForgeDirection dir = getDirection(world, x, y, z);
		float[] bounds = swizzleBounds(dir);
		float[] offset = new float[] { 0.5F - dir.offsetX * (0.5F - bounds[0]), 0.5F - dir.offsetY * (0.5F - bounds[1]), 0.5F - dir.offsetZ * (0.5F - bounds[2]) };
		setBlockBounds(offset[0] - bounds[0], offset[1] - bounds[1], offset[2] - bounds[2], offset[0] + bounds[0], offset[1] + bounds[1], offset[2] + bounds[2]);
	}

	private float[] swizzleBounds(ForgeDirection dir) {
		float[] bounds = getBounds();
		switch(dir) {
		case EAST:
		case WEST: return new float[] { bounds[2], bounds[1], bounds[0] };
		case UP:
		case DOWN: return new float[] { bounds[1], bounds[2], bounds[0] };
		default: return bounds;
		}
	}

	private float[] getBounds() {
		switch(type) {
		case FLUORESCENT: return new float[] { 0.5F, 0.5F, 0.1F };
		case HALOGEN: return new float[] { 0.35F, 0.25F, 0.2F };
		default: return new float[] { 0.25F, 0.2F, 0.15F };
		}
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hx, float hy, float hz, int initData) {
		return side << 1;
	}

	public ForgeDirection getDirection(IBlockAccess world, int x, int y, int z) {
		int metadata = world.getBlockMetadata(x, y, z);
		return getDirection(metadata);
	}

	public ForgeDirection getDirection(int metadata) {
		return ForgeDirection.getOrientation(metadata >> 1);
	}

	@Override
	public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
		if(!super.canPlaceBlockOnSide(world, x, y, z, side)) return false;
		ForgeDirection dir = ForgeDirection.getOrientation(side);
		return canPlace(world, x, y, z, dir);
	}

	protected boolean canPlace(World world, int x, int y, int z, ForgeDirection dir) {
		x -= dir.offsetX;
		y -= dir.offsetY;
		z -= dir.offsetZ;
		Block block = world.getBlock(x, y, z);
		if(block instanceof BlockSlab) {
			int meta = world.getBlockMetadata(x, y, z);
			boolean isTop = (meta & 8) == 8;
			if(dir == ForgeDirection.UP && !isTop) return true;
			if(dir == ForgeDirection.DOWN && isTop) return true;
			return block.isSideSolid(world, x, y, z, dir);
		}
		return block.isSideSolid(world, x, y, z, dir);
	}

	@Override
	public int getBeamLength() {
		return this.beamLength;
	}

	@Override
	public int transformMeta(int meta, int coordBaseMode) {
		int disabled = disableOnGeneration ? 1 : 0;
		return (INBTBlockTransformable.transformMetaDeco(meta >> 1, coordBaseMode) << 1) + disabled;
	}

	@Override
	public Block transformBlock(Block block) {
		if(!disableOnGeneration) return block;
		return block;
	}

	protected abstract Block getOff();
	protected abstract Block getOn();

	public static void propagateBeam(World world, int x, int y, int z, ForgeDirection dir, int distance) {
		distance--;
		if(distance <= 0) return;
		x += dir.offsetX;
		y += dir.offsetY;
		z += dir.offsetZ;

		Block block = world.getBlock(x, y, z);
		if(!block.isAir(world, x, y, z)) return;

		if(!(block instanceof SpotlightBeam)) {
			world.setBlock(x, y, z, ModBlocks.spotlight_beam, 0, 2);
		}
		if(SpotlightBeam.setDirection(world, x, y, z, dir, true) == 0) return;
		propagateBeam(world, x, y, z, dir, distance);
	}

	public static void propagateBeamWithMeta(World world, int x, int y, int z, ForgeDirection dir, int distance, int meta) {
		distance--;
		if(distance <= 0) return;
		x += dir.offsetX;
		y += dir.offsetY;
		z += dir.offsetZ;

		Block block = world.getBlock(x, y, z);
		if(!block.isAir(world, x, y, z)) return;

		if(!(block instanceof SpotlightBeam)) {
			world.setBlock(x, y, z, ModBlocks.spotlight_beam, meta, 2);
		}
		if(SpotlightBeam.setDirection(world, x, y, z, dir, true) == 0) return;
		propagateBeamWithMeta(world, x, y, z, dir, distance, meta);
	}

	public static void unpropagateBeam(World world, int x, int y, int z, ForgeDirection dir) {
		x += dir.offsetX;
		y += dir.offsetY;
		z += dir.offsetZ;
		Block block = world.getBlock(x, y, z);
		if(!(block instanceof SpotlightBeam)) return;

		if(SpotlightBeam.setDirection(world, x, y, z, dir, false) == 0) {
			world.setBlockToAir(x, y, z);
		}
		unpropagateBeam(world, x, y, z, dir);
	}

	public static void backPropagate(World world, int x, int y, int z, ForgeDirection dir, int meta) {
		x -= dir.offsetX;
		y -= dir.offsetY;
		z -= dir.offsetZ;
		Block block = world.getBlock(x, y, z);
		if(block instanceof ISpotlight) {
			ISpotlight spot = (ISpotlight) block;
			propagateBeam(world, x, y, z, dir, spot.getBeamLength());
		} else if(!(block instanceof SpotlightBeam)) {
			return;
		}
		backPropagate(world, x, y, z, dir, meta);
	}
}