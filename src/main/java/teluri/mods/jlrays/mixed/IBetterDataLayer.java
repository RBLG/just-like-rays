package teluri.mods.jlrays.mixed;

public interface IBetterDataLayer {
	public static int BYTE_SIZED = 4096;

	void setSize(int nsize);

	int getSize();

	default void setByteSized() {
		setSize(BYTE_SIZED);
	}
	
	default boolean isByteSized() {
		return getSize() == BYTE_SIZED;
	}

}
