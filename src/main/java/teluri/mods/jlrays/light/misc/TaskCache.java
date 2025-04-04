package teluri.mods.jlrays.light.misc;

import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import teluri.mods.jlrays.light.ByteDataLayer;
import teluri.mods.jlrays.light.JlrLightSectionStorage;
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
	protected final int ax, ay, az, bx, by, bz; // bounds

	protected final int sax, say, saz;
	protected final int lenx, leny, lenz;

	protected final MutableBlockPos mutpos = new MutableBlockPos();
	protected final LightChunkGetter chunkGetter;
	protected final JlrLightSectionStorage lightStorage;

	protected final LightChunk[][] chunkCache;
	protected final ByteDataLayer[][][] lightCache;
	protected final boolean[][][] affectedCache;

	public TaskCache(int nax, int nay, int naz, int nbx, int nby, int nbz, LightChunkGetter nchunkgetter, JlrLightSectionStorage nlightstorage) {
		// int ax, ay, az, bx, by, bz;
		ax = nax;
		ay = nay;
		az = naz;
		bx = nbx;
		by = nby;
		bz = nbz;
		chunkGetter = nchunkgetter;
		lightStorage = nlightstorage;

		int sbx, sby, sbz;
		sax = SectionPos.blockToSectionCoord(ax);
		say = SectionPos.blockToSectionCoord(ay);
		saz = SectionPos.blockToSectionCoord(az);
		sbx = SectionPos.blockToSectionCoord(bx);
		sby = SectionPos.blockToSectionCoord(by);
		sbz = SectionPos.blockToSectionCoord(bz);

		lenx = sbx - sax + 1;
		leny = sby - say + 1;
		lenz = sbz - saz + 1;

		chunkCache = new LightChunk[lenx][lenz];
		lightCache = new ByteDataLayer[lenx][leny][lenz];
		affectedCache = new boolean[lenx + 2][leny + 2][lenz + 2];
		for (int itx = sax; itx <= sbx; itx++) {
			for (int itz = saz; itz <= sbz; itz++) {
				chunkCache[itx - sax][itz - saz] = chunkGetter.getChunkForLighting(itx, itz);
				for (int ity = say; ity <= sby; ity++) {
					lightCache[itx - sax][ity - say][itz - saz] = lightStorage.getDataLayerForCaching(itx, ity, itz);
				}
			}
		}
	}

	public TaskCache(TaskCache prev) {
		ax = prev.ax;
		ay = prev.ay;
		az = prev.az;
		bx = prev.bx;
		by = prev.by;
		bz = prev.bz;
		chunkGetter = prev.chunkGetter;
		lightStorage = prev.lightStorage;
		sax = prev.sax;
		say = prev.say;
		saz = prev.saz;
		lenx = prev.lenx;
		leny = prev.leny;
		lenz = prev.lenz;
		chunkCache = prev.chunkCache;
		lightCache = prev.lightCache;
		affectedCache = new boolean[lenx + 2][leny + 2][lenz + 2];
	}

	public BlockState getState(int x, int y, int z) {
		return getCachedChunk(x, z).getBlockState(mutpos.set(x, y, z));
	}

	public LightChunk getCachedChunk(int x, int z) {
		int sx = SectionPos.blockToSectionCoord(x);
		int sz = SectionPos.blockToSectionCoord(z);
		return chunkCache[sx - sax][sz - saz];
	}

	public ByteDataLayer getCachedDataLayer(int x, int y, int z) {
		int sx = SectionPos.blockToSectionCoord(x);
		int sy = SectionPos.blockToSectionCoord(y);
		int sz = SectionPos.blockToSectionCoord(z);
		return lightCache[sx - sax][sy - say][sz - saz];
	}

	public void AddLightLevel(int x, int y, int z, int value) {
		getCachedDataLayer(x, y, z).absoluteAdd(x, y, z, value);
	}

	public void notifyUpdate(int x, int y, int z) {
		int sx1 = SectionPos.blockToSectionCoord(x - 1) - sax + 1;
		int sy1 = SectionPos.blockToSectionCoord(y - 1) - say + 1;
		int sz1 = SectionPos.blockToSectionCoord(z - 1) - saz + 1;
		int sx2 = SectionPos.blockToSectionCoord(x + 1) - sax + 1;
		int sy2 = SectionPos.blockToSectionCoord(y + 1) - say + 1;
		int sz2 = SectionPos.blockToSectionCoord(z + 1) - saz + 1;
		if (sx1 == sx2 && sy1 == sy2 && sz1 == sz2) {
			affectedCache[sx1][sy1][sz1] = true;
		} else {
			for (int itx = sx1; itx <= sx2; itx++) {
				for (int ity = sy1; ity <= sy2; ity++) {
					for (int itz = sz1; itz <= sz2; itz++) {
						affectedCache[itx][ity][itz] = true;
					}
				}
			}
		}
	}

	public void applyAffectedCache() {
		this.lightStorage.syncUsing(() -> { // keep notifying the section updates
			for (int itx = sax; itx <= sax + lenx; itx++) {
				for (int ity = say; ity <= say + leny; ity++) {
					for (int itz = saz; itz <= saz + lenz; itz++) {
						if (affectedCache[itx - sax + 1][ity - say + 1][itz - saz + 1]) { // +1 because affectedCache has a 1 section border around lightCache
							this.lightStorage.notifySingleSectionUpdate(itx, ity, itz);
						}
					}
				}
			}
		});
	}

	public static interface ITaskCacheFactory {
		public TaskCache create(int nax, int nay, int naz, int nbx, int nby, int nbz);

		default TaskCache createWithRange(int x, int y, int z, int range) {
			return create(x - range, y - range, z - range, x + range, y + range, z + range);
		}

		default TaskCache createWithRange(Vector3i xyz, int range) {
			return createWithRange(xyz.x, xyz.y, xyz.z, range);
		}

		default TaskCache createWithRange(BlockPos xyz, int range) {
			return createWithRange(xyz.getX(), xyz.getY(), xyz.getZ(), range);
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
