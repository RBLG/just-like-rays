package teluri.mods.jlrays.config;

import teluri.mods.jlrays.config.ILightLevelSizeHandler.LightLevelSizes;

public class JlrConfig {
	/**
	 * Config singleton
	 */
	private static JlrConfig settings = new JlrConfig();

	public static JlrConfig LazyGet() {
		if (settings == null) {
			settings = new JlrConfig();
		}
		return settings;
	}
	
	public ILightLevelSizeHandler depthHandler = LightLevelSizes.BYTE;
	
	public int precision = 0;
	
	public JlrConfig() {
		
	}
}
