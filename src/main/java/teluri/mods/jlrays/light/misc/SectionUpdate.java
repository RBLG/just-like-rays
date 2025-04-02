package teluri.mods.jlrays.light.misc;

import net.minecraft.core.BlockPos;

/**
 * bounds of a volume that contains one or more block updates located in the same section
 * @author RBLG
 * @since v0.0.7
 */
public class SectionUpdate {
	public int x1, y1, z1, x2, y2, z2;

	public SectionUpdate(int x, int y, int z) {
		x1 = x2 = x;
		y1 = y2 = y;
		z1 = z2 = z;
	}

	public boolean isSingleBlock() {
		return x1 == x2 && y1 == y2 && z1 == z2;
	}

	public void merge(BlockPos pos) {
		x1 = Math.min(x1, pos.getX());
		y1 = Math.min(y1, pos.getY());
		z1 = Math.min(z1, pos.getZ());
		x2 = Math.min(x2, pos.getX());
		y2 = Math.min(y2, pos.getY());
		z2 = Math.min(z2, pos.getZ());
	}
}