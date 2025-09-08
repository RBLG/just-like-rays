package teluri.mods.jlrays.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import teluri.mods.jlrays.JustLikeRays;

/**
 * config for blockstates modification
 * 
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class BlockConfig {
	/**
	 * Config singleton
	 */
	private static BlockConfig settings = null;

	public static BlockConfig LazyGet() {
		return settings == null ? (settings = new BlockConfig()) : settings;
	}

	public final HashMap<String, ArrayList<Consumer<BlockStateBase>>> blockstates = new HashMap<>();

	protected boolean late = false;

	// will be valid once all blockstates are initialized (so on world loading should be fine)
	public float maxEmission = 0;

	public float maxRange = 15;

	public BlockConfig() {
		addDefaultPatches();
	}

	private void addDefaultPatches() {
		on("block.minecraft.lava").addLightPatch((bs, bsep) -> {
			bsep.setLightEmit(10);
			bsep.setLightBlock(bs.getFluidState().isSource() ? 15 : 0);
		});
	}

	public float getMaxEmission() {
		return maxEmission;
	}

	public void notifyInitCache() {
		late = true;
	}

	public Builder on(String... keys) {
		return new Builder(keys);
	}

	public class Builder {
		final String[] keys;

		public Builder(String[] nkeys) {
			keys = nkeys;
		}

		public void addPatch(Consumer<BlockStateBase> bsmod) {
			if (late) {
				JustLikeRays.LOGGER.warn("config modified after blockstates init started, consider using an entrypoint of type \"jlr-config\"");
			}
			for (String key : keys) {
				blockstates.computeIfAbsent(key, (v) -> new ArrayList<>()).add(bsmod);
			}
		}

		public void addLightPatch(LightPropertiesPatch bsmod) {
			this.addPatch(bsmod);
		}

	}
}
