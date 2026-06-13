package api.hbm.tile;

import net.minecraftforge.common.util.ForgeDirection;

public interface IHeatPipe extends IHeatSource {
	public int getMaxHeat();
	public void setHeat(int heat);
	public boolean canConnectOnSide(ForgeDirection dir);
}
