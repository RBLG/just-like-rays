package teluri.mods.jlrays.light.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IBlockStateProvider {
	BlockState get(BlockPos pos);
}