package teluri.mods.jlrays.misc.sight;

import org.joml.Vector3i;

public class Quadrant {
	public final Vector3i axis1, axis2, axis3;

	public Quadrant(Vector3i naxis1, Vector3i naxis2, Vector3i naxis3) {
		axis1 = naxis1;
		axis2 = naxis2;
		axis3 = naxis3;
	}

}