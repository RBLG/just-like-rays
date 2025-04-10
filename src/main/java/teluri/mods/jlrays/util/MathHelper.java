package teluri.mods.jlrays.util;

import org.joml.Vector3i;

/**
 * @author RBLG
 * @since v0.0.7
 */
public class MathHelper {
	/**
	 * sum the components of a vector
	 * @param vec
	 * @return
	 */
	public static int sum(Vector3i vec) {
		return vec.x + vec.y + vec.z;
	}
}
