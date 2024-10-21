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
import teluri.mods.jlrays.ConeTracer26Nbs.IOpacityGetter;
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
		return !this.changeQueue.isEmpty(); // TODO
	}

	@Override
	public int runLightUpdates() { // TODO Auto-generated method stub

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

	protected void checkNode(long packedPos, long packedEmit) { // TODO
		long secpos = SectionPos.blockToSection(packedPos);
		this.mutablePos.set(packedPos);

		int oldemit = (int) (packedEmit >>> 32);
		int newemit = (int) packedEmit & Integer.MAX_VALUE;

		if (this.storage.storingLightForSection(secpos)) {
			BlockState blockState = this.getState(mutablePos);
			int newopacity = blockState.useShapeForLightOcclusion() ? 1 : 0;
			int oldlval = this.storage.getStoredLevel(packedPos);
			this.storage.setStoredLevel(packedPos, newemit);
			if (oldemit != 0 || newemit != 0) {
				UpdateLightForSourceChanges(mutablePos, oldemit, newemit);
			}
			int opadiff = Integer.signum(oldlval) - newopacity;
			if (opadiff != 0) {
				UpdateLightForOpacityChange(mutablePos, opadiff);
			}
		}
	}

	private MutableBlockPos gettermutpos = new MutableBlockPos();// HACK this forbid paralelization of the cones

	public void UpdateLightForSourceChanges(BlockPos pos, int oldemit, int newemit) {// TODO
		Vector3i source = new Vector3i(pos.getX(), pos.getY(), pos.getZ());
		int range = 20;

		IOpacityGetter opprov = (x, y, z) -> {
			this.gettermutpos.set(x, y, z);
			BlockState blockState = this.getState(gettermutpos);
			blockState.canOcclude();
			return blockState.canOcclude() ? 0 : 1;
			// 1 - this.getOpacity(blockState, this.gettermutpos);
		};

		ILightConsumer consu = (x, y, z, visi, dist) -> {
			long longpos = BlockPos.asLong(x, y, z);
			if (!this.storage.storingLightForSection(SectionPos.blockToSection(longpos))) {
				return;
			}
			double dval = visi * 4 / dist.length();
			int nival = (int) Math.clamp(dval * newemit - 2, 0, 15);
			int oival = (int) Math.clamp(dval * oldemit - 2, 0, 15);

			int oldlevel = this.storage.getStoredLevel(longpos);

			int newlevel = Math.clamp(oldlevel - oival + nival, 0, 15);
			this.storage.setStoredLevel(longpos, newlevel);

		};

		teluri.mods.jlrays.ConeTracer26Nbs.TraceAllCones(source, range, opprov, consu);
	}

	public void UpdateLightForOpacityChange(BlockPos pos, int opacitydiff) {
		// TODO
	}

	@Override
	public void propagateLightSources(ChunkPos chunkPos) { // TODO Auto-generated method stub
		this.setLightEnabled(chunkPos, true);
		LightChunk lightChunk = this.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
		if (lightChunk != null) {
			lightChunk.findBlockLightSources((blockPos, blockState) -> {
				int i = blockState.getLightEmission();
				this.UpdateLightForSourceChanges(blockPos, i, 0);
			});
		}
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
