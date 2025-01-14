package teluri.mods.jlrays;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import teluri.mods.jlrays.ConeTracer26Nbs.IAlphaProvider;
import teluri.mods.jlrays.ConeTracer26Nbs.ISightConsumer;
import teluri.mods.jlrays.ConeTracer26Nbs.ISightUpdateConsumer3;
import teluri.mods.jlrays.boilerplate.ShinyBlockPos;

/**
 * handle light updates logic
 * 
 * @author RBLG
 * @since v0.0.1
 */
public class ConeTracedLightEngine extends LightEngine<JlrLightSectionStorage.JlrDataLayerStorageMap, JlrLightSectionStorage> {
	// max range that can be updated by a light update.
	// a value above 15 will cause issues until the generation pyramid and loaded chunk borders logic is changed to handle more than adjacent chunks
	public static final int RANGE = 20;

	private final MutableBlockPos mutablePos = new MutableBlockPos();
	// queue of block updates. use a FIFO queue but it could probably use a hash like vanilla (would require logic to handle merging two updates on the same pos)
	private final LongArrayFIFOQueue changeQueue = new LongArrayFIFOQueue();

	public ConeTracedLightEngine(LightChunkGetter chunkSource) {
		this(chunkSource, new JlrLightSectionStorage(chunkSource));
	}

	protected ConeTracedLightEngine(LightChunkGetter chunkSource, JlrLightSectionStorage storage) {
		super(chunkSource, storage);
	}

	/**
	 * fired when a block update happen. will read the previous and current blockstate hidden in the shiny blockpos and queue the change for a light update
	 */
	@Override
	public void checkBlock(BlockPos pos) {
		long packed = 0;
		if (pos instanceof ShinyBlockPos) {
			ShinyBlockPos rpos = (ShinyBlockPos) pos;
			BlockState prev = rpos.previous;
			BlockState curr = rpos.current;
			long oldemit = prev.getLightEmission();
			long newemit = curr.getLightEmission();
			packed = newemit + (oldemit << 32);

			this.blockNodesToCheck.add(pos.asLong());
			this.changeQueue.enqueue(pos.asLong());
			this.changeQueue.enqueue(packed);
		} else {
			JustLikeRays.LOGGER.error("checkBlock in destination to the light engine was not provided a ShinyBlockPos");
		}
	}

	@Override
	public boolean hasLightWork() {
		return !this.changeQueue.isEmpty() || super.hasLightWork();
	}

	/**
	 * applies all updates in the queue
	 */
	@Override
	public int runLightUpdates() {
		if (changeQueue.size() > 8) { // TODO should be 2?
			JustLikeRays.LOGGER.debug(String.format("%d sources done at once. there might be mistakes", changeQueue.size() / 2));
		}
		while (2 <= changeQueue.size()) {
			long packedpos = changeQueue.dequeueLong();
			long packedemit = changeQueue.dequeueLong();
			this.updateBlock(packedpos, packedemit);
		}
		this.changeQueue.clear();
		this.blockNodesToCheck.clear();
		this.blockNodesToCheck.trim(512);
		this.storage.markNewInconsistencies(this);
		this.storage.swapSectionMap();
		return 0; // return value is unused anyway
	}

