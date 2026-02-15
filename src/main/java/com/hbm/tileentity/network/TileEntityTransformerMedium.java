package com.hbm.tileentity.network;

import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;
import api.hbm.energymk2.Nodespace.PowerNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityTransformerMedium extends TileEntityPylonBase {

	@Override
	public ConnectionType getConnectionType() {
		return ConnectionType.TRIPLE;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public double getMaxWireLength() {
		return 35D;
	}

	@Override
	public Vec3 getConnectionPoint() {
		return Vec3.createVectorHelper(xCoord + 0.5, yCoord + 2.75, zCoord + 0.5);
	}

	@Override
	public Vec3[] getMountPos() {
		double height = 2.75;
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
		if(dir == ForgeDirection.UNKNOWN) dir = ForgeDirection.NORTH;
		ForgeDirection side = dir.getRotation(ForgeDirection.UP);

		return new Vec3[] {
				Vec3.createVectorHelper(0.5 + side.offsetX, height, 0.5 + side.offsetZ),
				Vec3.createVectorHelper(0.5, height, 0.5),
				Vec3.createVectorHelper(0.5 - side.offsetX, height, 0.5 - side.offsetZ)
		};
	}

	@Override
	public PowerNode createNode() {
		TileEntity tile = (TileEntity) this;

		BlockPos corePos = new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord);
		PowerNode node = new PowerNode(corePos);

		node.setConnections(new DirPos(xCoord, yCoord, zCoord, ForgeDirection.UNKNOWN));

		for(int[] pos : this.connected) {
			node.addConnection(new DirPos(pos[0], pos[1], pos[2], ForgeDirection.UNKNOWN));
		}

		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
			if(side != ForgeDirection.UP && side != ForgeDirection.DOWN) {
				node.addConnection(new DirPos(xCoord + side.offsetX, yCoord, zCoord + side.offsetZ, side));
			}
		}

		return node;
	}
}
