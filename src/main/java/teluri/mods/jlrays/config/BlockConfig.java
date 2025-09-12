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

	/**
	 * initialize config if 1st call and return it
	 * @return
	 */
	public static BlockConfig LazyGet() {
		return settings == null ? (settings = new BlockConfig()) : settings;
	}

	public final HashMap<String, ArrayList<Consumer<BlockStateBase>>> blockstates = new HashMap<>();

	protected boolean late = false;

	// will be valid once all blockstates are initialized (so on world loading should be fine)
	public float maxEmission = 0;

	public BlockConfig() {
		addDefaultPatches();
	}

	/**
	 * modify lava properties for performance reasons
	 */
	private void addDefaultPatches() {
		on("block.minecraft.lava").addLightPatch((bs, bsep) -> {
			bsep.setLightEmit(10);
			bsep.setLightBlock(bs.getFluidState().isSource() ? 15 : 0);
		});
	}

	/**
	 * get the highest emit to know how far to scout for sources on block updates
	 * @return
	 */
	public float getMaxEmission() {
		return maxEmission;
	}

	/**
	 * tells the config that blockstate initialization started and further changes wont be used
	 */
	public void notifyInitCache() {
		late = true;
	}

	/**
	 * use as on(...).addPatch(...); to add a patch to one or multiple blockstates
	 */
	public Builder on(String... keys) {
		return new Builder(keys);
	}

	/** 
	 * builder for blockstate patches
	 */
	public class Builder {
		final String[] keys;

		public Builder(String[] nkeys) {
			keys = nkeys;
		}

		/**
		 * add a barebone patch on a blockstatebase
		 */
		public void addPatch(Consumer<BlockStateBase> bsmod) {
			if (late) {
				JustLikeRays.LOGGER.warn("config modified after blockstates init started, consider using an entrypoint of type \"jlr-config\"");
			}
			for (String key : keys) {
				blockstates.computeIfAbsent(key, (v) -> new ArrayList<>()).add(bsmod);
			}
		}
		
		/**
		 * add a patch on a blockstatebase with included access to blockstate emit and opacity
		 */
		public void addLightPatch(LightPropertiesPatch bsmod) {
			this.addPatch(bsmod);
		}

	}
}
