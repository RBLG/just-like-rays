package teluri.mods.jlrays.light.misc;

import org.joml.Vector3i;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IBlockStateProvider {
	BlockState getState(int x, int y, int z);

	default BlockState get(Vector3i xyz) {
		return getState(xyz.x, xyz.y, xyz.z);
	}

	default BlockState get(long xyz) {
		return getState(BlockPos.getX(xyz), BlockPos.getY(xyz), BlockPos.getZ(xyz));
	}

}