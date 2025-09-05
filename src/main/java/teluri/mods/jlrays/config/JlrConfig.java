package teluri.mods.jlrays.config;

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

	public IDepthHandler depthHandler = IDepthHandler.BYTE;

	public int precision = 0;

	public float distanceRatio = 0.01f;

	public final float minimumValue = 0.5f;

	public float getRangeEdgeNumber() { //TODO make getter/setters
		return 1 / (minimumValue * distanceRatio);
	}

	public JlrConfig() {

	}
}
