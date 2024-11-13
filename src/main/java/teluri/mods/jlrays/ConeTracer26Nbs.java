package teluri.mods.jlrays;

import org.joml.Vector3i;

public class ConeTracer26Nbs {

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
	private static final UCone XYZ = new UCone(X, Y, Z, 1, 0); // TODO move to an enum?
	private static final UCone XZY = new UCone(X, Z, Y, 0, 1); // TODO add a (1,2,3) sig for order?
	private static final UCone YXZ = new UCone(Y, X, Z, 0, 0);
	private static final UCone YZX = new UCone(Y, Z, X, 0, 1);
	private static final UCone ZXY = new UCone(Z, X, Y, 1, 1);
	private static final UCone ZYX = new UCone(Z, Y, X, 1, 0);
	private static final UCone[] UCONES = new UCone[] { XYZ, XZY, YXZ, YZX, ZXY, ZYX, };
	// all 48 signed cones
	private static final Cone[] CONES = genCones();

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

	private static final Offset ZERO = new Offset(0, 0, 0);
	/////////////////////////////////////////////////////////////////////

	/**
	 * trace all 48 cones around a source
	 * 
	 * @see ConeTracer26Nbs.TraceCone
	 */
	public static void TraceAllCones(Vector3i source, int range, IAlphaProvider opmap, ISightConsumer lmap) {
		for (Cone cone : CONES) {// 48 times
			TraceCone(source, ZERO, range, cone, opmap, lmap);
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
	public static void TraceChangeCone(Vector3i origin, Vector3i offset, int range, IAlphaProvider aprov, ISightConsumer vcons) {
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
			TraceCone(origin, offset2, range, cone, aprov, vcons);
		}
	}

	/**
	 * compute visibility (and light received) over one cone
	 * 
	 * @param origin:   origin of the cone
	 * @param offset:   offset between the origin and the source, components are sorted and absolute
	 * @param range:    how far should visibility be computed (to be estimated based on emit)
	 * @param v1,v2,v3: axises vectors
	 * @param opmap:    opacity provider
	 * @param vcons     visibility/light value consumer
	 */
	public static void TraceCone(Vector3i origin, Offset offset, int range, Cone cone, IAlphaProvider aprov, ISightConsumer vcons) {
		final Vector3i vit1 = new Vector3i(); // this is a nature friendly place here, we recycle our objects
		final Vector3i vit2 = new Vector3i();
		final Vector3i xyz = new Vector3i();
		range -= offset.o1;
		if (range <= 0) {
			return;
		}

		int size = range + 2;

		// store the visibility values
		float[] vbuffer = new float[size * size];
		vbuffer[0] = 1; // the source
		vbuffer[size] = 1; // the source
		vbuffer[size + 1] = 1; // the source

		// weights adjustments to reflect the source position rather than the origin of the cone
		int of1 = offset.o1 - offset.o2;
		int of2 = offset.o2 - offset.o3;
		int of3 = offset.o3;

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
					visi = Math.clamp(visi, 0, 1);
					xyz.set(cone.axis3).mul(it3).add(vit2).add(origin); // world position

					float alpha = aprov.get(xyz.x, xyz.y, xyz.z);

					// skip if doesnt have to output the edge (false==should output the edge)
					if (!((it1 + of1 == it2 && cone.edge1) || (it2 + of2 == it3 && cone.edge2) || (it2 + offset.o2 == 0 && cone.qedge2) || (it3 + offset.o3 == 0 && cone.qedge3))) {
						// light effects and output
						double dist = Vector3i.length(it1 + offset.o1, it2 + offset.o2, it3 + offset.o3);
						vcons.consumer(xyz.x, xyz.y, xyz.z, visi, alpha, dist);
					}

					if (alpha == 0) {
						vbuffer[index] = -0.0f;// below zero values mean shadows are larger around edges
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

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@FunctionalInterface
	public static interface IAlphaProvider { // or opacity, but alpha is shorter. 0=opaque, 1=air
		float get(int x, int y, int z);
	}

	@FunctionalInterface
	public static interface ISightConsumer { // or visibility, but sight is shorter
		void consumer(int x, int y, int z, float visi, float alpha, double distance);
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

}
