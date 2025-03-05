package teluri.mods.jlrays.light;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.boilerplate.ShinyBlockPos;
import teluri.mods.jlrays.light.NaiveFbGbvSightEngine.AlphaHolder;
import teluri.mods.jlrays.light.NaiveFbGbvSightEngine.IAlphaProvider;
import teluri.mods.jlrays.light.NaiveFbGbvSightEngine.IBlockUpdateIterator;
import teluri.mods.jlrays.light.NaiveFbGbvSightEngine.IBlockUpdateStep;
import teluri.mods.jlrays.light.NaiveFbGbvSightEngine.ISightUpdateConsumer;
import teluri.mods.jlrays.light.NaiveFbGbvSightEngine.Quadrant;

/**
 * handle light updates logic
 * 
 * @author RBLG
 * @since v0.0.1
 */
public class JlrBlockLightEngine extends LightEngine<JlrLightSectionStorage.JlrDataLayerStorageMap, JlrLightSectionStorage> {
	public static final float DISTANCE_RATIO = 0.1f;
	public static final float MINIMUM_VALUE = 0.5f;
	public static final float RANGE_EDGE_NUMBER = 1 / (MINIMUM_VALUE * DISTANCE_RATIO); // number used to get the edge from the source intensity
	public static final int MAX_RANGE = getRange(15); // range of the highest value emissive source possible. define how far to search for sources

	// map of all the changes to process with the previous blockstate associated
	private final Long2ObjectOpenHashMap<BlockState> changeMap = new Long2ObjectOpenHashMap<BlockState>();

	private final Long2ObjectOpenHashMap<BlockState> sourceChangeMap = new Long2ObjectOpenHashMap<BlockState>();
	private final Long2ObjectOpenHashMap<SectionUpdate> sectionChangeMap = new Long2ObjectOpenHashMap<SectionUpdate>();

	public static class SectionUpdate {
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

	public JlrBlockLightEngine(LightChunkGetter chunkSource) {
		this(chunkSource, new JlrLightSectionStorage(chunkSource));
	}

	protected JlrBlockLightEngine(LightChunkGetter chunkSource, JlrLightSectionStorage storage) {
		super(chunkSource, storage);
	}

