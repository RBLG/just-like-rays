package teluri.mods.jlrays.config;

import teluri.mods.jlrays.config.IDepthHandler.DepthHandlers;

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
	
	public IDepthHandler depthHandler = DepthHandlers.BYTE;
	
	public int precision = 0;
	
	public JlrConfig() {
		
	}
}
