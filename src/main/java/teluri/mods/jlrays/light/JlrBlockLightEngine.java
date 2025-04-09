package teluri.mods.jlrays.light;

import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.light.misc.IBlockStateProvider;
import teluri.mods.jlrays.light.misc.ILightStorage;
import teluri.mods.jlrays.light.misc.SectionUpdate;
import teluri.mods.jlrays.light.misc.TaskCache;
import teluri.mods.jlrays.light.misc.TaskCache.ITaskCacheFactory;
import teluri.mods.jlrays.light.sight.FbGbvSightEngine;
import teluri.mods.jlrays.light.sight.misc.AlphaHolder;
import teluri.mods.jlrays.light.sight.misc.ISightUpdateConsumer;
import teluri.mods.jlrays.light.sight.misc.Quadrant;
import teluri.mods.jlrays.light.sight.misc.AlphaHolder.IAlphaProvider;
import teluri.mods.jlrays.misc.ShinyBlockPos;
import static teluri.mods.jlrays.util.MathHelper.*;

/**
 * handle light updates logic
 * 
 * @author RBLG
 * @since v0.0.1
 */
public class JlrBlockLightEngine {
	public static final float DISTANCE_RATIO = 0.1f;
	public static final float MINIMUM_VALUE = 0.5f;
	public static final float RANGE_EDGE_NUMBER = 1 / (MINIMUM_VALUE * DISTANCE_RATIO); // number used to get the edge from the source intensity
	public static final int MAX_RANGE = getRange(15); // range of the highest value emissive source possible. define how far to search for sources

	// map of all the changes to process with the previous blockstate associated
	protected final Long2ObjectOpenHashMap<BlockState> changeMap = new Long2ObjectOpenHashMap<>();
	protected final Long2ObjectOpenHashMap<BlockState> sourceChangeMap = new Long2ObjectOpenHashMap<>();
	protected final Long2ObjectOpenHashMap<SectionUpdate> sectionChangeMap = new Long2ObjectOpenHashMap<>();

	protected final ITaskCacheFactory taskCacheFactory;
	protected final IBlockStateProvider blockStateProvider;
	protected final ILightStorage lightStorage;

	public JlrBlockLightEngine(IBlockStateProvider nBSProvider, ITaskCacheFactory ntaskCacheFactory, ILightStorage nLightStorage) {

		taskCacheFactory = ntaskCacheFactory;
		blockStateProvider = nBSProvider;
		lightStorage = nLightStorage;
	}

	/**
	 * fired when a block update happen. will read the previous and current blockstate hidden in the shiny blockpos and queue the change for a light update
	 */
	public void checkBlock(BlockPos pos) {
		if (!(pos instanceof ShinyBlockPos)) {
			JustLikeRays.LOGGER.error("checkBlock in destination to the light engine was not provided a ShinyBlockPos");
			return;
		}
		ShinyBlockPos rpos = (ShinyBlockPos) pos;
		BlockState prev = rpos.previous;
		BlockState curr = rpos.current;

		long secpos = SectionPos.blockToSection(pos.asLong());
		if (!lightStorage.storingLightForSection(secpos)) {
			return;
		}
		if (prev.getLightEmission() != curr.getLightEmission()) {
			sourceChangeMap.putIfAbsent(pos.asLong(), prev);
		}
		if (getAlpha(prev) != getAlpha(curr) || prev.useShapeForLightOcclusion() || curr.useShapeForLightOcclusion()) {
			changeMap.putIfAbsent(pos.asLong(), prev);

			SectionUpdate secupd = sectionChangeMap.get(secpos);
			if (secupd == null) {
				secupd = new SectionUpdate(pos.getX(), pos.getY(), pos.getZ());
				sectionChangeMap.put(secpos, secupd);
			} else {
				secupd.merge(pos);
			}

		}

	}

	public boolean hasLightWork() {
		return !changeMap.isEmpty();
	}

