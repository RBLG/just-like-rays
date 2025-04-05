package teluri.mods.jlrays.light.sight.misc;

import org.joml.Vector3i;

/**
 * technically an octant. hold the direction of an octant compared to an origin
 * 
 * @author RBLG
 * @since v0.0.7
 */
public class Quadrant {
	public final Vector3i axis1, axis2, axis3;

	public Quadrant(Vector3i naxis1, Vector3i naxis2, Vector3i naxis3) {
		axis1 = naxis1;
		axis2 = naxis2;
		axis3 = naxis3;
	}

}