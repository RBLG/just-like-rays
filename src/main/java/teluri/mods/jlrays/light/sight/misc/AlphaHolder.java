package teluri.mods.jlrays.light.sight.misc;

import org.joml.Vector3i;

/***
 * holds alpha (transparancy/opacity) values for each face of a block, plus the alpha value of the volume of the block
 * @author RBLG
 * @since v0.0.7
 */
public class AlphaHolder {
	public float block, f1, f2, f3, f4, f5, f6;

	@FunctionalInterface
	public interface IAlphaProvider {
		AlphaHolder get(Vector3i xyz, Quadrant quadr, AlphaHolder hol);
	}
}