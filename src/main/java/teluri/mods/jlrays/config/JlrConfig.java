package teluri.mods.jlrays.config;


/**
 * general config for the light engine
 * 
 * @author RBLG
 * @since v0.2.0
 */
public class JlrConfig {
	/**
	 * Config singleton
	 */
	private static JlrConfig settings = null;

	public static JlrConfig LazyGet() {
		return settings == null ? (settings = new JlrConfig()) : settings;
	}

	/**
	 * memory allocated to individual light level values
	 */
	public IDepthHandler depthHandler = IDepthHandler.BYTE;

	/**
	 * bits before the point in the light level fixed point number. allow better quality when sources are stacking
	 */
	public int precision = 0;

	/***
	 * distance multiplier for light falloff
	 */
	public float distanceRatio = 0.1f;

	/**
	 * flat malus on light level value (pre rounding). allow values to hit 0 and so have a finite range
	 */
	public final float minimumValue = 0.5f;

	public JlrConfig() {

	}
}
