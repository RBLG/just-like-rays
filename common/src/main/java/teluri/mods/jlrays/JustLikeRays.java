package teluri.mods.jlrays;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import teluri.mods.jlrays.config.Config;

/**
 * Just like rays mod initializer
 * 
 * @author RBLG
 * @since v0.0.1
 */
public class JustLikeRays implements ModInitializer {
	public static final String MOD_ID = "just_like_rays";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Config CONFIG;

	public void onInitialize() {
		AutoConfig.register(Config.class, Toml4jConfigSerializer::new);

		JustLikeRays.CONFIG = AutoConfig.getConfigHolder(Config.class).getConfig();
	}
}