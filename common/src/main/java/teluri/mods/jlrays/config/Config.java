package teluri.mods.jlrays.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static teluri.mods.jlrays.JustLikeRays.MOD_ID;

@me.shedaniel.autoconfig.annotation.Config(name = MOD_ID)
public class Config implements ConfigData {
    public List<BlockLightLevel> blockLightLevels = new ArrayList<>(List.of(
            new BlockLightLevel("block.minecraft.glowstone", 10),
            new BlockLightLevel("block.minecraft.sea_lantern", 10),
            new BlockLightLevel("block.minecraft.shroomlight", 10)
    ));
}
