package teluri.mods.jlrays.config;

import me.fzzyhmstrs.fzzy_config.annotations.Action;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.annotations.RequiresAction;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.resources.ResourceLocation;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.IChannelsHandler.ChannelsHandlers;
import teluri.mods.jlrays.config.ISamplesHandler.SamplesHandlers;

/**
 * light engine settings
 * 
 * @author RBLG
 * @since v0.2.0
 */
@RequiresAction(action = Action.RESTART)
public class JlrConfig extends Config {
	public static final JlrConfig CONFIG = ConfigApiJava.registerAndLoadConfig(JlrConfig::new, RegisterType.BOTH);

	public JlrConfig() {
		super(ResourceLocation.fromNamespaceAndPath(JustLikeRays.MOD_ID, "lighting"));
	}

	@Comment(value = "the amount of memory allocated for a single light level sample")
	public ValidatedInt depth = new ValidatedInt(1, 4, 1);

	@Comment("how much bit are used for light levels fixed point number values")
	public ValidatedInt precision = new ValidatedInt(0, 20, 0);

	@Comment("how much distance impact light levels (lower values=more range, recommended: 1 to 0.001)")
	public ValidatedFloat scale = new ValidatedFloat(0.1f, Float.MAX_VALUE, Float.MIN_NORMAL);

	@Comment("minimum value after which exposition is considered 0 for optimization purpose (recommended: 0.5 to 0.1)")
	public ValidatedFloat cutoff = new ValidatedFloat(0.5f, Float.MAX_VALUE, Float.MIN_NORMAL);

	@Comment("mode for handling color channels,"//
			+ "mono=no colored light," //
			+ "rgb= colored light but not impacted by tinted transparents blocks(not in 0.2.0),"//
			+ "full_rgb= rgb light but impacted by tinted transparent blocks (not in 0.2.0)")
	public ValidatedField<IChannelsHandler> channels = (new ValidatedString(IChannelsHandler.CHANNELS_HANDLERS.get(ChannelsHandlers.MONO))).map(//
			IChannelsHandler.CHANNELS_HANDLERS.inverse()::get, //
			IChannelsHandler.CHANNELS_HANDLERS::get);

	public ValidatedField<ISamplesHandler> samples = (new ValidatedString(ISamplesHandler.SAMPLES_HANDLERS.get(SamplesHandlers.ONE_PER_VOLUME))).map(//
			ISamplesHandler.SAMPLES_HANDLERS.inverse()::get, //
			ISamplesHandler.SAMPLES_HANDLERS::get);

	@Override
	public int defaultPermLevel() {
		return 4;
	}

	public static void init() {
	}

	public void onSyncClient() {
		CurrentConfig.current = new CurrentConfig();
	}

	// TODO make ByteDataLayer store the settings it was filled under
	// TODO find what trigger a light data init
}
