package teluri.mods.jlrays.config;

import java.util.function.Consumer;

import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.misc.IHasEmitProperties;
import teluri.mods.jlrays.misc.IHasEmitProperties.EmitProperties;

/**
 * small trick to avoid boilerplate code in settings
 * 
 * @author RBLG
 * @since v0.2.0
 */
@FunctionalInterface
public interface EmitPropertiesPatch extends Consumer<BlockStateBase> {

	default void accept(BlockStateBase bs) {
		if (bs instanceof IHasEmitProperties bsep) {
			apply(bs, bsep);
			return;
		} // else
		String desid = bs.getBlock().getDescriptionId();
		String interf = IHasEmitProperties.class.toString();
		JustLikeRays.LOGGER.warn("failed to modify blockstate properties, blockstate of block " + desid + " doesnt implement " + interf);
	}

	void apply(BlockStateBase bs, IHasEmitProperties bselp);

	@FunctionalInterface
	public interface EmitPropertiesFullPatch extends EmitPropertiesPatch {

		default void apply(BlockStateBase bs, IHasEmitProperties bselp) {
			var ep=bselp.initEmitProperties();
			apply(bs, bselp,ep );
		}

		void apply(BlockStateBase bs, IHasEmitProperties bselp, IHasEmitProperties.EmitProperties ep);
	}
}
