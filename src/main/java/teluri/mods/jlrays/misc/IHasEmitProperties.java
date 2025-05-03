package teluri.mods.jlrays.misc;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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

	public static class EmitProperties {
		public static EmitProperties DEFAULT = new EmitProperties();

		public Vector3f offset = new Vector3f(0); // -0.5 to 0.5 range
		public Vector3f radius = new Vector3f(0.5f); // offset+radius should be in the -0.5 to 0.5 range
		public float falloff = 0.01f; // how fast light fall off
	}
}
