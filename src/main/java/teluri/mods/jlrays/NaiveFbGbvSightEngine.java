package teluri.mods.jlrays;

import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * naive implementation of the Face Based GBV algorithm, which is an evolution of GBV that attribute a value per face, instead of per voxel. <br>
 * this come with the precision of 18 neigbors 3d GBV but without the leaks that happen with anything further than 6neigbors. <br>
 * it drawback is having to interpolate for 3 values instead of 1
 * 
 * @author RBLG
 * @since v0.0.4
 */
public class NaiveFbGbvSightEngine {
	private static final int[] SIGNS = new int[] { 1, -1 };
	// positive axis
	private static final Vector3i X = new Vector3i(1, 0, 0);
	private static final Vector3i Y = new Vector3i(0, 1, 0);
	private static final Vector3i Z = new Vector3i(0, 0, 1);

	public static record Quadrant(Vector3i axis1, Vector3i axis2, Vector3i axis3) {}

	private static final Quadrant[] QUADRANTS = genQuadrants();

	/**
	 * generates data of the 8 quadrants
	 */
	private static Quadrant[] genQuadrants() {
		Quadrant[] ncones = new Quadrant[8];
		int index = 0;
		for (int s1 : SIGNS) {
			for (int s2 : SIGNS) {
				for (int s3 : SIGNS) { // 8 variations
					Vector3i v1 = new Vector3i(X).mul(s1);
					Vector3i v2 = new Vector3i(Y).mul(s2);
					Vector3i v3 = new Vector3i(Z).mul(s3);
					ncones[index] = new Quadrant(v1, v2, v3);
					index++;
				}
			}
		}

		return ncones;
	}

	@FunctionalInterface
	public static interface IAlphaProvider {
		float get(Vector3i xyz);
	}

	@FunctionalInterface
	public static interface ISightConsumer {
		void consume(Vector3i xyz, float visi, float alpha, double distance);
	}

	@FunctionalInterface
	public static interface ISightUpdateConsumer {
		void consume(Vector3i xyz, float ovisi, float nvisi, double distance);
	}

	public static int sum(Vector3i vec) {
		return vec.x + vec.y + vec.z;
	}

	public static int max(int a, int b) {
		return Math.max(a, b);
	}


	/**
	 * run the FBGBV algorithm over all quadrant
	 * 
	 * @param origin position of the source of the light
	 * @param range  max range that is iterated over
	 * @param aprov  alpha provider
	 * @param scons  sight consumer (output)
	 */
	public static void traceAllQuadrants(Vector3i source, int range, IAlphaProvider aprov, ISightConsumer scons) {
		for (Quadrant quadrant : QUADRANTS) {
			traceQuadrant(source, range, quadrant, aprov, scons);
		}
	}

	public static void traceAllChangedQuadrants(Vector3i source, Vector3i offset, int range, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer sucons) {
		Vector3i vtmp = new Vector3i();
		for (Quadrant quadrant : QUADRANTS) {
			int comp1 = sum(vtmp.set(offset).mul(quadrant.axis1));
			int comp2 = sum(vtmp.set(offset).mul(quadrant.axis2));
			int comp3 = sum(vtmp.set(offset).mul(quadrant.axis3));
			if (0 <= comp1 && 0 <= comp2 && 0 <= comp3) {
				traceChangedQuadrant(source, range, quadrant, oaprov, naprov, sucons);
			}
		}
	}

