package teluri.mods.jlrays.config;

public class JlrConfig {
	/**
	 * Config singleton
	 */
	private static JlrConfig settings = null;

	public static JlrConfig LazyGet() {
		return settings == null ? (settings = new JlrConfig()) : settings;
	}

	public IDepthHandler depthHandler = IDepthHandler.BYTE;

	public int precision = 0;

	public float distanceRatio = 0.1f;

	public final float minimumValue = 0.5f;

	public JlrConfig() {

	}
}
