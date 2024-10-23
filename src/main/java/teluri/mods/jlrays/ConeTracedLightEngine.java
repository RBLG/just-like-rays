package teluri.mods.jlrays;

import org.joml.Vector3i;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import teluri.mods.jlrays.ConeTracer26Nbs.ILightConsumer;
import teluri.mods.jlrays.boilerplate.ShinyBlockPos;
import net.minecraft.world.level.lighting.BlockLightEngine;

public class ConeTracedLightEngine extends BlockLightEngine implements LayerLightEventListener {

	private final MutableBlockPos mutablePos = new MutableBlockPos();
	private final LongArrayFIFOQueue changeQueue = new LongArrayFIFOQueue();

	public ConeTracedLightEngine(LightChunkGetter chunkSource) {
		super(chunkSource);
	}

	@Override
	public void checkBlock(BlockPos pos) {
		long packed = 0;
		if (pos instanceof ShinyBlockPos) {
			ShinyBlockPos rpos = (ShinyBlockPos) pos;
			long newemit = rpos.current.getLightEmission();
			long oldemit = rpos.previous.getLightEmission();
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
		return !this.changeQueue.isEmpty(); // TODO this.storage.hasInconsistencies() ||
	}

	@Override
	public int runLightUpdates() {

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

	protected void checkNode(long packedPos, long packedEmit) {
		long secpos = SectionPos.blockToSection(packedPos);
		this.mutablePos.set(packedPos);

		int oldemit = (int) (packedEmit >>> 32);
		int newemit = (int) packedEmit & Integer.MAX_VALUE;

		if (this.storage.storingLightForSection(secpos)) {
			BlockState blockState = this.getState(mutablePos);
			int newopacity = getOpacity(blockState, gettermutpos) == 15 ? 0 : 1; // 1 == air
			int oldlval = this.storage.getStoredLevel(packedPos);
			this.storage.setStoredLevel(packedPos, newemit);
			if (oldemit != 0 || newemit != 0) {
				UpdateLightForSourceChanges(mutablePos, oldemit, newemit);
			}
			int opadiff = newopacity - Integer.signum(oldlval);
			if (opadiff != 0) {
				UpdateLightForOpacityChange(mutablePos, Integer.signum(oldlval), newopacity);
			}
		}
	}

	private MutableBlockPos gettermutpos = new MutableBlockPos();// HACK this forbid paralelization of the cones

	public void UpdateLightForSourceChanges(BlockPos pos, int oldemit, int newemit) {
		Vector3i source = new Vector3i(pos.getX(), pos.getY(), pos.getZ());
		int range = 20;

		ILightConsumer consu = (x, y, z, visi, dist) -> updateLight(x, y, z, visi, dist, oldemit, newemit, 0.8f);
		ConeTracer26Nbs.TraceAllCones(source, range, this::getOpacity, consu);
	}

	private MutableBlockPos gettermutpos2 = new MutableBlockPos();

	public void UpdateLightForOpacityChange(BlockPos pos, int oldopa, int newopa) {
		Vector3i origin = new Vector3i(pos.getX(), pos.getY(), pos.getZ());
		int range = 20;

		ILightConsumer consu1 = (x, y, z, visi, dist) -> {
			this.gettermutpos2.set(x, y, z);
			BlockState blockState = this.getState(gettermutpos2);
			int sourceEmission = blockState.getLightEmission();
			if (sourceEmission != 0) { // TODO add range check

				ILightConsumer consu2 = (x2, y2, z2, visi2, dist2) -> {
					this.updateLight(x2, y2, z2, visi * visi2, dist2, oldopa * sourceEmission, newopa * sourceEmission, 0.8f);
				};
				consu2.consumer(origin.x, origin.y, origin.z, 1, dist);

				Vector3i offset = new Vector3i(origin).sub(x, y, z);
				ConeTracer26Nbs.TraceChangeCone(origin, offset, range, this::getOpacity, consu2);
			}
		};
		ConeTracer26Nbs.TraceAllCones(origin, range, this::getOpacity, consu1);
	}

	@Override
	public void propagateLightSources(ChunkPos chunkPos) {
		this.setLightEnabled(chunkPos, true);
		LightChunk lightChunk = this.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
		if (lightChunk != null) {
			lightChunk.findBlockLightSources((blockPos, blockState) -> {
				int i = blockState.getLightEmission();
				this.UpdateLightForSourceChanges(blockPos, i, 0);
			});
		}
	}

	private float getOpacity(int x, int y, int z) { // this.shapeOccludes(packedPos, blockState, l, blockState2, direction)
		this.gettermutpos.set(x, y, z);
		BlockState blockState = this.getState(gettermutpos);
		return getOpacity(blockState, gettermutpos) == 15 && blockState.getLightEmission() == 0 ? 0 : 1;
	}

	private void updateLight(int x, int y, int z, float visi, double dist, float oldemit, float newemit, float decayrate) {
		long longpos = BlockPos.asLong(x, y, z);
		if (!this.storage.storingLightForSection(SectionPos.blockToSection(longpos))) {
			return;
		}
		// TODO try val/(1+val) *15 ?
		double len = dist * decayrate;
		int nival = (int) Math.clamp(visi * Math.max(newemit - len, 0), 0, 15);
		int oival = (int) Math.clamp(visi * Math.max(oldemit - len, 0), 0, 15);

		int oldlevel = this.storage.getStoredLevel(longpos);

		int newlevel = Math.clamp(oldlevel - oival + nival, 0, 15);
		this.storage.setStoredLevel(longpos, newlevel);
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
