package teluri.mods.jlrays.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.IChannelsHandler.ChannelsHandlers;
import teluri.mods.jlrays.config.ILightLevelSizeHandler.LightLevelSizes;
import teluri.mods.jlrays.config.ISamplesHandler.SamplesHandlers;

/**
 * settings for JLR.
 * 
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class Settings {
	/**
	 * Settings singleton
	 */
	public static final Settings settings = new Settings();

	public final HashMap<String, ArrayList<Consumer<BlockStateBase>>> blockstates;

	public final LightLevelSizes llsize = LightLevelSizes.BYTE;

	public final int precision = 0;

	public final ChannelsHandlers channels = ChannelsHandlers.MONO;

	public final SamplesHandlers samples = SamplesHandlers.ONE_PER_VOLUME;

	protected boolean late = false;

	public Settings() {
		blockstates = new HashMap<>();
		addEpModifier("block.minecraft.lava", (bs, bsep, ep) -> {
			bsep.setLightEmit(10);
			bsep.setLightBlock(bs.getFluidState().isSource() ? 15 : 0);
		});
		addEpModifier("block.minecraft.torch", (bs, bsep, ep) -> {
			ep.offset.set(0, 2 / 8f, 0);
			ep.radius.set(1 / 8f);
		});

	}

	public void addModifier(String key, Consumer<BlockStateBase> bsmod) {
		if (late) {
			JustLikeRays.LOGGER.warn("config modified after blockstates init started, consider using an entrypoint of type \"jlr-settings\"");
		}
		ArrayList<Consumer<BlockStateBase>> list;
		list = blockstates.computeIfAbsent(key, (v) -> new ArrayList<>());
		list.add(bsmod);
	}

	public void addEpModifier(String key, EmitPropertiesModifier bsmod) {
		this.addModifier(key, bsmod);
	}

}
