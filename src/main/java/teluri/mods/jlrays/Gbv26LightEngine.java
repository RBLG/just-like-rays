package teluri.mods.jlrays;

import org.joml.Vector3i;

public class Gbv26LightEngine {

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
						cones[index] = new Vector3i[] { v1, v2, v3 };
						index++;
					}
				}
			}
		}
		return ncones;
	}
	/////////////////////////////////////////////////////////////////////

	private boolean[][][] occupancy; // TODO
	private float[][][] output; // TODO

	public void DoAllCones(Vector3i source, int emit, int range) {
		for (Vector3i[] combo : combos) {// 48 times
			IterateOverCone(source, emit, range, combo[0], combo[1], combo[2]);
		}

	}

	public void IterateOverCone(Vector3i source, int emit, int range, Vector3i v1, Vector3i v2, Vector3i v3) {

		// store the visibility values
		float[][] vbuffer = new float[range + 1][range + 1];
		vbuffer[0][0] = 1; // the source

		for (int it1 = 1; it1 <= range; it1++) { // start at 1 to skip source
			Vector3i vit1 = new Vector3i(v1).mul(it1);
			float it1inv = 1f / it1;
			for (int it2 = it1; 0 <= it2; it2--) {// start from the end to handle neigbors replacement easily
				Vector3i vit2 = new Vector3i(v2).mul(it2).add(vit1);
				for (int it3 = it2; 0 <= it3; it3--) { // same than it2
					Vector3i sdist = new Vector3i(v3).mul(it3).add(vit2); // signed distance
					Vector3i xyz = new Vector3i(source).add(sdist); // world position

					if (occupancy[xyz.x][xyz.y][xyz.z]) {
						vbuffer[it2][it3] = 0;
						continue;
					}
					// weights
					int b1 = it1 - it2;
					int b2 = it2 - it3;
					int b3 = it3;

					// neigbors * their weights
					float nb1 = (b1 == 0) ? 0 : (vbuffer[it2][it3] * b1);
					float nb2 = (b2 == 0) ? 0 : (vbuffer[it2 - 1][it3] * b2);
					float nb3 = (b3 == 0) ? 0 : (vbuffer[it2 - 1][it3 - 1] * b3);

					// interpolating. it1inv is 1/(b1+b2+b3)
					float visi = (nb1 + nb2 + nb3) * it1inv;
					vbuffer[it2][it3] = visi; // replace the nb1 neigbor (as it wont be used anymore)

					if (visi == 0) {
						continue;
					}
					// end visibility computation

					// reduce the values at the edge to compensate for them being done again by
					// other cones
					float edgecoef = 1f;
					if (b1 == 0) {
						edgecoef *= 0.5f;
					}
					if (b2 == 0) {
						edgecoef *= 0.5f;
					}
					if (b3 == 0) {
						edgecoef *= 0.5f;
					}
					if (b2 == 0 && b3 == 0) {
						edgecoef *= 0.5f;
					}

					// light effects and output
					output[xyz.x][xyz.y][xyz.z] = visi * emit / (sdist.lengthSquared()) * edgecoef;
				}
			}
		}

	}

}
