package teluri.mods.jlrays.light;

import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.light.misc.ILightStorage;

/**
 * allow to abstract away mc light engine logic from the Jlr engine
 * @author RBLG
 * @since v0.0.7
 */
public class JlrBlockLightEngineAdapter extends LightEngine<JlrLightSectionStorage.JlrDataLayerStorageMap, JlrLightSectionStorage> implements ILightStorage {

	protected final JlrBlockLightEngine engine;

	public JlrBlockLightEngineAdapter(LightChunkGetter chunkSource) {
		this(chunkSource, new JlrLightSectionStorage(chunkSource));
	}

	protected JlrBlockLightEngineAdapter(LightChunkGetter chunkSource, JlrLightSectionStorage storage) {
		super(chunkSource, storage);

		engine = new JlrBlockLightEngine(this::getState, this);
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
		if (data != null && !(data instanceof ByteDataLayer)) {
			JustLikeRays.LOGGER.warn("block light data layer isnt byte sized");
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

}
