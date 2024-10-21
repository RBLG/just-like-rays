package teluri.mods.jlrays;

import org.joml.Vector3i;

public class ConeTracer26Nbs {

	private static final int[] SIGNS = new int[] { 1, -1 };
	private static final Vector3i X = new Vector3i(1, 0, 0);
	private static final Vector3i Y = new Vector3i(0, 1, 0);
	private static final Vector3i Z = new Vector3i(0, 0, 1);
	private static final UCone[] UCONES = new UCone[] { //
			new UCone(X, Y, Z, 1, 0), new UCone(X, Z, Y, 0, 1), //
			new UCone(Y, X, Z, 0, 0), new UCone(Y, Z, X, 0, 1), //
			new UCone(Z, X, Y, 1, 1), new UCone(Z, Y, X, 1, 0),//
	};

	private static final Cone[] cones = genCones();

	private static Cone[] genCones() {
		Cone[] ncones = new Cone[48];
		int index = 0;
		for (UCone ucone : UCONES) {
			for (int s1 : SIGNS) {
				for (int s2 : SIGNS) {
					for (int s3 : SIGNS) { // 48 variations
						Vector3i v1 = new Vector3i(ucone.axis1).mul(s1);
						Vector3i v2 = new Vector3i(ucone.axis2).mul(s2);
						Vector3i v3 = new Vector3i(ucone.axis3).mul(s3);
						ncones[index] = new Cone(v1, v2, v3, ucone.edge1, ucone.edge2, 0 < s2, 0 < s3);
						index++;
					}
				}
			}
		}
		return ncones;
	}
	/////////////////////////////////////////////////////////////////////

	/**
	 * trace all 48 cones
	 * 
	 * @see ConeTracer26Nbs.TraceCone
	 */
	public static void TraceAllCones(Vector3i source, int range, IOpacityGetter opmap, ILightConsumer lmap) {
		for (Cone cone : cones) {// 48 times
			TraceCone(source, range, cone, opmap, lmap);
		}
	}

	/**
	 * compute visibility (and light received) over one cone
	 * 
	 * @param source:   source of the light
	 * @param emit:     emition of the source
	 * @param range:    how far should visibility by computer (to be estimated based on emit)
	 * @param v1,v2,v3: axises vectors
	 * @param opmap:    opacity provider
	 * @param lmap      visibility/light value consumer
	 */
	public static void TraceCone(Vector3i source, int range, Cone cone, IOpacityGetter opmap, ILightConsumer lmap) {
		final Vector3i vit1 = new Vector3i();
		final Vector3i vit2 = new Vector3i();
		final Vector3i sdist = new Vector3i();
		final Vector3i xyz = new Vector3i();

		// store the visibility values
		float[][] vbuffer = new float[range + 1][range + 1];
		vbuffer[0][0] = 1; // the source

		// iterate from source to range (it1)
		for (int it1 = 1; it1 <= range; it1++) { // start at 1 to skip source
			vit1.set(cone.axis1).mul(it1);
			boolean nonzero = false;
			for (int it2 = it1; 0 <= it2; it2--) {// start from the end to handle vbuffer values turnover easily
				vit2.set(cone.axis2).mul(it2).add(vit1);
				for (int it3 = it2; 0 <= it3; it3--) { // same than it2
					sdist.set(cone.axis3).mul(it3).add(vit2); // signed distance
					xyz.set(source).add(sdist); // world position

					float opacity = opmap.getOpacity(xyz.x, xyz.y, xyz.z);
					if (opacity == 0) {
						vbuffer[it2][it3] = -0.0f; // below zero values mean shadow are larger around edges
						continue;
					}
					// weights
					int w1 = it1 - it2;
					int w2 = it2 - it3;
					int w3 = it3;

					// neigbors * their weights
					float nb1 = w1 == 0 ? 0 : vbuffer[it2 + 0][it3 + 0] * w1;
					float nb2 = w2 == 0 ? 0 : vbuffer[it2 - 1][it3 + 0] * w2;
					float nb3 = w3 == 0 ? 0 : vbuffer[it2 - 1][it3 - 1] * w3;

					// interpolating. it1 = b1 + b2 + b3
					float visi = /* opacity* */ (nb1 + nb2 + nb3) / it1;
					visi = Math.max(visi, 0);
					// replace the nb1 neigbor (as this pos was the last time it was needed, it can be replaced)
					vbuffer[it2][it3] = visi;

					if (visi <= 0) {
						continue;
					}
					nonzero = true;
					// end of visibility computation

					// skip if doesnt have to do the edge
					if ((w1 == 0 && cone.edge1) || (w2 == 0 && cone.edge2) || (it2 == 0 && cone.qedge2) || (it3 == 0 && cone.qedge3)) {
						continue;
					}

					// light effects and output
					lmap.Add(xyz.x, xyz.y, xyz.z, visi, sdist);
				}
			}
			if (!nonzero) {
				return;
			}
		}

	}

	@FunctionalInterface
	public static interface IOpacityGetter {
		float getOpacity(int x, int y, int z);
	}

	@FunctionalInterface
	public static interface ILightConsumer {
		void Add(int x, int y, int z, float value, Vector3i signedDistance);
	}

	private static class Cone {
		public final Vector3i axis1;
		public final Vector3i axis2;
		public final Vector3i axis3;
		public final boolean edge1;
		public final boolean edge2;
		// public final boolean edge3;
		// quadrant edge
		public final boolean qedge2;
		public final boolean qedge3;

		public Cone(Vector3i naxis1, Vector3i naxis2, Vector3i naxis3, boolean nedge1, boolean nedge2, boolean nqedge2, boolean nqedge3) {
			axis1 = naxis1;
			axis2 = naxis2;
			axis3 = naxis3;
			edge1 = nedge1;
			edge2 = nedge2;
			// edge3 = nedge3;
			qedge2 = nqedge2;
			qedge3 = nqedge3;
		}
	}

	private static class UCone {
		public final Vector3i axis1;
		public final Vector3i axis2;
		public final Vector3i axis3;
		public final boolean edge1;
		public final boolean edge2;

		public UCone(Vector3i naxis1, Vector3i naxis2, Vector3i naxis3, int nedge1, int nedge2) {
			axis1 = naxis1;
			axis2 = naxis2;
			axis3 = naxis3;
			edge1 = nedge1 == 0;
			edge2 = nedge2 == 0;
		}
	}

}