	/**
	 * run the FBGBV algorithm over a quadrant
	 * 
	 * @param origin position of the source of the light
	 * @param range  max range that is iterated over
	 * @param quadr  quadrant currently processed
	 * @param aprov  alpha provider
	 * @param scons  sight consumer (output)
	 */
	public static void traceQuadrant(Vector3i origin, int range, Quadrant quadr, IAlphaProvider aprov, ISightConsumer scons) {
		final Vector3i vit1 = new Vector3i();
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		if (range <= 0) {
			return;
		}

		int size = range + 2;

		// store the sight/visibility values of the last plane iterated over for incoming steps
		float[] vbuffer = new float[size * size * 3];

		// initialize the source sight values to visible
		vbuffer[0 + 0] = 1; // the source (1,0,0) neigbor's face 1
		vbuffer[0 + size * 3 + 1] = 1; // the source (0,1,0) neigbor's face 2
		vbuffer[0 + 3 + 2] = 1; // the source (0,0,1) neigbor's face 3

		for (int it1 = 0; it1 <= range; it1++) {
			vit1.set(quadr.axis1).mul(it1); // first component of xyz
			for (int it2 = 0; it2 <= range; it2++) {
				vit2.set(quadr.axis2).mul(it2).add(vit1); // second component of xyz
				for (int it3 = 0; it3 <= range; it3++) {
					if (it1 + it2 + it3 == 0) {
						continue; // skip if on the origin
					}

					int index = ((it2 * size) + it3) * 3; // translate 2d coordinates to 1d for use as an array index

					// get this voxel exposed faces from the buffer
					float face1 = vbuffer[index + 0];
					float face2 = vbuffer[index + 1];
					float face3 = vbuffer[index + 2];

					if (face1 == 0 && face2 == 0 && face3 == 0) {
						continue; // if none of the faces had sight, skip this step
					}

					xyz.set(quadr.axis3).mul(it3).add(vit2).add(origin); // world position

					float alpha = aprov.get(xyz); // get this voxel alpha

					// output the sight unless its an edge without the priority and therefore skip to avoid duplicated edges output
					if (!((it1 == 0 && quadr.axis1.x <= 0) || (it2 == 0 && quadr.axis2.y <= 0) || (it3 == 0 && quadr.axis3.z <= 0))) {
						float voxelvisi = (face1 * it1 + face2 * it2 + face3 * it3) / (it1 + it2 + it3);
						double dist = 1 + Vector3f.lengthSquared(it1 * 0.3f, it2 * 0.3f, it3 * 0.3f);
						scons.consume(xyz, voxelvisi, alpha, dist);
					}
					// weights are similar to 18 neigbors 3d classic gbv
					// weights are it1, it2 and it3 except for those 3
					int f4w1 = max(0, it1 - max(it2, it3)); // weight 1 for face 4. reach 0 when either it2 or it3 reach it1
					int f5w2 = max(0, it2 - max(it1, it3)); // weight 2 for face 5
					int f6w3 = max(0, it3 - max(it1, it2)); // weight 3 for face 6

					float face4, face5, face6;
					if (alpha == 0) {
						face4 = face5 = face6 = 0; //skip the computation
					} else {
						face4 = (face1 * f4w1 + face2 * it2 + face3 * it3) / (f4w1 + it2 + it3) * alpha;
						face5 = (face1 * it1 + face2 * f5w2 + face3 * it3) / (it1 + f5w2 + it3) * alpha;
						face6 = (face1 * it1 + face2 * it2 + face3 * f6w3) / (it1 + it2 + f6w3) * alpha;
					}
					
					// write the results to the non exposed faces of this voxel (which are also the exposed faces of later processed voxels)
					vbuffer[index + 0] = face4;
					vbuffer[index + size * 3 + 1] = face5;
					vbuffer[index + 3 + 2] = face6;
				}
			}
		}

	}