	/**
	 * applies all updates in the queue
	 */
	public void runLightUpdates() {
		// MutableBlockPos mbptmp = new MutableBlockPos();
		Vector3i vtmp = new Vector3i();

		// scout the area around block updates to find light sources that need to be updated
		sectionChangeMap.long2ObjectEntrySet().parallelStream().forEach((entry) -> {
			SectionUpdate secupd = entry.getValue();
			if (secupd.isSingleBlock()) {
				vtmp.set(secupd.x1, secupd.y1, secupd.z1);
				evaluateImpactedSources(vtmp);
			} else { // if there's more than one update in the per section group, use an alternative to single block sight
				groupApproximateImpactedSources(secupd);
			}
		});
		// updates light sources that need to be updated
		sourceChangeMap.forEach((longpos, prev) -> {
			BlockState curr = this.blockStateProvider.get(longpos);
			vtmp.set(BlockPos.getX(longpos), BlockPos.getY(longpos), BlockPos.getZ(longpos));
			updateImpactedSource(vtmp, prev, curr);

		});
		// as a security, set all newly opaque blocks' light to 0
		changeMap.forEach((longpos, prev) -> {
			BlockState curr = this.blockStateProvider.get(longpos);
			if (getAlpha(curr) == 0 && curr.getLightEmission() == 0) {
				lightStorage.setLevel(longpos, 0);
			}
		});
		changeMap.clear();
		changeMap.trim(512);

		sourceChangeMap.clear();
		sourceChangeMap.trim(512);

		sectionChangeMap.clear();
		sectionChangeMap.trim(512);

		this.lightStorage.onLightUpdateCompleted();
	}

	/**
	 * searches for light sources visible from the updated block
	 */
	protected void evaluateImpactedSources(Vector3i pos) {

		long[] inrangepos = new long[changeMap.size()];
		BlockState[] inrangebs = new BlockState[changeMap.size()];
		int size = filterBlockUpdatesByRange(pos, inrangepos, inrangebs, MAX_RANGE);

		TaskCache preCache = taskCacheFactory.createWithRange(pos, MAX_RANGE);
		FbGbvSightEngine.forEachQuadrants((quadrant) -> {
			TaskCache taskCache = preCache.shallowCopy(); // differents quadrant can get away with sharing most of the cache, just not the mutpos

			ISightUpdateConsumer scons = (source, unused1, unused2) -> {
				BlockState blockState = taskCache.get(source);
				int sourceemit = blockState.getLightEmission();
				if (sourceemit != 0) {
					// check is done as squared to avoid square roots
					float sourcerange = getRangeSquared(sourceemit);
					long dist = source.distanceSquared(pos);
					if (dist < sourcerange) {
						syncAddSourceChange(BlockPos.asLong(source.x, source.y, source.z), blockState);
					}
				}
				// no need to check for lightsources in previous blockStates, as they would already be in sourceChangemap
			};

			IAlphaProvider naprov = taskCache;
			if (size == 0) {
				FbGbvSightEngine.traceQuadrant(pos, MAX_RANGE, quadrant, naprov, scons, true); // true== scout
			} else {
				IAlphaProvider oaprov = getFastestPreviousAlphaProvider(inrangebs, inrangepos, size, taskCache);
				FbGbvSightEngine.traceChangedQuadrant(pos, MAX_RANGE, quadrant, oaprov, naprov, scons, true);
			}
		});
	}

	/**
	 * enforce synchronized access of sourceChangeMap put for async use
	 */
	private synchronized void syncAddSourceChange(long longpos, BlockState blockState) {
		sourceChangeMap.putIfAbsent(longpos, blockState);
	}

	/**
	 * iterate over the zone that can be impacted by an update in the SectionUpdate bounds
	 */
	protected void groupApproximateImpactedSources(SectionUpdate secupd) {
		// TODO need a check to avoid iterating over same areas
		// for Z lines, bound start and end based on if there's overlaping areas with other bounds
		// if a Z line is entirely inside another, skip it, as it will be dealt with by another bound
		int sx1 = secupd.x1 - MAX_RANGE, sy1 = secupd.y1 - MAX_RANGE, sz1 = secupd.z1 - MAX_RANGE;
		int sx2 = secupd.x2 + MAX_RANGE, sy2 = secupd.y2 + MAX_RANGE, sz2 = secupd.z2 + MAX_RANGE;

		TaskCache taskCache = taskCacheFactory.create(sx1, sy1, sz1, sx2, sy2, sz2);
		for (int itx = sx1; itx < sx2; itx++) {
			for (int ity = sy1; ity < sy2; ity++) {
				for (int itz = sz1; itz < sz2; itz++) {
					// mutpos0.set(itx, ity, itz);
					BlockState blockState = taskCache.getState(itx, ity, itz);
					int sourceemit = blockState.getLightEmission();
					if (sourceemit != 0) {
						syncAddSourceChange(BlockPos.asLong(itx, ity, itz), blockState);
					}
				}
			}
		}
	}

