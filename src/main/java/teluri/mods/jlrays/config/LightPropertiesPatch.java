package teluri.mods.jlrays.config;

import java.util.function.Consumer;

import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.misc.IHasLightProperties;

/**
 * small trick to avoid boilerplate code in settings
 * 
 * @author RBLG
 * @since v0.2.0
 */
@FunctionalInterface
public interface LightPropertiesPatch extends Consumer<BlockStateBase> {

	default void accept(BlockStateBase bs) {
		if (bs instanceof IHasLightProperties bsep) {
			apply(bs, bsep);
			return;
		} // else
		String desid = bs.getBlock().getDescriptionId();
		String interf = IHasLightProperties.class.toString();
		JustLikeRays.LOGGER.warn("failed to modify blockstate properties, blockstate of block " + desid + " doesnt implement " + interf);
	}

	void apply(BlockStateBase bs, IHasLightProperties bselp);
}
