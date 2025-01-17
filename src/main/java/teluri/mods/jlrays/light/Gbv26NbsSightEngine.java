package teluri.mods.jlrays.light;

import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * handle logic of the 3D 26 neigbors GBV algorithm
 * 
 * @author RBLG
 * @since v0.0.1
 */
public class Gbv26NbsSightEngine {

	private static final int[] SIGNS = new int[] { 1, -1 };
	// positive axis
	private static final Vector3i X = new Vector3i(1, 0, 0);
	private static final Vector3i Y = new Vector3i(0, 1, 0);
	private static final Vector3i Z = new Vector3i(0, 0, 1);
	// negative axis
	private static final Vector3i NX = new Vector3i(-1, 0, 0);
	private static final Vector3i NY = new Vector3i(0, -1, 0);
	private static final Vector3i NZ = new Vector3i(0, 0, -1);
	// unsigned cones
	private static final UCone XYZ = new UCone(X, Y, Z, 1, 0); // TODO move to enum?
	private static final UCone XZY = new UCone(X, Z, Y, 0, 1);
	private static final UCone YXZ = new UCone(Y, X, Z, 0, 0);
	private static final UCone YZX = new UCone(Y, Z, X, 0, 1);
	private static final UCone ZXY = new UCone(Z, X, Y, 1, 1);
	private static final UCone ZYX = new UCone(Z, Y, X, 1, 0);
	private static final UCone[] UCONES = new UCone[] { XYZ, XZY, YXZ, YZX, ZXY, ZYX, };
	// all 48 signed cones
	private static final Cone[] CONES = genCones();

	/**
	 * generates data for the 48 different cones
	 */
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

	// private static final Offset ZERO = new Offset(0, 0, 0);
	/////////////////////////////////////////////////////////////////////

	/**
	 * trace all 48 cones around a source
	 * 
	 * @see Gbv26NbsSightEngine.TraceCone
	 */
	public static void traceAllCones(Vector3i source, int range, IAlphaProvider opmap, ISightConsumer lmap) {
		// ExecutorService pool = Executors.newFixedThreadPool(48);
		for (Cone cone : CONES) {// 48 times
			traceCone(source, range, cone, opmap, lmap);
		}
	}

	/**
	 * trace a 1-6 cones with an offset. this offseted cone correspond to the changes of visibility values caused by changes of a single block
	 * 
	 * @param origin: position of the changed block
	 * @param offset: signed vector from the source to origin
	 * @param range:  max range to search light sources in
	 * @param aprov:  block alpha value provider
	 * @param lcons:  visibility consumer
	 */
	@Deprecated
	public static void traceChangeCone(Vector3i origin, Vector3i offset, int range, IAlphaProvider aprov, ISightConsumer vcons) {
		Vector3i v1 = offset.x < 0 ? NX : X;
		Vector3i v2 = offset.y < 0 ? NY : Y;
		Vector3i v3 = offset.z < 0 ? NZ : Z;
		int o1 = Math.abs(offset.x);
		int o2 = Math.abs(offset.y);
		int o3 = Math.abs(offset.z);
		int order1 = 1;
		int order2 = 2;
		int order3 = 3;

		// ordering high to low
		if (o1 < o2) { // a = a ^ b ^ (b = a) == swap(a,b)
			o1 = o1 ^ o2 ^ (o2 = o1);
			Vector3i vtmp = v1;
			v1 = v2;
			v2 = vtmp;
		}
		if (o2 < o3) {
			o2 = o2 ^ o3 ^ (o3 = o2);
			Vector3i vtmp = v2;
			v2 = v3;
			v3 = vtmp;
		}
		if (o1 < o2) {
			o1 = o1 ^ o2 ^ (o2 = o1);
			Vector3i vtmp = v1;
			v1 = v2;
			v2 = vtmp;
		}
		order2 = o1 == o2 ? order1 : order2;
		order3 = o2 == o3 ? order2 : order3;

		Vector3i oforder = new Vector3i(v1).absolute().mul(order1).add(new Vector3i(v2).absolute().mul(order2)).add(new Vector3i(v3).absolute().mul(order3));
		Offset offset2 = new Offset(o1, o2, o3);
		for (Cone cone : CONES) {
			if (Math.signum(offset.x) == -cone.xyz.x || Math.signum(offset.y) == -cone.xyz.y || Math.signum(offset.z) == -cone.xyz.z) {
				continue;
			}
			if (cone.order.x < oforder.x || cone.order.y < oforder.y || cone.order.z < oforder.z) {
				continue;
			}
			traceConeWithOffset(origin, offset2, range, cone, aprov, vcons);
		}
	}

