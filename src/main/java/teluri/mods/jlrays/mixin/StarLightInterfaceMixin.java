package teluri.mods.jlrays.mixin;

import java.util.ArrayDeque;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import ca.spottedleaf.starlight.common.light.BlockStarLightEngine;
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import teluri.mods.jlrays.light.starlight.JlrBlockEngineStarlightAdapter;

/**
 * @author RBLG
 * @since v0.0.6/v0.0.7
 */
@Mixin(StarLightInterface.class)
public class StarLightInterfaceMixin {
	@Shadow
	protected final ArrayDeque<BlockStarLightEngine> cachedBlockPropagators;

	@Inject(method = "getBlockLightEngine", at = { @At(value = "NEW") })
	protected BlockStarLightEngine newEngine() {
		return new JlrBlockEngineStarlightAdapter(null);
	}

	public StarLightInterfaceMixin() {
		this.cachedBlockPropagators = new ArrayDeque<BlockStarLightEngine>();

	}
}
