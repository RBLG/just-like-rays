package teluri.mods.jlrays.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import org.joml.Vector3f;

import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.EmitPropertiesPatch.EmitPropertiesFullPatch;
import teluri.mods.jlrays.misc.IHasEmitProperties.EmitProperties;

/**
 * settings for JLR.
 * 
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class BlockConfig {
	/**
	 * Config singleton
	 */
	private static BlockConfig settings = new BlockConfig();

	public static BlockConfig LazyGet() {
		if (settings == null) {
			settings = new BlockConfig();
		}
		return settings;
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
		on("block.minecraft.lava").addBasePatch((bs, bsep) -> {
			bsep.setLightEmit(10);
			bsep.setLightBlock(bs.getFluidState().isSource() ? 15 : 0);
		});
		on("block.minecraft.torch", "block.minecraft.soul_torch").addExtendedPatch((bs, bsep, ep) -> {
			ep.offset.set(0, 2, 0).div(16);
			ep.radius.set(1).div(16);
		});
		on("block.minecraft.wall_torch", "block.minecraft.soul_wall_torch").addExtendedPatch((bs, bsep, ep) -> {
			Vector3f facing = bs.getValue(BlockStateProperties.FACING).getUnitVec3().toVector3f();
			ep.offset.set(0, 2, 0).add(facing.mul(-5)).div(16);
			ep.radius.set(1).div(16);
		});
		on("block.minecraft.lantern", "block.minecraft.soul_lantern").addExtendedPatch((bs, bsep, ep) -> {
			float ofsy = bs.getValue(BlockStateProperties.HANGING) ? 4 : -4;
			ep.offset.set(0, ofsy, 0).div(16);
			ep.radius.set(2, 3, 2).div(16);
		});
		on("block.minecraft.campfire", "block.minecraft.soul_campfire").addBasePatch((bs, bsep) -> {
			if (bs.getValue(BlockStateProperties.LIT)) {
				EmitProperties ep = bsep.initEmitProperties();
				ep.radius.set(6, 4, 6).div(16);
				ep.offset.set(0, -2, 0).div(16);
			}
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
				JustLikeRays.LOGGER.warn("config modified after blockstates init started, consider using an entrypoint of type \"jlr-settings\"");
			}
			for (String key : keys) {
				blockstates.computeIfAbsent(key, (v) -> new ArrayList<>()).add(bsmod);
			}
		}

		public void addBasePatch(EmitPropertiesPatch bsmod) {
			this.addPatch(bsmod);
		}

		public void addExtendedPatch(EmitPropertiesFullPatch bsmod) {
			this.addPatch(bsmod);
		}
	}

	// TODO check between two blockstates to see if they have the same emitprops
}
