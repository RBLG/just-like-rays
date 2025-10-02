package teluri.mods.jlrays.util;

import java.util.function.Consumer;

public class MiscHelper {

	public static <T> T using(T obj, Consumer<T> cons) {
		cons.accept(obj);
		return obj;
	}

}
