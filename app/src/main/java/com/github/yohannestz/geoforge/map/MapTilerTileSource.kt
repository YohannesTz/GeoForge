package com.github.yohannestz.geoforge.map

import com.github.yohannestz.geoforge.utils.Constants
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex


class MapTilerTileSource(
    name: String?,
    zoomMinLevel: Int,
    zoomMaxLevel: Int,
    tileSizePixels: Int,
    imageFilenameEnding: String?,
    baseUrls: Array<String?>?,
    private val rootURL: String,
    private val tileSize: TileSize
) :
    OnlineTileSourceBase(
        name,
        zoomMinLevel,
        zoomMaxLevel,
        tileSizePixels,
        imageFilenameEnding,
        baseUrls
    ) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return rootURL + tileSize.value + "/" + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex) + "/" + MapTileIndex.getY(pMapTileIndex) +
                ".png" + "?key=${Constants.MAPTILER_API_KEY}"
    }
}