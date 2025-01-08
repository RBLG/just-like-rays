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

public class ConeTracedLightEngine extends LightEngine<JlrLightSectionStorage.JlrDataLayerStorageMap, JlrLightSectionStorage> {
	public static final int RANGE = 20;

	private final MutableBlockPos mutablePos = new MutableBlockPos();
	private final LongArrayFIFOQueue changeQueue = new LongArrayFIFOQueue();

	public ConeTracedLightEngine(LightChunkGetter chunkSource) {
		this(chunkSource, new JlrLightSectionStorage(chunkSource));
	}

	protected ConeTracedLightEngine(LightChunkGetter chunkSource, JlrLightSectionStorage storage) {
		super(chunkSource, storage);
	}

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

	public void UpdateLightForSourceChanges(Vector3i source, int oldemit, int newemit) {
		ISightConsumer consu = (xyz, visi, alpha, dist) -> {
			visi *= alpha;
			updateLight(xyz, visi, visi, dist, oldemit, newemit);
		};
		ConeTracer26Nbs.traceAllCones(source, RANGE, this::getAlpha, consu);
	}

	private MutableBlockPos gettermutpos2 = new MutableBlockPos();

	public void UpdateLightForOpacityChange(Vector3i origin, int oldopa, int newopa) {
		ISightConsumer consu1 = (source, souVisi, alpha, dist) -> {
			this.gettermutpos2.set(source.x, source.y, source.z);
			BlockState blockState = this.getState(gettermutpos2);
			int sourceEmit = blockState.getLightEmission();
			if (sourceEmit != 0 && dist <= RANGE) { // if is a source and is in range

				ISightUpdateConsumer3 consu2 = (xyz2, ovisi, nvisi, dist2) -> {
					updateLight(xyz2, ovisi, nvisi, dist2, sourceEmit, sourceEmit);
				};
				Vector3i offset = new Vector3i(origin).sub(source);
				IAlphaProvider oaprov = (xyz3) -> getOldOpacity(xyz3, origin, oldopa);
				ConeTracer26Nbs.traceChangeCone2(source, offset, RANGE, oaprov, this::getAlpha, consu2);
			}
		};
		ConeTracer26Nbs.traceAllCones(origin, RANGE, this::getAlpha, consu1);
	}

	@Override
	public void propagateLightSources(ChunkPos chunkPos) {
		this.setLightEnabled(chunkPos, true);
		LightChunk lightChunk = this.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
		if (lightChunk != null) {
			lightChunk.findBlockLightSources((blockPos, blockState) -> {
				int i = blockState.getLightEmission();
				Vector3i vpos = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
				this.UpdateLightForSourceChanges(vpos, 0, i);
				//this.storage.assertValidity(blockPos.asLong());
			});
		}
	}

	private MutableBlockPos gettermutpos = new MutableBlockPos();// HACK this forbid paralelization of the cones

	private float getAlpha(Vector3i xyz) { // this.shapeOccludes(packedPos, blockState, l, blockState2, direction)
		this.gettermutpos.set(xyz.x, xyz.y, xyz.z);
		BlockState blockState = this.getState(gettermutpos);
		return getAlpha(blockState, gettermutpos);
	}

	protected int getAlpha(BlockState blockState, BlockPos blockPos) {
		return getOpacity(blockState) == 1 ? 1 : 0;
	}

	private float getOldOpacity(Vector3i xyz, Vector3i changed, float changedopa) {
		if (xyz.equals(changed.x, changed.y, changed.z)) {
			return changedopa;
		}
		return getAlpha(xyz);
	}

	private void updateLight(Vector3i xyz, float ovisi, float nvisi, double dist, float oldemit, float newemit) {
		updateLight(BlockPos.asLong(xyz.x, xyz.y, xyz.z), ovisi, nvisi, dist, oldemit, newemit);
	}

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

	// public final int getEmission(long packedPos, BlockState state) {
	// int i = state.getLightEmission();
	// return i > 0 && this.storage.lightOnInSection(SectionPos.blockToSection(packedPos)) ? i : 0;
	// }

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