	/**
	 * 
	 * @param source
	 * @param offset
	 * @param range  max range for light updates
	 * @param oaprov old alpha provider
	 * @param naprov new alpha provider
	 * @param vcons  visibility data consumer
	 */
	public static void traceChangeCone2(Vector3i source, Vector3i offset, int range, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer3 vcons) {
		Vector3i vtmp = new Vector3i();
		for (Cone cone : CONES) {
			int comp1 = sum(vtmp.set(offset).mul(cone.axis1));
			int comp2 = sum(vtmp.set(offset).mul(cone.axis2));
			int comp3 = sum(vtmp.set(offset).mul(cone.axis3));
			if (0 <= comp1 && 0 <= comp2 && 0 <= comp3 && comp2 <= comp1 && comp3 <= comp2) {
				traceConeWithChange2(source, range, cone, oaprov, naprov, vcons);
			}
		}
	}

	public static void traceCone(Vector3i origin, int range, Cone cone, IAlphaProvider aprov, ISightConsumer vcons) {
		final Vector3i vit1 = new Vector3i(); // this is a nature friendly place here, we recycle our objects
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		if (range <= 0) {
			return;
		}

		int size = range + 2;

		// store the visibility values
		float[] vbuffer = new float[size * size];
		vbuffer[0] = 1; // the source
		vbuffer[size] = 1; // the source
		vbuffer[size + 1] = 1; // the source

		// iterate from source to range (it1)
		for (int it1 = 1; it1 <= range; it1++) { // start at 1 to skip source
			vit1.set(cone.axis1).mul(it1);
			boolean nonzero = false;
			float totinv = 1f / (it1 + 1);
			for (int it2 = it1; 0 <= it2; it2--) {// start from the end to handle vbuffer values turnover easily
				vit2.set(cone.axis2).mul(it2).add(vit1);
				for (int it3 = it2; 0 <= it3; it3--) { // same than it2

					int index = (it2 * size) + it3;
					float visi = vbuffer[index];
					if (visi <= 0) {
						continue;
					}
					xyz.set(cone.axis3).mul(it3).add(vit2).add(origin); // world position

					float alpha = aprov.get(xyz);

					// skip if doesnt have to output the edge (false==should output the edge)
					if (!((it1 == it2 && cone.edge1) || (it2 == it3 && cone.edge2) || (it2 == 0 && cone.qedge2) || (it3 == 0 && cone.qedge3))) {
						// light effects and output
						double dist = 1 + Vector3f.lengthSquared(it1 * 0.3f, it2 * 0.3f, it3 * 0.3f);
						vcons.consume(xyz, visi, alpha, dist);
					}

					if (alpha == 0) {
						vbuffer[index] = 0;// below zero values mean shadows are larger around edges
						continue;
					}
					nonzero = true;

					// weights
					int w1 = it1 + 1 - it2;
					int w2 = it2 + 1 - it3;
					int w3 = it3 + 1;

					visi *= totinv;
					// apply to next neigbors
					vbuffer[index] = visi * w1;
					vbuffer[index + size] += visi * w2;
					vbuffer[index + size + 1] += visi * w3;

				}
			}
			if (!nonzero) { // if no block was visible on the whole current plane, it mean none will be later
				return;
			}
		}

	}

