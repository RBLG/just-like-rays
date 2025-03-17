package teluri.mods.jlrays.light.starlight;

import ca.spottedleaf.starlight.common.light.StarLightInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import teluri.mods.jlrays.light.JlrBlockLightEngine;
import teluri.mods.jlrays.light.JlrBlockLightEngine.ILightStorage;

public class JlrBlockEngineStarlightAdapter implements ILightStorage {

	protected final JlrBlockLightEngine engine;
	protected final StarLightInterface slinter;

	public JlrBlockEngineStarlightAdapter(StarLightInterface nslinter) {
		slinter = nslinter;
		engine = new JlrBlockLightEngine(null, null, null, null);
	}

	public void checkBlock(BlockPos blockpos) {
		engine.checkBlock(blockpos);
	}

	public void runLightUpdates() {
		engine.runLightUpdates();
	}

	@Override
	public void setLevel(long pos, int value) {
		// TODO Auto-generated method stub

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
