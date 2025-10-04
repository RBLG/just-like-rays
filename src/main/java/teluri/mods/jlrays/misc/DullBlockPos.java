package teluri.mods.jlrays.misc;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import teluri.mods.jlrays.JustLikeRays;

/**
 * marks a checkBlock call that wasnt caught in a context where a previous blockstate was recuperable. <br/>
 * used for better debug and compatibility to show that a non ShinyBlockPos was already caught and doesnt require to log another error
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class DullBlockPos extends BlockPos {
	protected static final AtomicInteger COOLDOWN = new AtomicInteger(0);

	public DullBlockPos(BlockPos blockpos) {
		super(blockpos.getX(), blockpos.getY(), blockpos.getZ());
		warn();
	}

	@Override
	public BlockPos immutable() {
		return this;
	}
	
	public static void warn() {
		int val = COOLDOWN.getAndUpdate((v) -> v <= 0 ? 100 : v - 1);
		if (val <= 0) {
			String msg = "detected a non altered call to checkBlock, light wont be updated. this is probably a mod incompatibility (importance depends on what said mod does)."
					+ "this error will be silenced for 100 calls to avoid spam. see stack trace for more insight:";
			JustLikeRays.LOGGER.warn(msg);
			Thread.dumpStack();
		}
	}
}
