package teluri.mods.jlrays;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import com.google.common.annotations.VisibleForTesting;

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
import teluri.mods.jlrays.ConeTracer26Nbs.IChangeAlphaProvider;
import teluri.mods.jlrays.ConeTracer26Nbs.ISightConsumer;
import teluri.mods.jlrays.ConeTracer26Nbs.ISightUpdateConsumer2;
import teluri.mods.jlrays.boilerplate.ShinyBlockPos;

public class ConeTracedLightEngine extends LightEngine<JlrLightSectionStorage.JlrDataLayerStorageMap, JlrLightSectionStorage> {
	public static final int RANGE = 20;

	private final MutableBlockPos mutablePos = new MutableBlockPos();
	private final LongArrayFIFOQueue changeQueue = new LongArrayFIFOQueue();

	public ConeTracedLightEngine(LightChunkGetter chunkSource) {
		this(chunkSource, new JlrLightSectionStorage(chunkSource));
	}

	@VisibleForTesting
	public ConeTracedLightEngine(LightChunkGetter chunkSource, JlrLightSectionStorage storage) {
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
			this.checkNode(packedpos, packedemit);
		}
		this.changeQueue.clear();
		this.blockNodesToCheck.clear();
		this.blockNodesToCheck.trim(512);
		this.storage.markNewInconsistencies(this);
		this.storage.swapSectionMap();
		return 0; // return value is unused anyway
	}

	protected void checkNode(long packedPos, long packedEmit) { // if it reach there it mean the change was non trivial anyway
		long secpos = SectionPos.blockToSection(packedPos);
		this.mutablePos.set(packedPos);

		int oldemit = (int) (packedEmit >>> 32);
		int newemit = (int) (packedEmit & Integer.MAX_VALUE);

		if (this.storage.storingLightForSection(secpos)) {
			BlockState blockState = this.getState(mutablePos);
			int newopacity = getOpacity(blockState, mutablePos) == 15 ? 0 : 1; // 1 == air
			int oldlval = Integer.signum(this.storage.getStoredLevel(packedPos)); // !=0 mean air
			this.updateLight(packedPos, 1, 0, 1, 1, oldemit, newemit);
			int opadiff = newopacity - oldlval;
			if (opadiff != 0) {
				UpdateLightForOpacityChange(mutablePos, oldlval, newopacity);
			}
			if (oldemit != 0 || newemit != 0) {
				UpdateLightForSourceChanges(mutablePos, oldemit, newemit);
			}
		}
	}

	public void UpdateLightForSourceChanges(BlockPos pos, int oldemit, int newemit) {
		Vector3i source = new Vector3i(pos.getX(), pos.getY(), pos.getZ());

		ISightConsumer consu = (xyz, visi, alpha, dist) -> updateLight(xyz.x, xyz.y, xyz.z, visi, 0, alpha, dist, oldemit, newemit);
		ConeTracer26Nbs.traceAllCones(source, RANGE, this::getOpacity, consu);
	}

	private MutableBlockPos gettermutpos2 = new MutableBlockPos();

	public void UpdateLightForOpacityChange(BlockPos pos, int oldopa, int newopa) {
		Vector3i origin = new Vector3i(pos.getX(), pos.getY(), pos.getZ());
		Vector3i tmp = new Vector3i();

		ISightConsumer consu1 = (sou, souVisi, alpha, dist) -> {
			this.gettermutpos2.set(sou.x, sou.y, sou.z);
			BlockState blockState = this.getState(gettermutpos2);
			int sourceEmit = blockState.getLightEmission();
			if (sourceEmit != 0 && dist <= sourceEmit) { // if is a source and is in range
				int range2 = RANGE;

				ISightUpdateConsumer2 consu2 = (xyz2, visi2, changevisi, alpha2, dist2) -> {
					if (changevisi == 0) {
						return;
					}
					updateLight(xyz2.x, xyz2.y, xyz2.z, visi2, changevisi, alpha2, dist2, sourceEmit, sourceEmit);
				};
				// consu2.consume(origin.x, origin.y, origin.z, souVisi, newopa - oldopa, 1, dist);

				Vector3i offset = new Vector3i(origin).sub(sou);
				Vector3i source = tmp.set(sou);
				IChangeAlphaProvider cprov = (xyz3) -> (origin.x == xyz3.x && origin.y == xyz3.y && origin.z == xyz3.z) ? newopa - oldopa : 0;
				ConeTracer26Nbs.traceChangeCone2(source, offset, range2, this::getOpacity, cprov, consu2);
			}
		};
		ConeTracer26Nbs.traceAllCones(origin, RANGE, this::getOpacity, consu1);
	}

	@Override
	public void propagateLightSources(ChunkPos chunkPos) {
		this.setLightEnabled(chunkPos, true);
		LightChunk lightChunk = this.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
		if (lightChunk != null) {
			lightChunk.findBlockLightSources((blockPos, blockState) -> {
				int i = blockState.getLightEmission();
				this.UpdateLightForSourceChanges(blockPos, 0, i);
				this.storage.assertValidity(blockPos.asLong());
			});
		}
	}

	private MutableBlockPos gettermutpos = new MutableBlockPos();// HACK this forbid paralelization of the cones

	private float getOpacity(Vector3i xyz) { // this.shapeOccludes(packedPos, blockState, l, blockState2, direction)
		this.gettermutpos.set(xyz.x, xyz.y, xyz.z);
		BlockState blockState = this.getState(gettermutpos);
		return getOpacity(blockState, gettermutpos) == 15 ? 0 : 1;
	}

	private void updateLight(int x, int y, int z, float visi, float changevisi, float alpha, double dist, float oldemit, float newemit) {
		updateLight(BlockPos.asLong(x, y, z), visi, changevisi, alpha, dist, oldemit, newemit);
	}

	private void updateLight(long longpos, float visi, float changevisi, float alpha, double dist, float oldemit, float newemit) {
		if (alpha == 0 && changevisi == 0) {
			return;
		}
		visi *= alpha;
		// visi *= alpha; //if alpha ever become different than 0 or 1
		if (!this.storage.storingLightForSection(SectionPos.blockToSection(longpos))) {
			return;
		}
		visi /= dist;
		float oldvisi = visi - changevisi;
		int oival = Math.clamp((int) (oldvisi * oldemit - 0.5), 0, 15);
		int nival = Math.clamp((int) (visi * newemit - 0.5), 0, 15);

		// this.storage.isValid(longpos);
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
