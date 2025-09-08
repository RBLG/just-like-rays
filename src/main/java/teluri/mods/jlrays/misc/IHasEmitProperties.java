package teluri.mods.jlrays.misc;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * provide EmitProperties to be used on BlockState to provide more properties for light sources
 * 
 * @author RBLG
 * @since v0.2.0
 */
public interface IHasEmitProperties {

	void setLightBlock(int value);

	void setLightEmit(int value);

	EmitProperties initEmitProperties();

	@Nullable
	EmitProperties getEmitPropertiesNullable();

	default EmitProperties getEmitProperties() {
		EmitProperties prop = this.getEmitPropertiesNullable();
		return prop == null ? EmitProperties.DEFAULT : prop;
	}

	default boolean hasEmitProperties() {
		return this.getEmitPropertiesNullable() == null;
	}

	/**
	 * optionnal properties for light sources
	 */
	public static class EmitProperties {
		public static EmitProperties DEFAULT = new EmitProperties();

		public Vector3f offset = new Vector3f(0); // -0.5 to 0.5 range
		public Vector3f radius = new Vector3f(0.5f); // offset+radius should be in the -0.5 to 0.5 range
		public float emitScale = 1; // how fast light fall off //TODO handle

		public boolean isValid() {
			if (radius.x < 0 || radius.y < 0 || radius.z < 0) {
				return false;
			}
			Vector3f tmp = new Vector3f();
			tmp.set(offset).add(radius).absolute();
			boolean aaa = tmp.x <= 0.5f && tmp.y <= 0.5f && tmp.z <= 0.5f;
			tmp.set(offset).sub(radius).absolute();
			boolean bbb = tmp.x <= 0.5f && tmp.y <= 0.5f && tmp.z <= 0.5f;
			return aaa && bbb;
		}

		/**
		 * enforce that offset
		 */
		public void enforceValidity() {
			Vector3f tmp = new Vector3f();
			// enforce that radius is positive
			radius.max(tmp.set(0));
			// enforce that radius fit the -0.5..0.5 bound
			radius.min(tmp.set(0.5f));
			// enforce superior bound
			offset.min(tmp.set(0.5f).sub(radius));
			// enforce inferior bound
			offset.max(tmp.set(-0.5f).add(radius));
		}
	}
}
