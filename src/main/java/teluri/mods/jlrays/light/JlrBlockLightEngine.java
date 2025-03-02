package teluri.mods.jlrays.light;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
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
	// max range that can be updated by a light update.
	// a value above 15 will cause issues until the generation pyramid and loaded chunk borders logic is changed to handle more than adjacent chunks
	public static final int RANGE = 20;

	// map of all the changes to process with the previous blockstate associated
	private final Long2ObjectOpenHashMap<BlockState> changeMap = new Long2ObjectOpenHashMap<BlockState>();

	private final Long2ObjectOpenHashMap<BlockState> sourceChangeMap = new Long2ObjectOpenHashMap<BlockState>();

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
		if (pos instanceof ShinyBlockPos) {
			ShinyBlockPos rpos = (ShinyBlockPos) pos;
			BlockState prev = rpos.previous;
			BlockState curr = rpos.current;

			long secpos = SectionPos.blockToSection(pos.asLong());
			if (storage.storingLightForSection(secpos)) {
				if (getAlpha(prev) != getAlpha(curr) || prev.useShapeForLightOcclusion() || curr.useShapeForLightOcclusion()) {
					changeMap.putIfAbsent(pos.asLong(), prev);
				}
				if (prev.getLightEmission() != curr.getLightEmission()) {
					sourceChangeMap.putIfAbsent(pos.asLong(), prev);
				}
			}

		} else {
			JustLikeRays.LOGGER.error("checkBlock in destination to the light engine was not provided a ShinyBlockPos");
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

		changeMap.forEach((longpos, prev) -> {
			mutPos3.set(longpos);
			BlockState curr = getState(mutPos3);
			vtmp.set(mutPos3.getX(), mutPos3.getY(), mutPos3.getZ());
			evaluateImpactedSources(vtmp, prev, curr);
			if (getAlpha(curr) == 0) {
				storage.setStoredLevel(longpos, 0);
			}
		});
		sourceChangeMap.forEach((longpos, prev) -> {
			mutPos3.set(longpos);
			BlockState curr = getState(mutPos3);
			vtmp.set(mutPos3.getX(), mutPos3.getY(), mutPos3.getZ());
			updateImpactedSource(vtmp, prev, curr);

		});
		this.changeMap.clear();
		this.changeMap.trim(512);

		this.sourceChangeMap.clear();
		this.sourceChangeMap.trim(512);

		this.storage.markNewInconsistencies(this);
		this.storage.swapSectionMap();
		return 0; // return value is unused anyway
	}

	protected void evaluateImpactedSources(Vector3i pos, BlockState oldbs, BlockState newbs) {

		long[] inrangepos = new long[changeMap.size()];
		BlockState[] inrangebs = new BlockState[changeMap.size()];
		int size = filterBlockUpdatesByRange(pos, inrangepos, inrangebs);

		MutableBlockPos mutpos0 = new MutableBlockPos();
		NaiveFbGbvSightEngine.ISightUpdateConsumer scons = (source, unused1, unused2) -> {
			mutpos0.set(source.x, source.y, source.z);
			BlockState blockState = this.getState(mutpos0);
			if (blockState.getLightEmission() != 0) {
				sourceChangeMap.putIfAbsent(mutpos0.asLong(), blockState);
			}
		};
		MutableBlockPos mutpos4 = new MutableBlockPos();
		IAlphaProvider naprov = (xyz5, quadr, hol) -> getCurrentAlphas(xyz5, quadr, hol, mutpos4);

		if (size == 0) {
			NaiveFbGbvSightEngine.scoutAllQuadrantsUpdateless(pos, RANGE, naprov, scons);
		} else {
			IAlphaProvider oaprov = getFastestPreviousAlphaProvider(inrangebs, inrangepos, size);
			NaiveFbGbvSightEngine.scoutAllQuadrants(pos, RANGE, oaprov, naprov, scons);
		}

	}

	protected void updateImpactedSource(Vector3i source, BlockState oldbs, BlockState newbs) {
		int oldemit = oldbs.getLightEmission();
		int newemit = newbs.getLightEmission();
		int oldopacity = getAlpha(oldbs);
		int newopacity = getAlpha(newbs);
		if (oldopacity != newopacity && newopacity != 0) {
			updateLight(source, source, oldopacity, newopacity, oldemit, newemit);
		}
		long[] inrangepos = new long[changeMap.size()];
		BlockState[] inrangebs = new BlockState[changeMap.size()];
		int size = filterBlockUpdatesByRange(source, inrangepos, inrangebs);

		ISightUpdateConsumer consu = (xyz, ovisi, nvisi) -> updateLight(source, xyz, ovisi, nvisi, oldemit, newemit);
		MutableBlockPos mutpos = new MutableBlockPos();
		IAlphaProvider naprov = (xyz, quadr, hol) -> getCurrentAlphas(xyz, quadr, hol, mutpos);

		if (size == 0) {
			NaiveFbGbvSightEngine.traceAllQuadrants(source, RANGE, naprov, consu); // if no updates are around, its always an emit change!
		} else {
			IAlphaProvider oaprov = getFastestPreviousAlphaProvider(inrangebs, inrangepos, size);
			if (oldemit != newemit) {
				NaiveFbGbvSightEngine.traceAllQuadrants2(source, RANGE, oaprov, naprov, consu);
			} else {
				IBlockUpdateIterator buiter = (step) -> iterateOverUpdateList(step, inrangepos, size);
				NaiveFbGbvSightEngine.traceAllChangedQuadrants2(source, newemit, buiter, oaprov, naprov, consu);
			}
		}
	}

	protected int filterBlockUpdatesByRange(Vector3i source, long[] inrangepos, BlockState[] inrangebs) {
		Vector3i vtmp = new Vector3i();
		int iter = 0;
		for (Entry<BlockState> entry : changeMap.long2ObjectEntrySet()) {
			long epos = entry.getLongKey();
			vtmp.set(BlockPos.getX(epos), BlockPos.getY(epos), BlockPos.getZ(epos)).sub(source).absolute();
			int dist = Math.max(vtmp.x, Math.max(vtmp.y, vtmp.z));
			if (dist <= RANGE && dist != 0) {
				inrangepos[iter] = entry.getLongKey();
				inrangebs[iter] = entry.getValue();
				iter++;
			}
		}
		return iter;
	}

	protected IAlphaProvider getFastestPreviousAlphaProvider(BlockState[] oldbss, long[] targets, int size) {
		MutableBlockPos mutpos3 = new MutableBlockPos();
		long target = targets[0];
		BlockState bs = oldbss[0];
		return switch (size) {
		case 1 /*             */ -> (xyz, quadr, hol) -> getPreviousAlphasWhen1___(xyz, quadr, hol, mutpos3, bs, target);
		case 2, 3, 4, 5, 6, 7, 8 -> (xyz, quadr, hol) -> getPreviousAlphasWhenSome(xyz, quadr, hol, mutpos3, oldbss, targets, size);
		default /*            */ -> (xyz, quadr, hol) -> getPreviousAlphasWhenMany(xyz, quadr, hol, mutpos3);
		};
	}

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
				IAlphaProvider naprov = (xyz, quadr, hol) -> getCurrentAlphas(xyz, quadr, hol, mutpos);
				NaiveFbGbvSightEngine.traceAllQuadrants(vpos, RANGE, naprov, consu); // if no updates are around, its always an emit change!
			});
		}
	}

	protected int getAlpha(BlockState state) {
		// lightBlock is weird, 0..1 is transparent, 15 is opaque
		return state.getLightBlock() <= 1 ? 1 : 0;
	}

	public static interface IBlockStateProvider {
		BlockState get(BlockPos pos);
	}

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

	public AlphaHolder getCurrentAlphas(Vector3i xyz, Quadrant quadr, AlphaHolder hol, MutableBlockPos mutpos) {
		return getAlphases(xyz, this::getState, quadr, hol, mutpos);
	}

	public AlphaHolder getPreviousAlphasWhen1___(Vector3i xyz, Quadrant quadr, AlphaHolder hol, MutableBlockPos mutpos, BlockState oldbs, long target) {
		return getAlphases(xyz, (pos) -> getOldStateWhen1(pos, oldbs, target), quadr, hol, mutpos);
	}

	public AlphaHolder getPreviousAlphasWhenSome(Vector3i xyz, Quadrant quadr, AlphaHolder hol, MutableBlockPos mutpos, BlockState[] oldbss, long[] targets, int size) {
		return getAlphases(xyz, (pos) -> getOldStateWhenSome(pos, oldbss, targets, size), quadr, hol, mutpos);
	}

	public AlphaHolder getPreviousAlphasWhenMany(Vector3i xyz, Quadrant quadr, AlphaHolder hol, MutableBlockPos mutpos) {
		return getAlphases(xyz, this::getOldStateWhenMany, quadr, hol, mutpos);
	}

	public BlockState getOldStateWhen1(BlockPos pos, BlockState oldbs, long target) {
		return target == pos.asLong() ? oldbs : getState(pos);
	}

	public BlockState getOldStateWhenSome(BlockPos pos, BlockState[] oldbss, long[] targets, int size) {
		for (int iter = 0; iter < size; iter++) {
			if (targets[iter] == pos.asLong()) {
				return oldbss[iter];
			}
		}
		return getState(pos);
	}

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
	private void updateLight(Vector3i source, Vector3i xyz, float ovisi, float nvisi, float oldemit, float newemit) {
		long longpos = BlockPos.asLong(xyz.x, xyz.y, xyz.z);
		if (!this.storage.storingLightForSection(SectionPos.blockToSection(longpos))) {
			return;
		}
		float dist = 1 + Vector3f.lengthSquared((xyz.x - source.x) * 0.3f, (xyz.y - source.y) * 0.3f, (xyz.z - source.z) * 0.3f);

		int oival = ovisi == 0 ? 0 : Math.clamp((int) (ovisi / dist * oldemit - 0.5), 0, 15);
		int nival = nvisi == 0 ? 0 : Math.clamp((int) (nvisi / dist * newemit - 0.5), 0, 15);

		this.storage.addStoredLevel(longpos, -oival + nival);
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
