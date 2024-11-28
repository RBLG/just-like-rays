package teluri.mods.jlrays;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import teluri.mods.jlrays.mixed.IBetterDataLayer;

public class JlrLightSectionStorage extends LayerLightSectionStorage<JlrLightSectionStorage.JlrDataLayerStorageMap> {
	protected JlrLightSectionStorage(LightChunkGetter chunkSource) {
		super(LightLayer.BLOCK, chunkSource, new JlrLightSectionStorage.JlrDataLayerStorageMap(new Long2ObjectOpenHashMap<>()));
	}

	@Override
	protected int getLightValue(long levelPos) {
		long l = SectionPos.blockToSection(levelPos);
		DataLayer dataLayer = this.getDataLayer(l, false);
		return dataLayer == null ? 0
				: dataLayer.get(//
						SectionPos.sectionRelative(BlockPos.getX(levelPos)), //
						SectionPos.sectionRelative(BlockPos.getY(levelPos)), //
						SectionPos.sectionRelative(BlockPos.getZ(levelPos)) //
				);
	}

	@Override
	protected DataLayer createDataLayer(long sectionPos) {
		DataLayer dataLayer = super.createDataLayer(sectionPos);
		JustLikeRays.LOGGER.info("datalayer set to byte sized");
		((IBetterDataLayer) dataLayer).setByteSized();
		return dataLayer;
	}

	public static final class JlrDataLayerStorageMap extends DataLayerStorageMap<JlrLightSectionStorage.JlrDataLayerStorageMap> {
		public JlrDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> long2ObjectOpenHashMap) {
			super(long2ObjectOpenHashMap);
		}

		public JlrLightSectionStorage.JlrDataLayerStorageMap copy() {
			return new JlrLightSectionStorage.JlrDataLayerStorageMap(this.map.clone());
		}
	}

	public void isValid(long blockPos) {
		long sectionPos = SectionPos.blockToSection(blockPos);
		DataLayer layer = getDataLayer(sectionPos, false);
		if (layer == null) {
			return;
		}
		if (!((IBetterDataLayer) layer).isByteSized()) {
			JustLikeRays.LOGGER.error("datalayer isnt byte sized during validity check");
		}
	}
}
