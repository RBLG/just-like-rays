package teluri.mods.jlrays.light;

import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3i;

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
import teluri.mods.jlrays.light.NaiveFbGbvSightEngine.ISightConsumer;
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

	private BlockGetter level;

	public JlrBlockLightEngine(LightChunkGetter chunkSource, BlockGetter level) {
		this(chunkSource, new JlrLightSectionStorage(chunkSource));
		this.level = level;
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

			this.changeMap.putIfAbsent(pos.asLong(), prev);
		} else {
			JustLikeRays.LOGGER.error("checkBlock in destination to the light engine was not provided a ShinyBlockPos");
		}
	}

	@Override
	public boolean hasLightWork() {
		return !this.changeMap.isEmpty() || super.hasLightWork();
	}

	private final MutableBlockPos mutPos3 = new MutableBlockPos();

	/**
	 * applies all updates in the queue
	 */
	@Override
	public int runLightUpdates() {
		this.changeMap.forEach((longpos, prev) -> {
			mutPos3.set(longpos);
			BlockState curr = getState(mutPos3);
			updateBlock(longpos, prev, curr);
		});
		this.changeMap.clear();
		this.changeMap.trim(512);

		this.storage.markNewInconsistencies(this);
		this.storage.swapSectionMap();
		return 0; // return value is unused anyway
	}

	/**
	 * trigger a source update and/or an opacity update if required on a block change<br>
	 * if it reach there it mean the change was non trivial
	 * 
	 * @param packedPos
	 */
	protected void updateBlock(long packedPos, BlockState oldbs, BlockState newbs) {
		long secpos = SectionPos.blockToSection(packedPos);
		BlockPos blockPos = new BlockPos(BlockPos.getX(packedPos), BlockPos.getY(packedPos), BlockPos.getZ(packedPos));
		Vector3i vpos = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		if (storage.storingLightForSection(secpos)) {
			int oldopacity = getAlpha(blockPos, oldbs, level);
			int newopacity = getAlpha(blockPos, newbs, level);
			int oldemit = oldbs.getLightEmission();
			int newemit = newbs.getLightEmission();

			if (newopacity != oldopacity || oldbs.useShapeForLightOcclusion() || newbs.useShapeForLightOcclusion()) {
				UpdateLightForOpacityChange(vpos, oldbs, newbs);
			}
			if (oldemit != 0 || newemit != 0) {
				if (newopacity != 0) {
					updateLight(vpos, vpos, oldopacity, newopacity, oldemit, newemit);
				}
				UpdateLightForSourceChanges(vpos, oldemit, newemit);
			}
			if (newopacity == 0) {
				storage.setStoredLevel(packedPos, 0);
			}
		}
	}

	/**
	 * handle change of emition intensity on block update
	 * 
	 * @param source  position of the block update
	 * @param oldemit previous emition intensity
	 * @param newemit current emition intensity
	 */
	public void UpdateLightForSourceChanges(Vector3i source, int oldemit, int newemit) {
		ISightConsumer consu = (xyz, visi) -> updateLight(source, xyz, visi, visi, oldemit, newemit);
		MutableBlockPos mutpos1 = new MutableBlockPos();
		IAlphaProvider naprov = (xyz5, quadr, hol) -> getCurrentAlphases(xyz5, quadr, hol, mutpos1);
		NaiveFbGbvSightEngine.traceAllQuadrants(source, RANGE, naprov, consu);
	}

	/**
	 * handle change of opacity on block update
	 * 
	 * @param target position of the block update
	 */
	public void UpdateLightForOpacityChange(Vector3i target, BlockState oldbs, BlockState newbs) {
		MutableBlockPos mutpos0 = new MutableBlockPos();
		NaiveFbGbvSightEngine.ISightUpdateConsumer scons = (source, unused1, unused2) -> {
			mutpos0.set(source.x, source.y, source.z);
			BlockState blockState = this.getState(mutpos0);
			int emit = blockState.getLightEmission();
			if (emit == 0) {
				return;
			}
			ISightUpdateConsumer sucons = (xyz2, ovisi, nvisi) -> updateLight(source, xyz2, ovisi, nvisi, emit, emit);

			MutableBlockPos mutpos1 = new MutableBlockPos();
			MutableBlockPos mutpos2 = new MutableBlockPos();
			IAlphaProvider oaprov = (xyz5, quadr, hol) -> getPreviousAlphases(xyz5, quadr, hol, mutpos1, oldbs, target);
			IAlphaProvider naprov = (xyz5, quadr, hol) -> getCurrentAlphases(xyz5, quadr, hol, mutpos2);
			NaiveFbGbvSightEngine.traceAllChangedQuadrants(source, target, RANGE, oaprov, naprov, sucons);
		};
		MutableBlockPos mutpos3 = new MutableBlockPos();
		MutableBlockPos mutpos4 = new MutableBlockPos();
		IAlphaProvider oaprov2 = (xyz5, quadr, hol) -> getPreviousAlphases(xyz5, quadr, hol, mutpos3, oldbs, target);
		IAlphaProvider naprov2 = (xyz5, quadr, hol) -> getCurrentAlphases(xyz5, quadr, hol, mutpos4);
		NaiveFbGbvSightEngine.scoutAllQuadrants(target, RANGE, oaprov2, naprov2, scons);
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
				this.UpdateLightForSourceChanges(vpos, 0, i);
				// this.storage.assertValidity(blockPos.asLong());
			});
		}
	}

	protected int getAlpha(BlockPos blockPos, BlockState state, BlockGetter level) {
		// lightBlock is weird, 0..1 is transparent, 15 is opaque
		return state.getLightBlock(level, blockPos) <= 1 ? 1 : 0;
	}

	public static interface IBlockStateProvider {
		BlockState get(BlockPos pos);
	}

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

			BlockPos state1Pos = mutpos.set(xyz.x - quadr.axis1().x, xyz.y, xyz.z);
			BlockPos state2Pos = mutpos.set(xyz.x, xyz.y - quadr.axis2().y, xyz.z);
			BlockPos state3Pos = mutpos.set(xyz.x, xyz.y, xyz.z - quadr.axis3().z);
			BlockState state1 = bsprov.get(state1Pos);
			BlockState state2 = bsprov.get(state2Pos);
			BlockState state3 = bsprov.get(state3Pos);
			hol.f1 = shapeOccludes(mutpos.asLong(), state, state1Pos.asLong(), state1, d1) ? 0 : 1;
			hol.f2 = shapeOccludes(mutpos.asLong(), state, state2Pos.asLong(), state2, d2) ? 0 : 1;
			hol.f3 = shapeOccludes(mutpos.asLong(), state, state2Pos.asLong(), state3, d3) ? 0 : 1;

			BlockPos state4Pos = mutpos.set(xyz.x + quadr.axis1().x, xyz.y, xyz.z);
			BlockPos state5Pos = mutpos.set(xyz.x, xyz.y + quadr.axis2().y, xyz.z);
			BlockPos state6Pos = mutpos.set(xyz.x, xyz.y, xyz.z + quadr.axis3().z);
			BlockState state4 = bsprov.get(state4Pos);
			BlockState state5 = bsprov.get(state5Pos);
			BlockState state6 = bsprov.get(state6Pos);
			hol.f4 = shapeOccludes(mutpos.asLong(), state, state4Pos.asLong(), state4, d1.getOpposite()) ? 0 : 1;
			hol.f5 = shapeOccludes(mutpos.asLong(), state, state5Pos.asLong(), state5, d2.getOpposite()) ? 0 : 1;
			hol.f6 = shapeOccludes(mutpos.asLong(), state, state6Pos.asLong(), state6, d3.getOpposite()) ? 0 : 1;
		}
		return hol;
	}

	public AlphaHolder getCurrentAlphases(Vector3i xyz, Quadrant quadr, AlphaHolder hol, MutableBlockPos mutpos) {
		return getAlphases(xyz, (pos) -> getState(pos), quadr, hol, mutpos);
	}

	public AlphaHolder getPreviousAlphases(Vector3i xyz, Quadrant quadr, AlphaHolder hol, MutableBlockPos mutpos, BlockState oldbs, Vector3i target) {
		return getAlphases(xyz, (pos) -> getOldStatePoorly(pos, oldbs, target), quadr, hol, mutpos);
	}

	public BlockState getOldState(BlockPos pos) {
		BlockState state = changeMap.get(pos.asLong());
		return state != null ? state : getState(pos);
	}

	public BlockState getOldStatePoorly(BlockPos pos, BlockState oldbs, Vector3i target) {
		if (target.equals(pos.getX(), pos.getY(), pos.getZ())) {
			return oldbs;
		}
		return getState(pos);
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

		int oival = ovisi == 0 ? 0 : Mth.clamp((int) (ovisi / dist * oldemit - 0.5), 0, 15);
		int nival = nvisi == 0 ? 0 : Mth.clamp((int) (nvisi / dist * newemit - 0.5), 0, 15);

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
