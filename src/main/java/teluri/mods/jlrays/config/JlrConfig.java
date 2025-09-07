package teluri.mods.jlrays.config;

public class JlrConfig {
	/**
	 * Config singleton
	 */
	private static JlrConfig settings = null;

	public static JlrConfig LazyGet() {
		return settings == null ? (settings = new JlrConfig()) : settings;
	}

	public IDepthHandler depthHandler = IDepthHandler.SHORT;

	public int precision = 7;

	public float distanceRatio = 0.1f;

	public final float minimumValue = 0.25f;

	public JlrConfig() {

	}
}
