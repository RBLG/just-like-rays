package teluri.mods.jlrays.misc.light;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockStateProvider {
	BlockState get(BlockPos pos);
}