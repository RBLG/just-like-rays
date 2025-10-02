package teluri.mods.jlrays.light.sight.misc;

import org.joml.Vector3i;

/**
 * holds alpha (transparancy/opacity) values for each face of a block, plus the alpha value of the volume of the block
 * 
 * @author RBLG
 * @since v0.0.7
 */
public class AlphaHolder {
	public float block, f1, f2, f3, f4, f5, f6;

	public void setAll(float val) {
		f1 = f2 = f3 = f4 = f5 = f6 = block = val;
	}

	public void setAll(AlphaHolder hol) {
		block = hol.block;
		f1 = hol.f1;
		f2 = hol.f2;
		f3 = hol.f3;
		f4 = hol.f4;
		f5 = hol.f5;
		f6 = hol.f6;
	}

	@FunctionalInterface
	public interface IAlphaProvider {
		AlphaHolder getAlphas(Vector3i xyz, Vector3i source, Quadrant quadr, AlphaHolder hol);
	}

	@FunctionalInterface
	public interface IAlphaChangeProvider {
		void getAlphas(Vector3i xyz, Vector3i source, Quadrant quadr, AlphaHolder ohol, AlphaHolder nhol);
	}
}