	/**
	 * trigger a source update and/or an opacity update if required on a block change
	 * 
	 * @param packedPos
	 * @param packedEmit
	 */
	protected void updateBlock(long packedPos, long packedEmit) { // if it reach there it mean the change was non trivial anyway
		long secpos = SectionPos.blockToSection(packedPos);
		mutablePos.set(packedPos);
		Vector3i vpos = new Vector3i(mutablePos.getX(), mutablePos.getY(), mutablePos.getZ());

		int oldemit = (int) (packedEmit >>> 32);
		int newemit = (int) (packedEmit & Integer.MAX_VALUE);

		if (storage.storingLightForSection(secpos)) {
			BlockState blockState = getState(mutablePos);
			int newopacity = getAlpha(blockState, mutablePos);
			int oldopacity = storage.getFullStoredLevel(packedPos) == 0 ? 0 : 1; // !=0 mean air
			if (newopacity != oldopacity) {
				UpdateLightForOpacityChange(vpos, oldopacity, newopacity);
			}
			if (oldemit != 0 || newemit != 0) {
				if (newopacity != 0) {
					updateLight(vpos, oldopacity, 1, 1, oldemit, newemit);
				}
				UpdateLightForSourceChanges(vpos, oldemit, newemit);
			}
			if (newopacity == 0) {
				storage.setStoredLevel(mutablePos.asLong(), 0);
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
		NaiveFbGbvSightEngine.ISightConsumer consu = (xyz, visi, alpha, dist) -> {
			visi *= alpha;
			updateLight(xyz, visi, visi, dist, oldemit, newemit);
		};
		NaiveFbGbvSightEngine.traceAllQuadrants(source, RANGE, this::getAlpha, consu);
	}

	private MutableBlockPos gettermutpos2 = new MutableBlockPos();

	/**
	 * handle change of opacity on block update
	 * 
	 * @param origin position of the block update
	 * @param oldopa
	 * @param newopa
	 */
	public void UpdateLightForOpacityChange(Vector3i origin, int oldopa, int newopa) {
		NaiveFbGbvSightEngine.ISightConsumer consu1 = (source, souVisi, alpha, dist) -> {
			this.gettermutpos2.set(source.x, source.y, source.z);
			BlockState blockState = this.getState(gettermutpos2);
			int sourceEmit = blockState.getLightEmission();
			if (sourceEmit != 0 && dist <= RANGE) { // if is a source and is in range

				NaiveFbGbvSightEngine.ISightUpdateConsumer consu2 = (xyz2, ovisi, nvisi, dist2) -> {
					updateLight(xyz2, ovisi, nvisi, dist2, sourceEmit, sourceEmit);
				};
				Vector3i offset = new Vector3i(origin).sub(source);
				NaiveFbGbvSightEngine.IAlphaProvider oaprov = (xyz3) -> getOldOpacity(xyz3, origin, oldopa);
				NaiveFbGbvSightEngine.traceAllChangedQuadrants(source, offset, RANGE, oaprov, this::getAlpha, consu2);
			}
		};
		NaiveFbGbvSightEngine.traceAllQuadrants(origin, RANGE, this::getAlpha, consu1);
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

	private MutableBlockPos gettermutpos = new MutableBlockPos();// HACK this forbid paralelization of the cones

	/**
	 * get the alpha value of a block (aka transparency, aka the oposite of opacity) 0= opaque, 1= transparent (wait did i got the concept wrong?)
	 */
	private float getAlpha(Vector3i xyz) { // this.shapeOccludes(packedPos, blockState, l, blockState2, direction)
		this.gettermutpos.set(xyz.x, xyz.y, xyz.z);
		BlockState blockState = this.getState(gettermutpos);
		return getAlpha(blockState, gettermutpos);
	}

	protected int getAlpha(BlockState state, BlockPos blockPos) {
		// lightBlock is weird, 0..1 is transparent, 15 is opaque
		return state.getLightBlock() <= 1 ? 1 : 0;
	}

	/**
	 * get tha alpha value at a coordinate <br>
	 * but if at a coordinate where the change given happenned, look at the stored light level to deduce what the previous opacity was
	 * 
	 * @param xyz        the current position that is evaluated
	 * @param changed    the position of the change
	 * @param changedopa the previous alpha value
	 */
	private float getOldOpacity(Vector3i xyz, Vector3i changed, float changedopa) {
		if (xyz.equals(changed.x, changed.y, changed.z)) {
			return changedopa;
		}
		return getAlpha(xyz);
	}

	private void updateLight(Vector3i xyz, float ovisi, float nvisi, double dist, float oldemit, float newemit) {
		updateLight(BlockPos.asLong(xyz.x, xyz.y, xyz.z), ovisi, nvisi, dist, oldemit, newemit);
	}

	/**
	 * update the light level value of a block based on given visibility and emition changes
	 * 
	 * @param longpos position packed as a long
	 * @param ovisi   old visibility value
	 * @param nvisi   new visibility value
	 * @param dist    distance to the source
	 * @param oldemit old emition value
	 * @param newemit new emition value
	 */
	private void updateLight(long longpos, float ovisi, float nvisi, double dist, float oldemit, float newemit) {
		if (!this.storage.storingLightForSection(SectionPos.blockToSection(longpos))) {
			return;
		}
		ovisi /= dist;
		nvisi /= dist;
		int oival = ovisi == 0 ? 0 : Math.clamp((int) (ovisi * oldemit - 0.5), 0, 15);
		int nival = nvisi == 0 ? 0 : Math.clamp((int) (nvisi * newemit - 0.5), 0, 15);

		this.storage.addStoredLevel(longpos, -oival + nival);
	}

	/**
	 * receive light level data on loading chunks
	 */
	@Override
	public void queueSectionData(long sectionPos, @Nullable DataLayer data) {
		if (data != null && !(data instanceof ByteDataLayer)) {
			this.chunkSource.getLevel();
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