	/**
	 * updates light for all sources impacted by one or multiple block updates
	 */
	protected void updateImpactedSource(Vector3i source, BlockState oldbs, BlockState newbs) {
		int oldemit = oldbs.getLightEmission();
		int newemit = newbs.getLightEmission();

		if (oldemit != newemit) {
			int change = getLightLevelChange(source, source, 1, 1, oldemit, newemit);
			this.lightStorage.addLevel(BlockPos.asLong(source.x, source.y, source.z), change);
		}
		int range = getRange(Math.max(oldemit, newemit));

		long[] inrangepos = new long[changeMap.size()];
		BlockState[] inrangebs = new BlockState[changeMap.size()];
		int size = filterBlockUpdatesByRange(source, inrangepos, inrangebs, range);

		TaskCache preCache = this.taskCacheFactory.createWithRange(source, range);
		FbGbvSightEngine.forEachQuadrants((quadrant) -> { // TODO remove class creation for performance (?)
			TaskCache taskCache = preCache.shallowCopy();
			ISightUpdateConsumer consu = (xyz, ovisi, nvisi) -> updateLight(source, xyz, ovisi, nvisi, oldemit, newemit, taskCache);
			IAlphaProvider naprov = taskCache;

			if (size != 0 && (oldemit != newemit || isQuadrantChanged(inrangepos, size, source, quadrant))) {
				IAlphaProvider oaprov = getFastestPreviousAlphaProvider(inrangebs, inrangepos, size, taskCache);
				FbGbvSightEngine.traceChangedQuadrant(source, range, quadrant, oaprov, naprov, consu, false);
			} else if (oldemit != newemit) {
				// if no updates are around, its always an emit change, unless it was a bad approximation
				FbGbvSightEngine.traceQuadrant(source, range, quadrant, naprov, consu, false);
			}
		});
		preCache.applyAffectedCache();
	}

	/**
	 * iterate over a list of positions and return if there's at least one in the quadrant's direction
	 */
	public boolean isQuadrantChanged(long[] inrangepos, int size, Vector3i source, Quadrant quadrant) {
		Vector3i vtmp = new Vector3i();
		for (int it = 0; it < size; it++) {
			long target = inrangepos[it];
			int x = BlockPos.getX(target);
			int y = BlockPos.getY(target);
			int z = BlockPos.getZ(target);
			int comp1 = sum(vtmp.set(x, y, z).sub(source).mul(quadrant.axis1));
			int comp2 = sum(vtmp.set(x, y, z).sub(source).mul(quadrant.axis2));
			int comp3 = sum(vtmp.set(x, y, z).sub(source).mul(quadrant.axis3));
			if (0 <= comp1 && 0 <= comp2 && 0 <= comp3) {
				return true;
			}
		}
		return false;
	}

	/**
	 * put in inrangepos and inrangebs the positions and blockStates of block updates that are in range of the source
	 */
	protected int filterBlockUpdatesByRange(Vector3i source, long[] inrangepos, BlockState[] inrangebs, int range) {
		Vector3i vtmp = new Vector3i();
		int iter = 0;
		for (Entry<BlockState> entry : changeMap.long2ObjectEntrySet()) {
			long epos = entry.getLongKey();
			vtmp.set(BlockPos.getX(epos), BlockPos.getY(epos), BlockPos.getZ(epos)).sub(source).absolute();
			int dist = Math.max(vtmp.x, Math.max(vtmp.y, vtmp.z));
			if (dist <= range && dist != 0) {
				inrangepos[iter] = entry.getLongKey();
				inrangebs[iter] = entry.getValue();
				iter++;
			}
		}
		return iter;
	}

