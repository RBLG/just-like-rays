package teluri.mods.jlrays;

import org.joml.Math;

/**
 * @author RBLG
 * @since v0.0.4
 * 
 * handles methods for tonemapping
 */
public final class ToneMapperHelper {
	private ToneMapperHelper() {}
	
	/**
	 * reinhard tonemapping from https://64.github.io/tonemapping/#reinhard
	 * @param define the scale of the input value (similar to dividing by "one" the input value)
	 * @param define the scale of the output value (multiplies the output by "one2"
	 */
	public static float tonemap(float value, float one, float one2) {
		return Math.min(value / (one + value) * one2 * 1.4f, 15);
	}

	/**
	 * extended reinhard tonemapping from https://64.github.io/tonemapping/#extended-reinhard
	 */
	public static float extendedTonemap(float value, float white, float one) {
		float v = value;
		float numerator = v * (1.0f + (v / white * white));
		float result = numerator / (1.0f + v);
		return Math.min(result * one, 15);
	}
	
	/**
	 * cheap alternative to take advantage of the limited range of values
	 */
	public static int cheapTonemap(int value) {
		if (value <= 13) {
			return value;
		} else if (value <= 30) {
			return 14;
		} else {
			return 15;
		}
	}

}