	/**
	 * run the FBGBV algorithm over a quadrant while tracking the visibility before and after a block update<br>
	 * refer to traceQuadrant(..) for more in depth comments
	 * 
	 * @param origin position of the source of the light
	 * @param range  max range that is iterated over
	 * @param quadr  quadrant currently processed
	 * @param oaprov old visibility provider
	 * @param naprov new visibility provider
	 * @param sucons sight consumer
	 */
	public static void traceChangedQuadrant(Vector3i origin, int range, Quadrant quadr, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer sucons) {
		final Vector3i vit1 = new Vector3i();
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		if (range <= 0) {
			return;
		}

		int size = range + 2;

		// store the visibility values
		float[] ovbuffer = new float[size * size * 3]; // variables prefixed with o (for old) refer to pre block update
		float[] nvbuffer = new float[size * size * 3]; // variables prefixed with n (for new) refer to post block update

		ovbuffer[0 + 0] = 1; // the source (1,0,0) neigbor's face 1
		ovbuffer[0 + size * 3 + 1] = 1; // the source (0,1,0) neigbor's face 2
		ovbuffer[0 + 3 + 2] = 1; // the source (0,0,1) neigbor's face 3

		nvbuffer[0 + 0] = 1;
		nvbuffer[0 + size * 3 + 1] = 1;
		nvbuffer[0 + 3 + 2] = 1;

		for (int it1 = 0; it1 <= range; it1++) {
			vit1.set(quadr.axis1).mul(it1);
			for (int it2 = 0; it2 <= range; it2++) {
				vit2.set(quadr.axis2).mul(it2).add(vit1);
				for (int it3 = 0; it3 <= range; it3++) {
					if (it1 + it2 + it3 == 0) {
						continue;
					}

					int index = ((it2 * size) + it3) * 3;

					float oface1 = ovbuffer[index + 0];
					float oface2 = ovbuffer[index + 1];
					float oface3 = ovbuffer[index + 2];

					float nface1 = nvbuffer[index + 0];
					float nface2 = nvbuffer[index + 1];
					float nface3 = nvbuffer[index + 2];

					if (oface1 == 0 && oface2 == 0 && oface3 == 0 && nface1 == 0 && nface2 == 0 && nface3 == 0) {
						continue;
					}

					xyz.set(quadr.axis3).mul(it3).add(vit2).add(origin); // world position

					float oalpha = oaprov.get(xyz);
					float nalpha = naprov.get(xyz);

					// avoid duplicated edges
					if (!((it1 == 0 && quadr.axis1.x <= 0) || (it2 == 0 && quadr.axis2.y <= 0) || (it3 == 0 && quadr.axis3.z <= 0))) {
						if (oalpha != 0 || nalpha != 0) { // add check to if oalpha==nalpha
							float ovoxelvisi = (oface1 * it1 + oface2 * it2 + oface3 * it3) / (it1 + it2 + it3) * oalpha;
							float nvoxelvisi = (nface1 * it1 + nface2 * it2 + nface3 * it3) / (it1 + it2 + it3) * nalpha;
							double dist = 1 + Vector3f.lengthSquared(it1 * 0.3f, it2 * 0.3f, it3 * 0.3f);
							// check if ovisi=nvisi?
							sucons.consume(xyz, ovoxelvisi, nvoxelvisi, dist);
						}
					}

					// weights are it1, it2 and it3 except for those 3
					// weight 1 for face 4
					int f4w1 = max(0, it1 - max(it2, it3));
					// weight 2 for face 5
					int f5w2 = max(0, it2 - max(it1, it3));
					// weight 3 for face 6
					int f6w3 = max(0, it3 - max(it1, it2));

					float oface4, oface5, oface6;
					if (oalpha == 0) {
						oface4 = oface5 = oface6 = 0;
					} else {
						oface4 = (oface1 * f4w1 + oface2 * it2 + oface3 * it3) / (f4w1 + it2 + it3) * oalpha;
						oface5 = (oface1 * it1 + oface2 * f5w2 + oface3 * it3) / (it1 + f5w2 + it3) * oalpha;
						oface6 = (oface1 * it1 + oface2 * it2 + oface3 * f6w3) / (it1 + it2 + f6w3) * oalpha;
					}
					ovbuffer[index + 0] = oface4;
					ovbuffer[index + size * 3 + 1] = oface5;
					ovbuffer[index + 3 + 2] = oface6;

					float nface4, nface5, nface6;
					if (nalpha == 0) {
						nface4 = nface5 = nface6 = 0;
					} else {
						nface4 = (nface1 * f4w1 + nface2 * it2 + nface3 * it3) / (f4w1 + it2 + it3) * nalpha;
						nface5 = (nface1 * it1 + nface2 * f5w2 + nface3 * it3) / (it1 + f5w2 + it3) * nalpha;
						nface6 = (nface1 * it1 + nface2 * it2 + nface3 * f6w3) / (it1 + it2 + f6w3) * nalpha;

					}
					nvbuffer[index + 0] = nface4;
					nvbuffer[index + size * 3 + 1] = nface5;
					nvbuffer[index + 3 + 2] = nface6;
				}
			}
		}

	}

}
