package teluri.mods.jlrays.light;

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

	public static class AlphaHolder {
		public float block, f1, f2, f3, f4, f5, f6;
	}

	@FunctionalInterface
	public static interface IAlphaProvider {
		AlphaHolder get(Vector3i xyz, Quadrant quadr, AlphaHolder hol);
	}

	@FunctionalInterface
	public static interface ISightUpdateConsumer {
		void consume(Vector3i xyz, float ovisi, float nvisi);
	}

	@FunctionalInterface
	public static interface IBlockUpdateIterator {
		void forEach(IBlockUpdateStep cons);
	}

	@FunctionalInterface
	public static interface IBlockUpdateStep {
		boolean consume(int x, int y, int z);
	}

	public static int sum(Vector3i vec) {
		return vec.x + vec.y + vec.z;
	}

	public static float max(float a, float b) {
		return Math.max(a, b);
	}

	/**
	 * standard weighted interpolation <br>
	 * 
	 * @return (val1 * w1 + val2 * w2 + val3 * w3) / (w1 + w2 + w3)
	 */
	public static float interpolate(float val1, float w1, float val2, float w2, float val3, float w3) {
		return (val1 * w1 + val2 * w2 + val3 * w3) / (w1 + w2 + w3);
	}

	/**
	 * check if a quadrant should draw blocks when on an edge shared with another quadrant
	 */
	public static boolean isNotDuplicatedEdge(Quadrant quadr, int itr1, int itr2, int itr3) {
		return (itr1 != 0 || 0 <= quadr.axis1.x) && (itr2 != 0 || 0 <= quadr.axis2.y) && (itr3 != 0 || 0 <= quadr.axis3.z);
	}

	/**
	 * get the visibility value for the block based on the visibility values of the faces and their weights
	 */
	public static float facesToVolumeValue(float val1, float w1, float val2, float w2, float val3, float w3) {
		// innacurate but diagonal walls dont cast shadows on themselves
		// return max(val1, max(val2, val3));
		// accurate output but might look worse.
		return interpolate(val1, w1, val2, w2, val3, w3);
	}

	/**
	 * run the FBGBV algorithm over all quadrant
	 * 
	 * @param origin position of the source of the light
	 * @param range  max range that is iterated over
	 * @param aprov  alpha provider
	 * @param scons  sight consumer (output)
	 */
	public static void traceAllQuadrants(Vector3i source, int range, IAlphaProvider aprov, ISightUpdateConsumer scons) {
		for (Quadrant quadrant : QUADRANTS) {
			traceQuadrant(source, range, quadrant, aprov, scons, false);
		}
	}

	/**
	 * scout the visible area around a position to see if there is a visible light source
	 */
	public static void scoutAllQuadrants(Vector3i pos, int range, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer sucons) {
		for (Quadrant quadrant : QUADRANTS) {
			traceChangedQuadrant(pos, range, quadrant, oaprov, naprov, sucons, true);
		}
	}

	/**
	 * scoute the visible area around a position when there is no block updates in range
	 */
	public static void scoutAllQuadrantsUpdateless(Vector3i source, int range, IAlphaProvider aprov, ISightUpdateConsumer scons) {
		for (Quadrant quadrant : QUADRANTS) {
			traceQuadrant(source, range, quadrant, aprov, scons, true);
		}
	}

	/**
	 * update quadrants impacted by a single block update (unused)
	 */
	public static void traceAllChangedQuadrants(Vector3i source, Vector3i target, int range, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer sucons) {
		Vector3i vtmp = new Vector3i();
		for (Quadrant quadrant : QUADRANTS) {
			int comp1 = sum(vtmp.set(target).sub(source).mul(quadrant.axis1));
			int comp2 = sum(vtmp.set(target).sub(source).mul(quadrant.axis2));
			int comp3 = sum(vtmp.set(target).sub(source).mul(quadrant.axis3));
			if (0 <= comp1 && 0 <= comp2 && 0 <= comp3) {
				traceChangedQuadrant(source, range, quadrant, oaprov, naprov, sucons, false);
			}
		}
	}

	/**
	 * update the entire area around a source while handling block updates in range
	 */
	public static void traceAllQuadrants2(Vector3i source, int range, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer sucons) {
		for (Quadrant quadrant : QUADRANTS) {
			traceChangedQuadrant(source, range, quadrant, oaprov, naprov, sucons, false);
		}
	}

	/**
	 * update the quadrants around a source that are impacted by at least one block update
	 */
	public static void traceAllChangedQuadrants2(Vector3i source, int range, IBlockUpdateIterator iter, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer sucons) {
		Vector3i vtmp = new Vector3i();
		// check if there's a block update in a quadrant and if so, update the quadrant
		for (Quadrant quadrant : QUADRANTS) {
			iter.forEach((x, y, z) -> {
				int comp1 = sum(vtmp.set(x, y, z).sub(source).mul(quadrant.axis1));
				int comp2 = sum(vtmp.set(x, y, z).sub(source).mul(quadrant.axis2));
				int comp3 = sum(vtmp.set(x, y, z).sub(source).mul(quadrant.axis3));
				if (0 <= comp1 && 0 <= comp2 && 0 <= comp3) {
					traceChangedQuadrant(source, range, quadrant, oaprov, naprov, sucons, false);
					return true;
				}
				return false;
			});
		}
	}

	/**
	 * run the FBGBV algorithm over a quadrant with no block updates in range
	 * 
	 * @param origin position of the source of the light
	 * @param range  max range that is iterated over
	 * @param quadr  quadrant currently processed
	 * @param aprov  alpha provider
	 * @param scons  sight consumer (output)
	 */
	public static void traceQuadrant(Vector3i origin, int range, Quadrant quadr, IAlphaProvider aprov, ISightUpdateConsumer scons, boolean scout) {
		final Vector3i vit1 = new Vector3i();
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		AlphaHolder ahol = new AlphaHolder();
		if (range <= 0) {
			return;
		}
		// TODO implement early return
		int size = range + 2;

		// store the sight/visibility values of the last plane iterated over for incoming steps
		float[] vbuffer = new float[size * size * 3];

		// initialize the source sight values to visible
		vbuffer[0 + 0] = 1; // the source (1,0,0) neigbor's face 1
		vbuffer[0 + size * 3 + 1] = 1; // the source (0,1,0) neigbor's face 2
		vbuffer[0 + 3 + 2] = 1; // the source (0,0,1) neigbor's face 3

		for (int itr1 = 0; itr1 <= range; itr1++) {
			vit1.set(quadr.axis1).mul(itr1); // first component of xyz
			for (int itr2 = 0; itr2 <= range; itr2++) {
				vit2.set(quadr.axis2).mul(itr2).add(vit1); // second component of xyz
				for (int itr3 = 0; itr3 <= range; itr3++) {
					if (itr1 + itr2 + itr3 == 0) {
						continue; // skip if on the origin
					}

					int index = ((itr2 * size) + itr3) * 3; // translate 2d coordinates to 1d for use as an array index

					// get this voxel exposed faces from the buffer
					float face1 = vbuffer[index + 0];
					float face2 = vbuffer[index + 1];
					float face3 = vbuffer[index + 2];

					if (face1 == 0 && face2 == 0 && face3 == 0) {
						continue; // if none of the faces had sight, skip this step
					}

					xyz.set(quadr.axis3).mul(itr3).add(vit2).add(origin); // world position

					AlphaHolder alpha = aprov.get(xyz, quadr, ahol); // get this voxel alpha

					face1 *= alpha.f1;
					face2 *= alpha.f2;
					face3 *= alpha.f3;

					// output the sight unless its an edge without the priority and therefore skip to avoid duplicated edges output
					if (isNotDuplicatedEdge(quadr, itr1, itr2, itr3)) {
						if (scout) {
							scons.consume(xyz, 1, 1);
						} else if (alpha.block != 0 && (face1 != 0 || face2 != 0 || face3 != 0)) {
							float voxelvisi = facesToVolumeValue(face1, itr1, face2, itr2, face3, itr3) * alpha.block;
							scons.consume(xyz, voxelvisi, voxelvisi);
						}
					}
					// weights are similar to 18 neigbors 3d classic gbv
					// weights are it1, it2 and it3 except for those 3
					float f4w1 = max(0, itr1 - max(itr2, itr3) + 0.5f); // weight 1 for face 4. reach 0 when either it2 or it3 reach it1
					float f5w2 = max(0, itr2 - max(itr1, itr3) + 0.5f); // weight 2 for face 5
					float f6w3 = max(0, itr3 - max(itr1, itr2) + 0.5f); // weight 3 for face 6

					float face4, face5, face6;
					if (alpha.block == 0) {
						face4 = face5 = face6 = 0; // skip the computation
					} else {
						face4 = interpolate(face1, f4w1, face2, itr2, face3, itr3) * alpha.f4;
						face5 = interpolate(face1, itr1, face2, f5w2, face3, itr3) * alpha.f5;
						face6 = interpolate(face1, itr1, face2, itr2, face3, f6w3) * alpha.f6;
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
	public static void traceChangedQuadrant(Vector3i origin, int range, Quadrant quadr, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer sucons, boolean scout) {
		final Vector3i vit1 = new Vector3i();
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		AlphaHolder oahol = new AlphaHolder();
		AlphaHolder nahol = new AlphaHolder();
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

		for (int itr1 = 0; itr1 <= range; itr1++) {
			vit1.set(quadr.axis1).mul(itr1);
			for (int itr2 = 0; itr2 <= range; itr2++) {
				vit2.set(quadr.axis2).mul(itr2).add(vit1);
				for (int itr3 = 0; itr3 <= range; itr3++) {
					if (itr1 + itr2 + itr3 == 0) {
						continue;
					}

					int index = ((itr2 * size) + itr3) * 3;

					float oface1 = ovbuffer[index + 0];
					float oface2 = ovbuffer[index + 1];
					float oface3 = ovbuffer[index + 2];

					float nface1 = nvbuffer[index + 0];
					float nface2 = nvbuffer[index + 1];
					float nface3 = nvbuffer[index + 2];

					if (oface1 == 0 && oface2 == 0 && oface3 == 0 && nface1 == 0 && nface2 == 0 && nface3 == 0) {
						continue;
					}

					xyz.set(quadr.axis3).mul(itr3).add(vit2).add(origin); // world position

					oahol = oaprov.get(xyz, quadr, oahol);
					oface1 *= oahol.f1;
					oface2 *= oahol.f2;
					oface3 *= oahol.f3;

					nahol = naprov.get(xyz, quadr, nahol);
					nface1 *= nahol.f1;
					nface2 *= nahol.f2;
					nface3 *= nahol.f3;

					// avoid duplicated edges
					if (isNotDuplicatedEdge(quadr, itr1, itr2, itr3)) {
						if (scout) {
							sucons.consume(xyz, 1, 1);
						} else if ((oahol.block != 0 || nahol.block != 0) && (oahol != nahol || oface1 != nface1 || oface2 != nface2 || oface3 != nface3)) {
							float ovoxelvisi = facesToVolumeValue(oface1, itr1, oface2, itr2, oface3, itr3) * oahol.block;
							float nvoxelvisi = facesToVolumeValue(nface1, itr1, nface2, itr2, nface3, itr3) * nahol.block;
							sucons.consume(xyz, ovoxelvisi, nvoxelvisi);
						}
					}

					// weights are it1, it2 and it3 except for those 3

					float f4w1 = max(0, itr1 - max(itr2, itr3) + 0.5f); // weight 1 for face 4
					float f5w2 = max(0, itr2 - max(itr1, itr3) + 0.5f); // weight 2 for face 5
					float f6w3 = max(0, itr3 - max(itr1, itr2) + 0.5f); // weight 3 for face 6

					float oface4, oface5, oface6;
					if (oahol.block == 0) {
						oface4 = oface5 = oface6 = 0;
					} else {
						oface4 = interpolate(oface1, f4w1, oface2, itr2, oface3, itr3) * oahol.f4;
						oface5 = interpolate(oface1, itr1, oface2, f5w2, oface3, itr3) * oahol.f5;
						oface6 = interpolate(oface1, itr1, oface2, itr2, oface3, f6w3) * oahol.f6;
					}
					ovbuffer[index + 0] = oface4;
					ovbuffer[index + size * 3 + 1] = oface5;
					ovbuffer[index + 3 + 2] = oface6;

					float nface4, nface5, nface6;
					if (nahol.block == 0) {
						nface4 = nface5 = nface6 = 0;
					} else {
						nface4 = interpolate(nface1, f4w1, nface2, itr2, nface3, itr3) * nahol.f4;
						nface5 = interpolate(nface1, itr1, nface2, f5w2, nface3, itr3) * nahol.f5;
						nface6 = interpolate(nface1, itr1, nface2, itr2, nface3, f6w3) * nahol.f6;
					}
					nvbuffer[index + 0] = nface4;
					nvbuffer[index + size * 3 + 1] = nface5;
					nvbuffer[index + 3 + 2] = nface6;
				}
			}
		}

	}

}