	/**
	 * return a provider adapted to the amount of block updates in range
	 */
	protected IAlphaProvider getFastestPreviousAlphaProvider(BlockState[] oldbss, long[] targets, int size, TaskCache taskCache) {
		IBlockStateProvider bsprov = getFastestPreviousBlockStateProvider(oldbss, targets, size, taskCache);
		return (xyz, quadr, hol) -> getAlphas(xyz, bsprov, quadr, hol);
	}

	/**
	 * return the fastest blockstate provider based on the amount of block updates in range
	 */
	protected IBlockStateProvider getFastestPreviousBlockStateProvider(BlockState[] oldbss, long[] targets, int size, TaskCache taskCache) {
		return switch (size) {
		default/*             */ -> (x, y, z) -> getOldStateWhenMany(x, y, z, taskCache);
		case 0 /*             */ -> taskCache;
		case 2, 3, 4, 5, 6, 7, 8 -> (x, y, z) -> getOldStateWhenSome(x, y, z, oldbss, targets, size, taskCache);
		case 1 /*             */ -> {
			long target = targets[0];
			BlockState bs = oldbss[0];
			yield (x, y, z) -> getOldStateWhen1(x, y, z, bs, target, taskCache);
		}
		};
	}

	/**
	 * in a chunk, compute all emition for each sources in the chunk
	 */
	public void propagateLightSources(ChunkPos chunkPos) {
		lightStorage.setLightEnabled(chunkPos, true);
		// TODO find a way to init TaskCache properly with chunkPos
		lightStorage.findBlockLightSources(chunkPos, (blockPos, blockState) -> {
			int i = blockState.getLightEmission();
			int range = getRange(i);

			TaskCache preCache = this.taskCacheFactory.createWithRange(blockPos, range);
			FbGbvSightEngine.forEachQuadrants((quadrant) -> {
				TaskCache taskCache = preCache.shallowCopy();

				Vector3i vpos = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
				ISightUpdateConsumer consu = (xyz, ovisi, nvisi) -> updateLight(vpos, xyz, ovisi, nvisi, 0, i, taskCache);
				IAlphaProvider naprov = taskCache;
				FbGbvSightEngine.traceQuadrant(vpos, range, quadrant, naprov, consu, false);
			});
			preCache.applyAffectedCache();
		});
	}

	/**
	 * get the transparency of a blockstate (0=opaque, 1=transparent)
	 */
	public static int getAlpha(BlockState state) {
		// lightBlock is weird, 0..1 is transparent, 15 is opaque
		return (state.getLightBlock() <= 1) ? 1 : 0;
	}

	/**
	 * handle shape based occlusion
	 */
	public static AlphaHolder getAlphas(Vector3i xyz, IBlockStateProvider bsprov, Quadrant quadr, AlphaHolder hol) {
		BlockState state = bsprov.get(xyz);
		hol.f1 = hol.f2 = hol.f3 = hol.f4 = hol.f5 = hol.f6 = hol.block = getAlpha(state);
		if (hol.block != 0 && isntEmptyShape(state)) {
			Direction d1 = 0 < quadr.axis1.x ? Direction.WEST : Direction.EAST;
			Direction d2 = 0 < quadr.axis2.y ? Direction.DOWN : Direction.UP;
			Direction d3 = 0 < quadr.axis3.z ? Direction.NORTH : Direction.SOUTH;
			Direction d4 = d1.getOpposite();
			Direction d5 = d2.getOpposite();
			Direction d6 = d3.getOpposite();

			// previous
			hol.f1 = getFaceAlpha(state, bsprov, d1, xyz.x - quadr.axis1.x, xyz.y, xyz.z) * hol.block;
			hol.f2 = getFaceAlpha(state, bsprov, d2, xyz.x, xyz.y - quadr.axis2.y, xyz.z) * hol.block;
			hol.f3 = getFaceAlpha(state, bsprov, d3, xyz.x, xyz.y, xyz.z - quadr.axis3.z) * hol.block;

			// next
			hol.f4 = getFaceAlpha(state, bsprov, d4, xyz.x + quadr.axis1.x, xyz.y, xyz.z) * hol.block;
			hol.f5 = getFaceAlpha(state, bsprov, d5, xyz.x, xyz.y + quadr.axis2.y, xyz.z) * hol.block;
			hol.f6 = getFaceAlpha(state, bsprov, d6, xyz.x, xyz.y, xyz.z + quadr.axis3.z) * hol.block;
		}
		return hol;
	}