	/**
	 * compute visibility (and light received) over the cone of changes caused by an opacity change of one block over one source
	 * 
	 * @param origin:   origin of the cone
	 * @param offset:   offset between the origin and the source, components are sorted and absolute
	 * @param range:    how far should visibility be computed (to be estimated based on emit)
	 * @param v1,v2,v3: axises vectors
	 * @param opmap:    opacity provider
	 * @param vcons     visibility/light value consumer
	 */
	@Deprecated
	public static void traceConeWithOffset(Vector3i origin, Offset offset, int range, Cone cone, IAlphaProvider aprov, ISightConsumer vcons) {
		final Vector3i vit1 = new Vector3i(); // this is a nature friendly place here, we recycle our objects
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		range -= offset.o1;
		if (range <= 0) {
			return;
		}

		int size = range + 2;

		// weights adjustments to reflect the source position rather than the origin of the cone
		int of1 = offset.o1 - offset.o2;
		int of2 = offset.o2 - offset.o3;
		int of3 = offset.o3;

		int layer1tot = offset.o1 + 1;
		// store the visibility values
		float[] vbuffer = new float[size * size];
		vbuffer[0] = (of1 + 1) / layer1tot;
		vbuffer[size] = (of2 + 1) / layer1tot;
		vbuffer[size + 1] = (of3 + 1) / layer1tot;

		// iterate from source to range (it1)
		for (int it1 = 1; it1 <= range; it1++) { // start at 1 to skip source
			vit1.set(cone.axis1).mul(it1);
			boolean nonzero = false;
			float totinv = 1f / (it1 + offset.o1 + 1);
			for (int it2 = it1; 0 <= it2; it2--) {// start from the end to handle vbuffer values turnover easily
				vit2.set(cone.axis2).mul(it2).add(vit1);
				for (int it3 = it2; 0 <= it3; it3--) { // same than it2

					int index = (it2 * size) + it3;
					float visi = vbuffer[index];
					if (visi <= 0) {
						continue;
					}

					xyz.set(cone.axis3).mul(it3).add(vit2).add(origin); // world position

					float alpha = aprov.get(xyz);

					// skip if doesnt have to output the edge (false==should output the edge)
					if (!((it1 + of1 == it2 && cone.edge1) || (it2 + of2 == it3 && cone.edge2) || (it2 + offset.o2 == 0 && cone.qedge2) || (it3 + offset.o3 == 0 && cone.qedge3))) {
						// light effects and output
						double dist = 1 + Vector3f.lengthSquared((it1 + offset.o1) * 0.3f, (it2 + offset.o2) * 0.3f, (it3 + offset.o3) * 0.3f);
						vcons.consume(xyz, visi, alpha, dist);
					}

					if (alpha == 0) {
						vbuffer[index] = 0;// below zero values mean shadows are larger around edges
						continue;
					}
					nonzero = true;

					// weights
					int w1 = it1 + 1 - it2 + of1;
					int w2 = it2 + 1 - it3 + of2;
					int w3 = it3 + 1 + of3;

					visi *= totinv;
					// apply to next neigbors
					vbuffer[index] = visi * w1;
					vbuffer[index + size] += visi * w2;
					vbuffer[index + size + 1] += visi * w3;

				}
			}
			if (!nonzero) { // if no block was visible on the whole current plane, it mean none will be later
				return;
			}
		}

	}

