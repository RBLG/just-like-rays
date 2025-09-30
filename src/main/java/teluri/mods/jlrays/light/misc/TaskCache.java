package teluri.mods.jlrays.light.misc;

import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.FluidState;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.light.DynamicDataLayer;
import teluri.mods.jlrays.light.JlrBlockLightEngine;
import teluri.mods.jlrays.light.JlrLightSectionStorage;
import teluri.mods.jlrays.light.sight.misc.AlphaHolder;
import teluri.mods.jlrays.light.sight.misc.AlphaHolder.IAlphaProvider;
import teluri.mods.jlrays.light.sight.misc.Quadrant;
import static java.lang.Math.*;

import java.util.function.BiConsumer;

import org.joml.Vector3i;

/**
 * allow paralelization by reimplementing caches starlight style
 * 
 * @author RBLG
 * @since v0.0.7
 */
public class TaskCache implements IBlockStateProvider, IAlphaProvider {
	protected final int ax, ay, az, bx, by, bz; // bounds TODO make paralelization even cooler by using that to paralelize sources

	protected final int sax, say, saz; // lower corner of the bounds aka 0,0,0 in cache section coordinates
	protected final int lenx, leny, lenz; // length of the caches

	protected final MutableBlockPos mutpos = new MutableBlockPos();
	protected final LightChunkGetter chunkGetter;
	protected final JlrLightSectionStorage lightStorage;

	protected final LightChunk[][] chunkCache; // blockstate sources (never null)
	protected final DynamicDataLayer[][][] lightCache; // light level data (can be nulls)
	protected final boolean[][][] affectedCache; // used to tell mc what section need to be rebuilt

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
		sax = SectionPos.blockToSectionCoord(ax - 1);
		say = SectionPos.blockToSectionCoord(ay - 1);
		saz = SectionPos.blockToSectionCoord(az - 1);
		sbx = SectionPos.blockToSectionCoord(bx + 1);
		sby = SectionPos.blockToSectionCoord(by + 1);
		sbz = SectionPos.blockToSectionCoord(bz + 1);

		lenx = sbx - sax + 1;
		leny = sby - say + 1;
		lenz = sbz - saz + 1;

		chunkCache = new LightChunk[lenx][lenz];
		lightCache = new DynamicDataLayer[lenx][leny][lenz];
		affectedCache = new boolean[lenx + 2][leny + 2][lenz + 2];
		for (int itx = sax; itx <= sbx; itx++) {
			for (int itz = saz; itz <= sbz; itz++) {
				LightChunk chunk = chunkGetter.getChunkForLighting(itx, itz);
				chunkCache[itx - sax][itz - saz] = chunk != null ? chunk : BEDROCK_GIVER;
				for (int ity = say; ity <= sby; ity++) {
					lightCache[itx - sax][ity - say][itz - saz] = lightStorage.getDataLayerForCaching(itx, ity, itz);
				}
			}
		}
	}

	private TaskCache(TaskCache prev) {
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
		affectedCache = prev.affectedCache;
	}

	/**
	 * copy this while sharing the caches (not the mutable block pos) (they are thread safe if handled correctly)
	 */
	public TaskCache shallowCopy() {
		return new TaskCache(this);
	}

	public BlockState getState(int x, int y, int z) {
		return getCachedChunk(x, z).getBlockState(mutpos.set(x, y, z));
	}

	public LightChunk getCachedChunk(int x, int z) {
		int sx = SectionPos.blockToSectionCoord(x);
		int sz = SectionPos.blockToSectionCoord(z);
		return this.getCachedChunkFromSectionPos(sx, sz);
	}

	public LightChunk getCachedChunkFromSectionPos(int sx, int sz) {
		sx -= sax;
		sz -= saz;
		if (sx < 0 || lenx <= sx || sz < 0 || lenz <= sz) {
			JustLikeRays.LOGGER.info("chunk cache oob at s:" + sx + "," + sz + " sa:" + sax + "," + saz + " len:" + lenx + "," + lenz);
			LightChunk chunk = this.chunkGetter.getChunkForLighting(sx + sax, sz + saz);
			return chunk == null ? BEDROCK_GIVER : chunk;
		}
		return chunkCache[sx][sz];
	}

	public DynamicDataLayer getCachedDataLayer(int x, int y, int z) {
		int sx = SectionPos.blockToSectionCoord(x);
		int sy = SectionPos.blockToSectionCoord(y);
		int sz = SectionPos.blockToSectionCoord(z);
		return getCachedDataLayerFromSectionPos(sx, sy, sz);
	}

	public DynamicDataLayer getCachedDataLayerFromSectionPos(int sx, int sy, int sz) {
		return lightCache[sx - sax][sy - say][sz - saz];
	}

	/**
	 * store what sections are affected by the updates in the affectedCache
	 */
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

	/**
	 * apply the affected cache to mc light engine's affected section set
	 */
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

	/**
	 * current alpha provider directly in there to avoid allocating by using lambdas
	 */
	@Override
	public AlphaHolder getAlphas(Vector3i xyz, Vector3i source, Quadrant quadr, AlphaHolder hol) {
		return JlrBlockLightEngine.getAlphas(xyz, source, this, quadr, hol);
	}

	public void findBlockLightSources(ChunkPos chunkPos, BiConsumer<BlockPos, BlockState> consumer) {
		getCachedChunk(chunkPos.x, chunkPos.z).findBlockLightSources(consumer);
	}

	/**
	 * factory for TaskCache
	 */
	@FunctionalInterface
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

	/**
	 * dummy LightChunk to ensure chunkCache has no null entry
	 */
	private static final LightChunk BEDROCK_GIVER = new LightChunk() {
		@Override
		public BlockState getBlockState(BlockPos pos) {
			return Blocks.BEDROCK.defaultBlockState();
		}

		@Override
		public void findBlockLightSources(BiConsumer<BlockPos, BlockState> output) {
			// simply does nothing.
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			throw new UnsupportedOperationException();
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getHeight() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getMinY() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ChunkSkyLightSources getSkyLightSources() {
			throw new UnsupportedOperationException();
		}
	};
}
