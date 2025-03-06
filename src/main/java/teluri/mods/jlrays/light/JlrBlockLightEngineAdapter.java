package teluri.mods.jlrays.light;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import teluri.mods.jlrays.JustLikeRays;

/**
 * @author RBLG
 * @since v0.0.7
 */
public class JlrBlockLightEngineAdapter extends LightEngine<JlrLightSectionStorage.JlrDataLayerStorageMap, JlrLightSectionStorage> {

	protected final JlrBlockLightEngine engine;

	public JlrBlockLightEngineAdapter(LightChunkGetter chunkSource) {
		this(chunkSource, new JlrLightSectionStorage(chunkSource));
	}

	protected JlrBlockLightEngineAdapter(LightChunkGetter chunkSource, JlrLightSectionStorage storage) {
		super(chunkSource, storage);
		engine = new JlrBlockLightEngine(storage, chunkSource, this);
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
	public BlockState getState(BlockPos pos) {
		return super.getState(pos);
	}

	public static boolean isEmptyShape(BlockState state) {
		return LightEngine.isEmptyShape(state);

	}

	public boolean shapeOccludes(BlockState state1, BlockState state2, Direction direction) {
		return super.shapeOccludes(state1, state2, direction);
	}

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
