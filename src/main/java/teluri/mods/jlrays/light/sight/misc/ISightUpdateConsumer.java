package teluri.mods.jlrays.light.sight.misc;

import org.joml.Vector3i;

@FunctionalInterface
public interface ISightUpdateConsumer {
	void consume(Vector3i xyz, float ovisi, float nvisi);
}