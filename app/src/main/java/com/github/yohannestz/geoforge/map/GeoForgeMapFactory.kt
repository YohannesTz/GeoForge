package com.github.yohannestz.geoforge.map

import com.github.yohannestz.geoforge.utils.Constants

class GeoForgeMapFactory {
    companion object {
        fun createMap(type: MapType): MapTilerTileSource {
            return when (type) {
                MapType.MAPTILER_BASIC_V2 -> createBasicMap()
                MapType.MAPTILER_DATAVIS -> createDataVisMap()
                MapType.MAPTILER_OSM_URL -> createOSMmap()
                MapType.MAPTILER_DATAVIS_LIGHT -> createDataVisLightMap()
                MapType.MAPTILER_OUTDOOR_V2 -> createOutDoorVersionTwoMap()
            }
        }

        private fun createOutDoorVersionTwoMap(): MapTilerTileSource {
            return GeoForgeTileSource.builder()
                .setName("Basic V2")
                .setZoomLevels(0, 18)
                .setTileSize(256)
                .setImageFilenameEnding(".png")
                .setBaseUrls(arrayOf())
                .setRootURL(Constants.MAPTILER_BASIC_V2)
                .setTileSize(TileSize.SIZE_SMALL)
                .build()
        }

        private fun createDataVisMap(): MapTilerTileSource {
            return GeoForgeTileSource.builder()
                .setName("Data Vis")
                .setZoomLevels(0, 18)
                .setTileSize(256)
                .setImageFilenameEnding(".png")
                .setBaseUrls(arrayOf())
                .setRootURL(Constants.MAPTILER_DATAVIS)
                .setTileSize(TileSize.SIZE_SMALL)
                .build()
        }

        private fun createDataVisLightMap(): MapTilerTileSource {
            return GeoForgeTileSource.builder()
                .setName("OSM URL")
                .setZoomLevels(0, 18)
                .setTileSize(256)
                .setImageFilenameEnding(".png")
                .setBaseUrls(arrayOf())
                .setRootURL(Constants.MAPTILER_OSM_URL)
                .setTileSize(TileSize.SIZE_SMALL)
                .build()
        }

        private fun createOSMmap(): MapTilerTileSource {
            return GeoForgeTileSource.builder()
                .setName("Datavis Light")
                .setZoomLevels(0, 18)
                .setTileSize(256)
                .setImageFilenameEnding(".png")
                .setBaseUrls(arrayOf())
                .setRootURL(Constants.MAPTILER_DATAVIS_LIGHT)
                .setTileSize(TileSize.SIZE_SMALL)
                .build()
        }

        private fun createBasicMap(): MapTilerTileSource {
            return GeoForgeTileSource.builder()
                .setName("OutDoor V2")
                .setZoomLevels(0, 18)
                .setTileSize(256)
                .setImageFilenameEnding(".png")
                .setBaseUrls(arrayOf())
                .setRootURL(Constants.MAPTILER_OUTDOOR_V2)
                .setTileSize(TileSize.SIZE_SMALL)
                .build()
        }
    }
}