package teluri.mods.jlrays.light;

import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.light.misc.ILightStorage;
import teluri.mods.jlrays.light.misc.TaskCache;

/**
 * allow to abstract away mc light engine logic from the Jlr engine
 * 
 * @author RBLG
 * @since v0.0.7
 */
public class JlrBlockLightEngineAdapter extends LightEngine<JlrLightSectionStorage.JlrDataLayerStorageMap, JlrLightSectionStorage> implements ILightStorage {

	protected final JlrBlockLightEngine engine;

	protected ThreadLocal<MutableBlockPos> threadLocalMutPos = ThreadLocal.withInitial(() -> new MutableBlockPos());

	public JlrBlockLightEngineAdapter(LightChunkGetter chunkSource) {
		this(chunkSource, new JlrLightSectionStorage(chunkSource));
	}

	protected JlrBlockLightEngineAdapter(LightChunkGetter chunkSource, JlrLightSectionStorage storage) {
		super(chunkSource, storage);

		engine = new JlrBlockLightEngine(this::getState, this::createTask, this);
	}

	@Override
	public void checkBlock(BlockPos pos) {
		this.engine.checkBlock(pos);
	}

	@Override
	public void propagateLightSources(ChunkPos chunkPos) {
		this.engine.propagateLightSources(chunkPos);
	}

	@Override
	public boolean hasLightWork() {
		return this.engine.hasLightWork() || super.hasLightWork();
	}

	@Override
	public int runLightUpdates() {
		this.engine.runLightUpdates();
		return 0; // return value is unused anyway
	}

	@Override
	public void queueSectionData(long sectionPos, @Nullable DataLayer data) {
		if (data != null && !(data instanceof DynamicDataLayer)) {
			JustLikeRays.LOGGER.warn("block light DataLayer isnt DynamicDataLayer");
		}
		super.queueSectionData(sectionPos, data);
	}

	@Override
	public void findBlockLightSources(ChunkPos chunkPos, BiConsumer<BlockPos, BlockState> consumer) {
		LightChunk lightChunk = this.chunkSource.getChunkForLighting(chunkPos.x, chunkPos.z);
		if (lightChunk != null) {
			lightChunk.findBlockLightSources(consumer);
		}
	}

	@Deprecated(since = "v0.0.1")
	@Override
	protected void checkNode(long packedPos) {
		// disabling vanilla light engine behavior
	}

	@Deprecated(since = "v0.0.1")
	@Override
	protected void propagateIncrease(long packedPos, long queueEntry, int lightLevel) {
		// disabling vanilla light engine behavior
	}

	@Deprecated(since = "v0.0.1")
	@Override
	protected void propagateDecrease(long packedPos, long lightLevel) {
		// disabling vanilla light engine behavior
	}

	@Override
	public void setLevel(long pos, int value) {
		this.storage.setStoredLevel(pos, value);
	}

	@Override
	public void addLevel(long pos, int value) {
		this.storage.addStoredLevel(pos, value);
	}

	@Override
	public int getLevel(long pos) {
		return this.storage.getStoredLevel(pos);
	}

	@Override
	public boolean storingLightForSection(long secpos) {
		return storage.storingLightForSection(secpos);
	}

	@Override
	public void onLightUpdateCompleted() {
		storage.markNewInconsistencies(this);
		storage.swapSectionMap();
	}

	public DynamicDataLayer getDataLayer(int x, int y, int z) {
		return storage.getDataLayerForCaching(x, y, z);
	}

	public BlockState getState(int x, int y, int z) {
		int sx = SectionPos.blockToSectionCoord(x);
		int sz = SectionPos.blockToSectionCoord(z);
		LightChunk lightChunk = this.getChunk(sx, sz);
		return (lightChunk == null) ? Blocks.BEDROCK.defaultBlockState() : lightChunk.getBlockState(this.threadLocalMutPos.get().set(x, y, z));

	}

	public TaskCache createTask(int nax, int nay, int naz, int nbx, int nby, int nbz) {
		return new TaskCache(nax, nay, naz, nbx, nby, nbz, chunkSource, storage);
	}

	@Override
	@Nullable
	protected LightChunk getChunk(int x, int z) { // make it stateless
		return this.chunkSource.getChunkForLighting(x, z);
	}

	@Override
	public void notifyUpdate(int x, int y, int z) {
		this.storage.notifyUpdate(x, y, z);
	}

	@Override
	public void setLightEnabled(ChunkPos chunkPos, boolean enabled) {
		super.setLightEnabled(chunkPos, enabled);
	}
}
