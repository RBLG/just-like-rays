package teluri.mods.jlrays;

import org.joml.Vector3i;

public class ConeTracer26Nbs {

	private static final int[] signs = new int[] { 1, -1 };
	private static final Vector3i[] axises = new Vector3i[] { new Vector3i(1, 0, 0), new Vector3i(0, 1, 0), new Vector3i(0, 0, 1) };
	private static final Vector3i[][] combos = new Vector3i[][] { //
			new Vector3i[] { axises[0], axises[1], axises[2] }, new Vector3i[] { axises[0], axises[2], axises[1] }, //
			new Vector3i[] { axises[1], axises[0], axises[2] }, new Vector3i[] { axises[1], axises[2], axises[0] }, //
			new Vector3i[] { axises[2], axises[0], axises[1] }, new Vector3i[] { axises[2], axises[1], axises[0] },//
	};

	private static final Vector3i[][] cones = genCones();

	private static Vector3i[][] genCones() {
		Vector3i[][] ncones = new Vector3i[48][3];
		int index = 0;
		for (Vector3i[] combo : combos) {
			for (int s1 : signs) {
				for (int s2 : signs) {
					for (int s3 : signs) { // 48 variations
						Vector3i v1 = new Vector3i(combo[0]).mul(s1);
						Vector3i v2 = new Vector3i(combo[1]).mul(s2);
						Vector3i v3 = new Vector3i(combo[2]).mul(s3);
						ncones[index] = new Vector3i[] { v1, v2, v3 };
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
		for (Vector3i[] cones : cones) {// 48 times
			TraceCone(source, range, cones[0], cones[1], cones[2], opmap, lmap);
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
	public static void TraceCone(Vector3i source, int range, Vector3i v1, Vector3i v2, Vector3i v3, IOpacityGetter opmap, ILightConsumer lmap) {
		final Vector3i vit1 = new Vector3i();
		final Vector3i vit2 = new Vector3i();
		final Vector3i sdist = new Vector3i();
		final Vector3i xyz = new Vector3i();
		
		
		// store the visibility values
		float[][] vbuffer = new float[range + 1][range + 1];
		vbuffer[0][0] = 1; // the source

		// iterate from source to range (it1)
		for (int it1 = 1; it1 <= range; it1++) { // start at 1 to skip source
			vit1.set(v1).mul(it1);
			boolean nonzero = false;
			for (int it2 = it1; 0 <= it2; it2--) {// start from the end to handle vbuffer values turnover easily
				vit2.set(v2).mul(it2).add(vit1);
				for (int it3 = it2; 0 <= it3; it3--) { // same than it2
					sdist.set(v3).mul(it3).add(vit2); // signed distance
					xyz.set(source).add(sdist); // world position

					float opacity = opmap.getOpacity(xyz.x, xyz.y, xyz.z);
					if (opacity == 0) {
						vbuffer[it2][it3] = 0;
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
					float visi = /*opacity* */ (nb1 + nb2 + nb3) / it1;

					// replace the nb1 neigbor (as this pos was the last time it was needed, it can be replaced)
					vbuffer[it2][it3] = visi;

					if (visi == 0) {
						continue;
					}
					nonzero = true;
					// end of visibility computation

					// reduce the values at the edges to compensate for them being done again by other cones
					float edgecoef = 1;
					edgecoef *= w1 == 0 ? 0.5 : 1;
					edgecoef *= w2 == 0 ? 0.5 : 1;
					edgecoef *= w3 == 0 ? 0.5 : 1;
					edgecoef *= w3 == 0 && w2 == 0 ? 0.5 : 1; // this edge get done 8 time and so need an extra *0.5

					// light effects and output
					lmap.Add(xyz.x, xyz.y, xyz.z, visi * edgecoef, sdist);
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

}
