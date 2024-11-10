package teluri.mods.jlrays.boilerplate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * hacky way to send blockstates to the light engine from LevelChunk/ProtoChunk.<br>
 * if you're calling checkNodes, consider wrapping your blockpos as a ShinyBlockPos
 */
public class ShinyBlockPos extends BlockPos {
	public final BlockState previous;
	public final BlockState current;

	public ShinyBlockPos(BlockPos blockpos, BlockState nprevious, BlockState ncurrent) {
		super(blockpos.getX(), blockpos.getY(), blockpos.getZ());
		previous = nprevious;
		current = ncurrent;
	}
	
	@Override
	public BlockPos immutable() {
		return this;
	}

}