package teluri.mods.jlrays.forge;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import teluri.mods.jlrays.JustLikeRays;
import teluri.mods.jlrays.config.Config;

/**
 * Just like rays mod initializer
 * 
 * @author RBLG
 * @since v0.0.1
 */
@Mod(JustLikeRays.MOD_ID)
@Mod.EventBusSubscriber(modid = JustLikeRays.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class JustLikeRaysForge {
	public JustLikeRaysForge() {
		AutoConfig.register(Config.class, Toml4jConfigSerializer::new);

		FMLJavaModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () ->
				new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) ->
						AutoConfig.getConfigScreen(Config.class, screen).get()
				)
		);
	}
}