	@Deprecated
	public static void traceConeWithChange(Vector3i origin, int range, Cone cone, IAlphaProvider aprov, IChangeAlphaProvider cprov, ISightUpdateConsumer2 vcons) {
		final Vector3i vit1 = new Vector3i(); // this is a nature friendly place here, we recycle our objects
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		if (range <= 0) {
			return;
		}

		int size = range + 2;

		// store the visibility values
		float[] vbuffer = new float[size * size];
		float[] vbuffer2 = new float[size * size];
		vbuffer[0] = 1;
		vbuffer[size] = 1;
		vbuffer[size + 1] = 1;

		// iterate from source to range (it1)
		for (int it1 = 1; it1 <= range; it1++) { // start at 1 to skip source
			vit1.set(cone.axis1).mul(it1);
			boolean nonzero = false;
			float totinv = 1f / (it1 + 1);
			for (int it2 = it1; 0 <= it2; it2--) {// start from the end to handle vbuffer values turnover easily
				vit2.set(cone.axis2).mul(it2).add(vit1);
				for (int it3 = it2; 0 <= it3; it3--) { // same than it2
					int index = (it2 * size) + it3;
					float visi = vbuffer[index];
					if (visi <= 0) {
						continue;
					}

					xyz.set(cone.axis3).mul(it3).add(vit2).add(origin); // world position

					float alpha = aprov.get(xyz);

					float changevisi;
					float changealpha = cprov.get(xyz);
					if (changealpha == 0) {
						changevisi = vbuffer2[index];
					} else {
						changevisi = visi * changealpha;
					}

					// skip if doesnt have to output the edge (false==should output the edge)
					if (!((it1 == it2 && cone.edge1) || (it2 == it3 && cone.edge2) || (it2 == 0 && cone.qedge2) || (it3 == 0 && cone.qedge3))) {
						// light effects and output
						double dist = 1 + Vector3f.lengthSquared(it1 * 0.3f, it2 * 0.3f, it3 * 0.3f);
						vcons.consume(xyz, visi, changevisi, alpha, dist);
					}

					if (alpha == 0 && changevisi == 0) {
						vbuffer[index] = 0;
						vbuffer2[index] = 0;
						continue;
					}
					nonzero = true;

					// weights
					int w1 = it1 + 1 - it2;
					int w2 = it2 + 1 - it3;
					int w3 = it3 + 1;

					changevisi *= totinv;

					// apply to next neigbors
					vbuffer2[index] = changevisi * w1;
					vbuffer2[index + size] += changevisi * w2;
					vbuffer2[index + size + 1] += changevisi * w3;

					if (alpha == 0) {
						vbuffer[index] = 0;
						continue;
					}

					visi *= totinv;
					// apply to next neigbors
					vbuffer[index] = visi * w1;
					vbuffer[index + size] += visi * w2;
					vbuffer[index + size + 1] += visi * w3;

				}
			}
			if (!nonzero) { // if no block was visible on the whole current plane, it mean none will be later
				return;
			}
		}

	}

