package teluri.mods.jlrays.util;

/**
 * @author RBLG
 * @since v0.0.7
 * abstraction of the iteration over a list of position
 */
@FunctionalInterface
public interface IPositionIterator {
	void forEach(IPositionIteratorStep cons);
	
	@FunctionalInterface
	public static interface IPositionIteratorStep {
		boolean consume(int x, int y, int z);
	}
}