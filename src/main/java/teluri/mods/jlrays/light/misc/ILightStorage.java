package teluri.mods.jlrays.light.misc;

import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import teluri.mods.jlrays.light.ByteDataLayer;

/**
 * abstract the needs of the light engine to keep more independant from mc implementation (in theory)
 * 
 * @author RBLG
 * @since v0.0.7
 */
public interface ILightStorage {
	public void setLevel(long pos, int value);

	public void addLevel(long pos, int value);

	public int getLevel(long pos);

	public boolean storingLightForSection(long secpos);

	public void setLightEnabled(ChunkPos chunkPos, boolean enabled);

	public void onLightUpdateCompleted();

	public void findBlockLightSources(ChunkPos chunkPos, BiConsumer<BlockPos, BlockState> step);

	public ByteDataLayer getDataLayer(int x, int y, int z);

	public void notifyUpdate(int x, int y, int z);
}