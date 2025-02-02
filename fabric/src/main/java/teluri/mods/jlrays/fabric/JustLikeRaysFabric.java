package teluri.mods.jlrays.fabric;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.Config;

/**
 * Just like rays mod initializer
 * 
 * @author RBLG
 * @since v0.0.1
 */
public class JustLikeRaysFabric extends JustLikeRays implements ModInitializer {
	@Override
	public void onInitialize() {
		AutoConfig.register(Config.class, Toml4jConfigSerializer::new);
	}
}