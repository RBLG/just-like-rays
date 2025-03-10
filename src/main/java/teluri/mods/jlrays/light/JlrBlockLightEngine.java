package teluri.mods.jlrays.light;

import java.util.function.BiConsumer;

import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
public class JlrBlockLightEngine {
	public static final float DISTANCE_RATIO = 0.1f;
	public static final float MINIMUM_VALUE = 0.5f;
	public static final float RANGE_EDGE_NUMBER = 1 / (MINIMUM_VALUE * DISTANCE_RATIO); // number used to get the edge from the source intensity
	public static final int MAX_RANGE = getRange(15); // range of the highest value emissive source possible. define how far to search for sources

	// map of all the changes to process with the previous blockstate associated
	protected final Long2ObjectOpenHashMap<BlockState> changeMap = new Long2ObjectOpenHashMap<BlockState>();
	protected final Long2ObjectOpenHashMap<BlockState> sourceChangeMap = new Long2ObjectOpenHashMap<BlockState>();
	protected final Long2ObjectOpenHashMap<SectionUpdate> sectionChangeMap = new Long2ObjectOpenHashMap<SectionUpdate>();

	protected final ILightSourceFinder lightSourceFinder;
	protected final IBlockStateProvider blockStateProvider;
	protected final ILightStorage lightStorage;

    private BlockGetter level;