	/**
	 * fired when a block update happen. will read the previous and current blockstate hidden in the shiny blockpos and queue the change for a light update
	 */
	@Override
	public void checkBlock(BlockPos pos) {
		// long packed = 0;
		if (!(pos instanceof ShinyBlockPos)) {
			JustLikeRays.LOGGER.error("checkBlock in destination to the light engine was not provided a ShinyBlockPos");
			return;
		}
		ShinyBlockPos rpos = (ShinyBlockPos) pos;
		BlockState prev = rpos.previous;
		BlockState curr = rpos.current;

		long secpos = SectionPos.blockToSection(pos.asLong());
		if (!storage.storingLightForSection(secpos)) {
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

	@Override
	public boolean hasLightWork() {
		return !changeMap.isEmpty() || super.hasLightWork();
	}

	private final MutableBlockPos mutPos3 = new MutableBlockPos();

	/**
	 * applies all updates in the queue
	 */
	@Override
	public int runLightUpdates() {
		Vector3i vtmp = new Vector3i();

		//scout the area around block updates to find light sources that need to be updated
		sectionChangeMap.forEach((longpos, secupd) -> {
			if (secupd.isSingleBlock()) {
				vtmp.set(secupd.x1, secupd.y1, secupd.z1);
				evaluateImpactedSources(vtmp);
			} else { // if there's more than one update in the per section group, use an alternative to single block sight
				groupApproximateImpactedSources(secupd);
			}
		});
		//updates light sources that need to be updated
		sourceChangeMap.forEach((longpos, prev) -> {
			mutPos3.set(longpos);
			BlockState curr = getState(mutPos3);
			vtmp.set(mutPos3.getX(), mutPos3.getY(), mutPos3.getZ());
			updateImpactedSource(vtmp, prev, curr);

		});
		// as a security, set all newly opaque blocks' light to 0
		changeMap.forEach((longpos, prev) -> { 
			mutPos3.set(longpos);
			BlockState curr = getState(mutPos3);
			if (getAlpha(curr) == 0) {
				storage.setStoredLevel(longpos, 0);
			}
		});
		changeMap.clear();
		changeMap.trim(512);

		sourceChangeMap.clear();
		sourceChangeMap.trim(512);

		sectionChangeMap.clear();
		sectionChangeMap.trim(512);

		storage.markNewInconsistencies(this);
		storage.swapSectionMap();
		return 0; // return value is unused anyway
	}

	/**
	 * searches for light sources visible from the updated block
	 */
	protected void evaluateImpactedSources(Vector3i pos) {

		long[] inrangepos = new long[changeMap.size()];
		BlockState[] inrangebs = new BlockState[changeMap.size()];
		int size = filterBlockUpdatesByRange(pos, inrangepos, inrangebs, MAX_RANGE);

		MutableBlockPos mutpos0 = new MutableBlockPos();
		NaiveFbGbvSightEngine.ISightUpdateConsumer scons = (source, unused1, unused2) -> {
			mutpos0.set(source.x, source.y, source.z);
			BlockState blockState = this.getState(mutpos0);
			int sourceemit = blockState.getLightEmission();
			if (sourceemit != 0) {
				// check is done as squared to avoid square roots
				float sourcerange = getRangeSquared(sourceemit);
				double dist = source.distanceSquared(pos);
				if (dist < sourcerange) {
					sourceChangeMap.putIfAbsent(mutpos0.asLong(), blockState);
				}
			}
			// no need to check for lightsources in previous blockStates, as they would already be in sourceChangemap
		};
		MutableBlockPos mutpos4 = new MutableBlockPos();
		IAlphaProvider naprov = (xyz5, quadr, hol) -> getAlphases(xyz5, this::getState, quadr, hol, mutpos4);

		if (size == 0) {
			NaiveFbGbvSightEngine.scoutAllQuadrantsUpdateless(pos, MAX_RANGE, naprov, scons);
		} else {
			IAlphaProvider oaprov = getFastestPreviousAlphaProvider(inrangebs, inrangepos, size);
			NaiveFbGbvSightEngine.scoutAllQuadrants(pos, MAX_RANGE, oaprov, naprov, scons);
		}
	}

	/**
	 * iterate over the zone that can be impacted by an update in the SectionUpdate bounds
	 */
	protected void groupApproximateImpactedSources(SectionUpdate secupd) {
		// TODO need a check to avoid iterating over same areas
		// for Z lines, bound start and end based on if there's overlaping areas with other bounds
		// if a Z line is entirely inside another, skip it, as it will be dealt with by another bound

		MutableBlockPos mutpos0 = new MutableBlockPos();
		for (int itx = secupd.x1 - MAX_RANGE; itx < secupd.x2 + MAX_RANGE; itx++) {
			for (int ity = secupd.y1 - MAX_RANGE; ity < secupd.y2 + MAX_RANGE; ity++) {
				for (int itz = secupd.z1 - MAX_RANGE; itz < secupd.z2 + MAX_RANGE; itz++) {
					mutpos0.set(itx, ity, itz);
					BlockState blockState = this.getState(mutpos0);
					int sourceemit = blockState.getLightEmission();
					if (sourceemit != 0) {
						sourceChangeMap.putIfAbsent(mutpos0.asLong(), blockState);
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
		int oldopacity = getAlpha(oldbs);
		int newopacity = getAlpha(newbs);
		if ((oldopacity != newopacity || oldemit != newemit) && newopacity != 0) {
			updateLight(source, source, oldopacity, newopacity, oldemit, newemit);
		}
		int range = getRange(Math.max(oldemit, newemit));

		long[] inrangepos = new long[changeMap.size()];
		BlockState[] inrangebs = new BlockState[changeMap.size()];
		int size = filterBlockUpdatesByRange(source, inrangepos, inrangebs, range);

		ISightUpdateConsumer consu = (xyz, ovisi, nvisi) -> updateLight(source, xyz, ovisi, nvisi, oldemit, newemit);
		MutableBlockPos mutpos = new MutableBlockPos();
		IAlphaProvider naprov = (xyz, quadr, hol) -> getAlphases(xyz, this::getState, quadr, hol, mutpos);

		if (size == 0) {
			if (oldemit != newemit) {
				// if no updates are around, its always an emit change, unless it was a bad approximation
				NaiveFbGbvSightEngine.traceAllQuadrants(source, range, naprov, consu);
			}
		} else {
			IAlphaProvider oaprov = getFastestPreviousAlphaProvider(inrangebs, inrangepos, size);
			if (oldemit != newemit) {
				NaiveFbGbvSightEngine.traceAllQuadrants2(source, range, oaprov, naprov, consu);
			} else {
				IBlockUpdateIterator buiter = (step) -> iterateOverUpdateList(step, inrangepos, size);
				NaiveFbGbvSightEngine.traceAllChangedQuadrants2(source, newemit, buiter, oaprov, naprov, consu);
			}
		}
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
	protected IAlphaProvider getFastestPreviousAlphaProvider(BlockState[] oldbss, long[] targets, int size) {
		MutableBlockPos mutpos = new MutableBlockPos();
		IBlockStateProvider bsprov = getFastestPreviousBlockStateProvider(oldbss, targets, size);
		return (xyz, quadr, hol) -> getAlphases(xyz, bsprov, quadr, hol, mutpos);
	}

	/**
	 * return the fastest blockstate provider based on the amount of block updates in range
	 */
	protected IBlockStateProvider getFastestPreviousBlockStateProvider(BlockState[] oldbss, long[] targets, int size) {
		return switch (size) {
		default/*             */ -> this::getOldStateWhenMany;
		case 0 /*             */ -> this::getState;
		case 2, 3, 4, 5, 6, 7, 8 -> (pos) -> getOldStateWhenSome(pos, oldbss, targets, size);
		case 1 /*             */ -> {
			long target = targets[0];
			BlockState bs = oldbss[0];
			yield (pos) -> getOldStateWhen1(pos, bs, target);
		}
		};
	}

	/**
	 * applies step to all positions in targets in size
	 */
	protected static void iterateOverUpdateList(IBlockUpdateStep step, long[] targets, int size) {
		for (int it = 0; it < size; it++) {
			long target = targets[it];
			if (step.consume(BlockPos.getX(target), BlockPos.getY(target), BlockPos.getZ(target))) {
				return;
			}
		}
	}

	/**
	 * in a chunk, compute all emition for each sources in the chunk
	 */
	@Override
	public void propagateLightSources(ChunkPos chunkPos) {
		this.setLightEnabled(chunkPos, true);
		LightChunk lightChunk = this.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
		if (lightChunk != null) {
			lightChunk.findBlockLightSources((blockPos, blockState) -> {
				int i = blockState.getLightEmission();
				Vector3i vpos = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
				ISightUpdateConsumer consu = (xyz, ovisi, nvisi) -> updateLight(vpos, xyz, ovisi, nvisi, 0, i);
				MutableBlockPos mutpos = new MutableBlockPos();
				IAlphaProvider naprov = (xyz, quadr, hol) -> getAlphases(xyz, this::getState, quadr, hol, mutpos);
				NaiveFbGbvSightEngine.traceAllQuadrants(vpos, getRange(i), naprov, consu); // if no updates are around, its always an emit change!
			});
		}
	}

	/**
	 * get the transparency of a blockstate (0=opaque, 1=transparent)
	 */
	protected int getAlpha(BlockState state) {
		// lightBlock is weird, 0..1 is transparent, 15 is opaque
		return state.getLightBlock() <= 1 ? 1 : 0;
	}

	public static interface IBlockStateProvider {
		BlockState get(BlockPos pos);
	}

	/**
	 * handle shape based occlusion
	 */
	private AlphaHolder getAlphases(Vector3i xyz, IBlockStateProvider bsprov, Quadrant quadr, AlphaHolder hol, MutableBlockPos mutpos) {
		mutpos.set(xyz.x, xyz.y, xyz.z);
		BlockState state = bsprov.get(mutpos);
		hol.f1 = hol.f2 = hol.f3 = hol.f4 = hol.f5 = hol.f6 = hol.block = 0;
		hol.block = getAlpha(state);
		if (hol.block == 0 || isEmptyShape(state)) {
			hol.f1 = hol.f2 = hol.f3 = hol.f4 = hol.f5 = hol.f6 = hol.block;
		} else {
			Direction d1 = 0 < quadr.axis1().x ? Direction.WEST : Direction.EAST;
			Direction d2 = 0 < quadr.axis2().y ? Direction.DOWN : Direction.UP;
			Direction d3 = 0 < quadr.axis3().z ? Direction.NORTH : Direction.SOUTH;

			BlockState state1 = bsprov.get(mutpos.set(xyz.x - quadr.axis1().x, xyz.y, xyz.z));
			BlockState state2 = bsprov.get(mutpos.set(xyz.x, xyz.y - quadr.axis2().y, xyz.z));
			BlockState state3 = bsprov.get(mutpos.set(xyz.x, xyz.y, xyz.z - quadr.axis3().z));
			hol.f1 = shapeOccludes(state, state1, d1) ? 0 : 1;
			hol.f2 = shapeOccludes(state, state2, d2) ? 0 : 1;
			hol.f3 = shapeOccludes(state, state3, d3) ? 0 : 1;

			BlockState state4 = bsprov.get(mutpos.set(xyz.x + quadr.axis1().x, xyz.y, xyz.z));
			BlockState state5 = bsprov.get(mutpos.set(xyz.x, xyz.y + quadr.axis2().y, xyz.z));
			BlockState state6 = bsprov.get(mutpos.set(xyz.x, xyz.y, xyz.z + quadr.axis3().z));
			hol.f4 = shapeOccludes(state, state4, d1.getOpposite()) ? 0 : 1;
			hol.f5 = shapeOccludes(state, state5, d2.getOpposite()) ? 0 : 1;
			hol.f6 = shapeOccludes(state, state6, d3.getOpposite()) ? 0 : 1;
		}
		return hol;
	}

	/**
	 * blockstate provider for when there's a single block updates in range
	 */
	public BlockState getOldStateWhen1(BlockPos pos, BlockState oldbs, long target) {
		return target == pos.asLong() ? oldbs : getState(pos);
	}

	/**
	 * blockstate provider for when there's a small amount of block updates in range
	 */
	public BlockState getOldStateWhenSome(BlockPos pos, BlockState[] oldbss, long[] targets, int size) {
		for (int iter = 0; iter < size; iter++) {
			if (targets[iter] == pos.asLong()) {
				return oldbss[iter];
			}
		}
		return getState(pos);
	}

	/**
	 * blockstate provider for when there's many block updates in range
	 */
	public BlockState getOldStateWhenMany(BlockPos pos) {
		BlockState state = changeMap.get(pos.asLong());
		return state != null ? state : getState(pos);
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
	private void updateLight(Vector3i source, Vector3i xyz, float ovisi, float nvisi, int oldemit, int newemit) {
		long longpos = BlockPos.asLong(xyz.x, xyz.y, xyz.z);
		if (!this.storage.storingLightForSection(SectionPos.blockToSection(longpos))) {
			return;
		}
		float dist = 1 + source.distanceSquared(xyz) * DISTANCE_RATIO;

		int oival = ovisi == 0 ? 0 : Math.clamp((int) (ovisi / dist * oldemit - MINIMUM_VALUE), 0, oldemit);
		int nival = nvisi == 0 ? 0 : Math.clamp((int) (nvisi / dist * newemit - MINIMUM_VALUE), 0, newemit);

		this.storage.addStoredLevel(longpos, -oival + nival);
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

	/**
	 * receive light level data on loading chunks
	 */
	@Override
	public void queueSectionData(long sectionPos, @Nullable DataLayer data) {
		if (data != null && !(data instanceof ByteDataLayer)) {
			JustLikeRays.LOGGER.warn("block light data layer isnt byte sized");
		}
		super.queueSectionData(sectionPos, data);
	}

	//////////////////////////////////////////////////////////////////////////

	@Deprecated
	@Override
	protected void checkNode(long packedPos) {}

	@Deprecated
	@Override
	protected void propagateIncrease(long packedPos, long queueEntry, int lightLevel) {}

	@Deprecated
	@Override
	protected void propagateDecrease(long packedPos, long lightLevel) {}

}
