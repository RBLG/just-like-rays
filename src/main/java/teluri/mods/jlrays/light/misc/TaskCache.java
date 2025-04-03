package teluri.mods.jlrays.light.misc;

import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import teluri.mods.jlrays.light.sight.misc.Quadrant;
import static java.lang.Math.*;

import org.joml.Vector3i;

/**
 * allow paralelization by reimplementing cache starlight style
 * 
 * @author RBLG
 * @since v0.0.7
 */
public class TaskCache implements IBlockStateProvider {
	final int ax, ay, az, bx, by, bz; // bounds

	protected final int sax, saz;
	protected final int lenx;

	protected final MutableBlockPos mutpos = new MutableBlockPos();
	protected final LightChunkGetter LcGetter;

	protected LightChunk[] cache;

	public TaskCache(int nax, int nay, int naz, int nbx, int nby, int nbz, LightChunkGetter ngetter) {
		// int ax, ay, az, bx, by, bz;
		ax = nax;
		ay = nay;
		az = naz;
		bx = nbx;
		by = nby;
		bz = nbz;
		LcGetter = ngetter;

		int sbx, sbz;
		sax = SectionPos.blockToSectionCoord(ax);
		saz = SectionPos.blockToSectionCoord(az);
		sbx = SectionPos.blockToSectionCoord(bx);
		sbz = SectionPos.blockToSectionCoord(bz);

		int lenz;
		lenx = sbx - sax + 1;
		lenz = sbz - saz + 1;

		int len = lenx * lenz;

		cache = new LightChunk[len];
		for (int itx = sax; itx <= sbx; itx++) {
			for (int itz = saz; itz <= sbz; itz++) {
				cache[(itx - sax) + (itz - saz) * lenx] = LcGetter.getChunkForLighting(itx, itz);
			}
		}
	}

	public BlockState getState(int x, int y, int z) {
		return getLightChunk(x, y, z).getBlockState(mutpos.set(x, y, z));
	}

	public LightChunk getLightChunk(int x, int y, int z) {
		int sx = SectionPos.blockToSectionCoord(ax) - sax;
		int sz = SectionPos.blockToSectionCoord(az) - saz;
		return cache[sx + sz * lenx];
	}

	public static interface ITaskCacheFactory {
		public TaskCache create(int nax, int nay, int naz, int nbx, int nby, int nbz);

		default TaskCache createWithRange(int x, int y, int z, int range) {
			return create(x - range, y - range, z - range, x + range, y + range, z + range);
		}

		default TaskCache createWithQuadrant(int x, int y, int z, int range, Quadrant quadrant) {
			int x2, y2, z2;
			x2 = x + quadrant.axis1.x * range;
			y2 = y + quadrant.axis2.y * range;
			z2 = z + quadrant.axis3.z * range;

			return create(min(x, x2), min(y, y2), min(z, z2), max(x, x2), max(y, y2), max(z, z2));
		}

		default TaskCache createWithQuadrant(Vector3i xyz, int range, Quadrant quadrant) {
			return createWithQuadrant(xyz.x, xyz.y, xyz.z, range, quadrant);
		}
	}
}
