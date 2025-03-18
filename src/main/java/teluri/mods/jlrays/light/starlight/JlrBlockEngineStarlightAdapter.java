package teluri.mods.jlrays.light.starlight;

import java.util.Set;

import ca.spottedleaf.starlight.common.light.BlockStarLightEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LightChunkGetter;
import teluri.mods.jlrays.light.JlrBlockLightEngine;
import teluri.mods.jlrays.light.JlrBlockLightEngine.ILightStorage;

public class JlrBlockEngineStarlightAdapter extends BlockStarLightEngine implements ILightStorage {

	protected final JlrBlockLightEngine engine;
	protected final Level level;

	public JlrBlockEngineStarlightAdapter(Level nlevel) {
		super(nlevel);
		level = nlevel;
		engine = new JlrBlockLightEngine(null, null, null, nlevel);
	}

	@Override
	protected void propagateBlockChanges(LightChunkGetter lightAccess, ChunkAccess atChunk, Set<BlockPos> positions) {
		for (final BlockPos pos : positions) {
			engine.checkBlock(pos);
		}

		engine.runLightUpdates();
	}

	@Override
	public void setLevel(long pos, int value) {
		//TODO cant with this architecture
	}

	@Override
	public void addLevel(long pos, int value) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getLevel(long pos) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean storingLightForSection(long secpos) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setLightEnabled(ChunkPos chunkPos, boolean enabled) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLightUpdateCompleted() {
		// TODO Auto-generated method stub

	}

}