	public JlrBlockLightEngine(ILightSourceFinder nsourceFinder, IBlockStateProvider nBSProvider, ILightStorage nLightStorage) {

		lightSourceFinder = nsourceFinder;
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
		if (getAlpha(rpos, prev, level) != getAlpha(rpos, curr, level) || prev.useShapeForLightOcclusion() || curr.useShapeForLightOcclusion()) {
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

	public BlockState getState(BlockPos pos) {
		return this.blockStateProvider.get(pos);
	}

	/**
	 * applies all updates in the queue
	 */
	public void runLightUpdates() {
		MutableBlockPos mbptmp = new MutableBlockPos();
		Vector3i vtmp = new Vector3i();

		// scout the area around block updates to find light sources that need to be updated
		sectionChangeMap.forEach((longpos, secupd) -> {
			if (secupd.isSingleBlock()) {
				vtmp.set(secupd.x1, secupd.y1, secupd.z1);
				evaluateImpactedSources(vtmp);
			} else { // if there's more than one update in the per section group, use an alternative to single block sight
				groupApproximateImpactedSources(secupd);
			}
		});
		// updates light sources that need to be updated
		sourceChangeMap.forEach((longpos, prev) -> {
			mbptmp.set(longpos);
			BlockState curr = getState(mbptmp);
			vtmp.set(mbptmp.getX(), mbptmp.getY(), mbptmp.getZ());
			updateImpactedSource(vtmp, prev, curr);

		});
		// as a security, set all newly opaque blocks' light to 0
		changeMap.forEach((longpos, prev) -> {
			mbptmp.set(longpos);
			BlockState curr = getState(mbptmp);
			if (getAlpha(BlockPos.of(longpos), curr, level) == 0 && curr.getLightEmission() == 0) {
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
		// int oldopacity = getAlpha(oldbs);
		// int newopacity = getAlpha(newbs);
		if (oldemit != newemit) {
			updateLight(source, source, 1, 1, oldemit, newemit);
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
	public void propagateLightSources(ChunkPos chunkPos) {
		lightStorage.setLightEnabled(chunkPos, true);
		lightSourceFinder.findBlockLightSources(chunkPos, (blockPos, blockState) -> {
			int i = blockState.getLightEmission();
			Vector3i vpos = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
			ISightUpdateConsumer consu = (xyz, ovisi, nvisi) -> updateLight(vpos, xyz, ovisi, nvisi, 0, i);
			MutableBlockPos mutpos = new MutableBlockPos();
			IAlphaProvider naprov = (xyz, quadr, hol) -> getAlphases(xyz, this::getState, quadr, hol, mutpos);
			NaiveFbGbvSightEngine.traceAllQuadrants(vpos, getRange(i), naprov, consu); // if no updates are around, its always an emit change!
		});
	}

	/**
	 * get the transparency of a blockstate (0=opaque, 1=transparent)
	 */
	protected int getAlpha(BlockPos blockPos, BlockState state, BlockGetter level) {
		// lightBlock is weird, 0..1 is transparent, 15 is opaque
		return (state.getLightBlock(level, blockPos) <= 1) ? 1 : 0;
	}

	/**
	 * handle shape based occlusion
	 */
	private AlphaHolder getAlphases(Vector3i xyz, IBlockStateProvider bsprov, Quadrant quadr, AlphaHolder hol, MutableBlockPos mutpos) {
		mutpos.set(xyz.x, xyz.y, xyz.z);
		BlockState state = bsprov.get(mutpos);
		hol.f1 = hol.f2 = hol.f3 = hol.f4 = hol.f5 = hol.f6 = hol.block = 0;
		hol.block = getAlpha(mutpos, state, level);
		if (hol.block == 0 || isEmptyShape(state)) {
			hol.f1 = hol.f2 = hol.f3 = hol.f4 = hol.f5 = hol.f6 = hol.block;
		} else {
			Direction d1 = 0 < quadr.axis1().x ? Direction.WEST : Direction.EAST;
			Direction d2 = 0 < quadr.axis2().y ? Direction.DOWN : Direction.UP;
			Direction d3 = 0 < quadr.axis3().z ? Direction.NORTH : Direction.SOUTH;
			Direction d4 = d1.getOpposite();
			Direction d5 = d2.getOpposite();
			Direction d6 = d3.getOpposite();

			BlockPos curpos = new BlockPos(xyz.x, xyz.y, xyz.z);

			// previous
			hol.f1 = getFaceAlpha(state, bsprov, d1, curpos, mutpos.set(xyz.x - quadr.axis1().x, xyz.y, xyz.z));
			hol.f2 = getFaceAlpha(state, bsprov, d2, curpos, mutpos.set(xyz.x, xyz.y - quadr.axis2().y, xyz.z));
			hol.f3 = getFaceAlpha(state, bsprov, d3, curpos, mutpos.set(xyz.x, xyz.y, xyz.z - quadr.axis3().z));

			// next
			hol.f4 = getFaceAlpha(state, bsprov, d4, curpos, mutpos.set(xyz.x + quadr.axis1().x, xyz.y, xyz.z));
			hol.f5 = getFaceAlpha(state, bsprov, d5, curpos, mutpos.set(xyz.x, xyz.y + quadr.axis2().y, xyz.z));
			hol.f6 = getFaceAlpha(state, bsprov, d6, curpos, mutpos.set(xyz.x, xyz.y, xyz.z + quadr.axis3().z));
		}
		return hol;
	}

	/**
	 * get an adjacent blockstate and check if light can pass from one to the other block
	 */
	protected float getFaceAlpha(BlockState curstate, IBlockStateProvider bsprov, Direction dir, BlockPos curpos, BlockPos otherpos) {
		BlockState otherstate = bsprov.get(otherpos);
		return shapeOccludes(curstate, otherstate, curpos, otherpos, dir) ? 0 : 1;
	}

    protected static boolean isEmptyShape(BlockState state) {
        return !state.canOcclude() || !state.useShapeForLightOcclusion();
    }

	public boolean shapeOccludes(BlockState state1, BlockState state2, BlockPos pos1, BlockPos pos2, Direction dir) {
		VoxelShape voxelShape = LightEngine.getOcclusionShape(level, pos1, state1, dir);
		VoxelShape voxelShape2 = LightEngine.getOcclusionShape(level, pos2, state2, dir.getOpposite());
		return Shapes.faceShapeOccludes(voxelShape, voxelShape2);
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
		if (!this.lightStorage.storingLightForSection(SectionPos.blockToSection(longpos))) {
			return;
		}
		float dist = 1 + source.distanceSquared(xyz) * DISTANCE_RATIO;

		int oival = ovisi == 0 ? 0 : Mth.clamp((int) (ovisi / dist * oldemit - MINIMUM_VALUE), 0, oldemit);
		int nival = nvisi == 0 ? 0 : Mth.clamp((int) (nvisi / dist * newemit - MINIMUM_VALUE), 0, newemit);

		this.lightStorage.addLevel(longpos, -oival + nival);
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

	public static interface ILightStorage {
		public void setLevel(long pos, int value);

		public void addLevel(long pos, int value);

		public int getLevel(long pos);

		public boolean storingLightForSection(long secpos);

		public void setLightEnabled(ChunkPos chunkPos, boolean enabled);

		public void onLightUpdateCompleted();
	}

	public static interface ILightSourceFinder {
		public void findBlockLightSources(ChunkPos chunkPos, BiConsumer<BlockPos, BlockState> step);
	}

	public static interface IBlockStateProvider {
		BlockState get(BlockPos pos);
	}
}
