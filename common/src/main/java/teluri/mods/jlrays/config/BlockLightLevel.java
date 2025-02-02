package teluri.mods.jlrays.config;

public class BlockLightLevel {
    public String blockId;

    public int lightLevel;

    public BlockLightLevel() {}

    public BlockLightLevel(String blockId, int lightLevel) {
        this.blockId = blockId;
        this.lightLevel = lightLevel;
    }
}