	/**
	 * get an adjacent blockstate and check if light can pass from one to the other block
	 */
	protected static float getFaceAlpha(BlockState curstate, IBlockStateProvider bsprov, Direction dir, int ox, int oy, int oz) {
		BlockState otherstate = bsprov.getState(ox, oy, oz);
		return shapeOccludes(curstate, otherstate, dir) ? 0 : 1;
	}

	protected static boolean isntEmptyShape(BlockState state) {
		return state.canOcclude() && state.useShapeForLightOcclusion();
	}

	public static boolean shapeOccludes(BlockState state1, BlockState state2, Direction dir) {
		VoxelShape voxelShape = LightEngine.getOcclusionShape(state1, dir);
		VoxelShape voxelShape2 = LightEngine.getOcclusionShape(state2, dir.getOpposite());
		return Shapes.faceShapeOccludes(voxelShape, voxelShape2);
	}

	/**
	 * blockstate provider for when there's a single block updates in range
	 */
	public BlockState getOldStateWhen1(int x, int y, int z, BlockState oldbs, long target, TaskCache taskCache) {
		return target == BlockPos.asLong(x, y, z) ? oldbs : taskCache.getState(x, y, z);
	}

	/**
	 * blockstate provider for when there's a small amount of block updates in range
	 */
	public BlockState getOldStateWhenSome(int x, int y, int z, BlockState[] oldbss, long[] targets, int size, TaskCache taskCache) {
		for (int iter = 0; iter < size; iter++) {
			if (targets[iter] == BlockPos.asLong(x, y, z)) {
				return oldbss[iter];
			}
		}
		return taskCache.getState(x, y, z);
	}

	/**
	 * blockstate provider for when there's many block updates in range
	 */
	public BlockState getOldStateWhenMany(int x, int y, int z, TaskCache taskCache) {
		BlockState state = changeMap.get(BlockPos.asLong(x, y, z));
		return state != null ? state : taskCache.getState(x, y, z);
	}

	/**
	 * update the light level value of a block based on given visibility and emition changes
	 * 
	 * @param source  position of the light source
	 * @param xyz     position of the block to update
	 * @param ovisi   old visibility value
	 * @param nvisi   new visibility value
	 * @param oldemit old emition value
	 * @param newemit new emition value
	 */
	private static void updateLight(Vector3i source, Vector3i xyz, float ovisi, float nvisi, int oldemit, int newemit, TaskCache taskCache) {
		ByteDataLayer data = taskCache.getCachedDataLayer(xyz.x, xyz.y, xyz.z);
		if (data == null) {
			return;
		}
		int change = getLightLevelChange(source, xyz, ovisi, nvisi, oldemit, newemit);
		if (change != 0) {
			data.absoluteAdd(xyz.x, xyz.y, xyz.z, change);
			taskCache.notifyUpdate(xyz.x, xyz.y, xyz.z);
		}
	}

	/**
	 * get the light update change value without actually applying it
	 */
	private static int getLightLevelChange(Vector3i source, Vector3i xyz, float ovisi, float nvisi, int oldemit, int newemit) {
		float distinv = 1 / (1 + source.distanceSquared(xyz) * DISTANCE_RATIO);

		int oldlevel = calculateLightLevel(ovisi, distinv, oldemit);
		int newlevel = calculateLightLevel(nvisi, distinv, newemit);

		return -oldlevel + newlevel;
	}

	private static int calculateLightLevel(float visi, float distinv, int emit) {
		return visi == 0 ? 0 : Math.clamp((int) Math.floor(visi * distinv * emit - MINIMUM_VALUE), 0, emit); // TODO choose round or floor
	}

	/**
	 * get the max range impacted by a source of given emission intensity
	 */
	public static int getRange(int emit) {
		return (int) Math.ceil(Math.sqrt(emit * RANGE_EDGE_NUMBER));
	}

	/**
	 * get the square of the max range impacted by a source of given emission intensity
	 */
	public static float getRangeSquared(int emit) {
		return emit * RANGE_EDGE_NUMBER;
	}
}