	public static void traceConeWithChange2(Vector3i origin, int range, Cone cone, IAlphaProvider oaprov, IAlphaProvider naprov, ISightUpdateConsumer3 vcons) {
		final Vector3i vit1 = new Vector3i(); // this is a nature friendly place here, we recycle our objects
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		if (range <= 0) {
			return;
		}

		int size = range + 2;

		// store the visibility values
		float[] ovbuffer = new float[size * size];
		float[] nvbuffer = new float[size * size];
		nvbuffer[0] = ovbuffer[0] = 1;
		nvbuffer[size] = ovbuffer[size] = 1;
		nvbuffer[size + 1] = ovbuffer[size + 1] = 1;

		// iterate from source to range (it1)
		for (int it1 = 1; it1 <= range; it1++) { // start at 1 to skip source
			vit1.set(cone.axis1).mul(it1);
			boolean nonzero = false;
			float totinv = 1f / (it1 + 1);
			for (int it2 = it1; 0 <= it2; it2--) {// start from the end to handle vbuffer values turnover easily
				vit2.set(cone.axis2).mul(it2).add(vit1);
				for (int it3 = it2; 0 <= it3; it3--) { // same than it2
					int index = (it2 * size) + it3;
					float ovisi = ovbuffer[index];
					float nvisi = nvbuffer[index];
					if (ovisi <= 0 && nvisi <= 0) {
						continue;
					}

					xyz.set(cone.axis3).mul(it3).add(vit2).add(origin); // world position

					float oalpha = oaprov.get(xyz);
					float nalpha = naprov.get(xyz);

					// skip if doesnt have to output the edge (false==should output the edge)
					if (!((it1 == it2 && cone.edge1) || (it2 == it3 && cone.edge2) || (it2 == 0 && cone.qedge2) || (it3 == 0 && cone.qedge3))) {
						// light effects and output
						if (oalpha != 0 || nalpha != 0) {
							double dist = 1 + Vector3f.lengthSquared(it1 * 0.3f, it2 * 0.3f, it3 * 0.3f);
							vcons.consume(xyz, ovisi * oalpha, nvisi * nalpha, dist);
						}
					}

					// weights
					int w1 = it1 + 1 - it2;
					int w2 = it2 + 1 - it3;
					int w3 = it3 + 1;

					if (oalpha == 0) {
						ovbuffer[index] = 0;
					} else {
						nonzero = true;

						ovisi *= totinv * oalpha;
						ovbuffer[index] = ovisi * w1;
						ovbuffer[index + size] += ovisi * w2;
						ovbuffer[index + size + 1] += ovisi * w3;
					}

					if (nalpha == 0) {
						nvbuffer[index] = 0;
					} else {
						nonzero = true;

						nvisi *= totinv * nalpha;
						nvbuffer[index] = nvisi * w1;
						nvbuffer[index + size] += nvisi * w2;
						nvbuffer[index + size + 1] += nvisi * w3;
					}
				}
			}
			if (!nonzero) { // if no block was visible on the whole current plane, it mean none will be later
				return;
			}
		}

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@FunctionalInterface
	public static interface IAlphaProvider { // or opacity, but alpha is shorter. 0=opaque, 1=air
		float get(Vector3i xyz);
	}

	@FunctionalInterface
	public static interface IChangeAlphaProvider { // or opacity, but alpha is shorter. 0=opaque, 1=air
		float get(Vector3i xyz);
	}

	@FunctionalInterface
	public static interface ISightConsumer { // or visibility, but sight is shorter
		void consume(Vector3i xyz, float visi, float alpha, double distance);
	}

	@FunctionalInterface
	public static interface ISightUpdateConsumer { // or visibility, but sight is shorter
		void consume(Vector3i xyz, float visi, float coneratio, float alpha, double distance);
	}

	@FunctionalInterface
	public static interface ISightUpdateConsumer2 { // or visibility, but sight is shorter
		void consume(Vector3i xyz, float visi, float changevisi, float alpha, double distance);
	}

	@FunctionalInterface
	public static interface ISightUpdateConsumer3 { // or visibility, but sight is shorter
		void consume(Vector3i xyz, float ovisi, float nvisi, double distance);
	}

	private static record Cone(// signed iteration cone
			Vector3i axis1, Vector3i axis2, Vector3i axis3, //
			boolean edge1, boolean edge2, // diagonal edges priorities
			boolean qedge2, boolean qedge3, // quadrant edges priorities
			Vector3i xyz, Vector3i order//
	) {
		public Cone(Vector3i axis1, Vector3i axis2, Vector3i axis3, boolean edge1, boolean edge2, boolean qedge2, boolean qedge3) {
			this(axis1, axis2, axis3, edge1, edge2, qedge2, qedge3, new Vector3i(axis1).add(axis2).add(axis3), //
					new Vector3i(axis1).absolute().add(new Vector3i(axis2).absolute().mul(2)).add(new Vector3i(axis3).absolute().mul(3))//
			);
		}

	}

	private static record UCone(Vector3i axis1, Vector3i axis2, Vector3i axis3, boolean edge1, boolean edge2) { // unsigned cone

		public UCone(Vector3i naxis1, Vector3i naxis2, Vector3i naxis3, int nedge1, int nedge2) {
			this(naxis1, naxis2, naxis3, nedge1 == 0, nedge2 == 0);
		}
	}

	private static record Offset(int o1, int o2, int o3) {} // not stored as a vector so that 1,2,3 doesnt get mixed with x,y,z

	public static int sum(Vector3i vec) {
		return vec.x + vec.y + vec.z;
	}

}
