package com.github.yohannestz.geoforge.map

import android.util.Log

class GeoForgeTileSource private constructor(
    private val name: String?,
    private val zoomMinLevel: Int,
    private val zoomMaxLevel: Int,
    private val tileSizePixels: Int,
    private val imageFilenameEnding: String?,
    private val baseUrls: Array<String?>?,
    private val rootURL: String,
    private val tileSize: TileSize
) {
    companion object {
        fun builder(): GeoForgeBuilder {
            return GeoForgeBuilder()
        }
    }

    fun build(): MapTilerTileSource {
        return MapTilerTileSource(
            name,
            zoomMinLevel,
            zoomMaxLevel,
            tileSizePixels,
            imageFilenameEnding,
            baseUrls,
            rootURL,
            tileSize
        )
    }

    class GeoForgeBuilder {
        private var name: String? = null
        private var zoomMinLevel: Int = 0
        private var zoomMaxLevel: Int = 0
        private var tileSizePixels: Int = 0
        private var imageFilenameEnding: String? = null
        private var baseUrls: Array<String?>? = null
        private lateinit var rootURL: String
        private lateinit var tileSize: TileSize

        fun setName(name: String?): GeoForgeBuilder {
            this.name = name
            return this
        }

        fun setZoomLevels(minLevel: Int, maxLevel: Int): GeoForgeBuilder {
            this.zoomMinLevel = minLevel
            this.zoomMaxLevel = maxLevel
            return this
        }

        fun setTileSize(tileSizePixels: Int): GeoForgeBuilder {
            this.tileSizePixels = tileSizePixels
            return this
        }

        fun setImageFilenameEnding(imageFilenameEnding: String?): GeoForgeBuilder {
            this.imageFilenameEnding = imageFilenameEnding
            return this
        }

        fun setBaseUrls(baseUrls: Array<String?>?): GeoForgeBuilder {
            this.baseUrls = baseUrls
            return this
        }

        fun setRootURL(rootURL: String): GeoForgeBuilder {
            this.rootURL = rootURL
            return this
        }

        fun setTileSize(tileSize: TileSize): GeoForgeBuilder {
            this.tileSize = tileSize
            return this
        }

        fun build(): MapTilerTileSource {
            return GeoForgeTileSource(
                name,
                zoomMinLevel,
                zoomMaxLevel,
                tileSizePixels,
                imageFilenameEnding,
                baseUrls,
                rootURL,
                tileSize
            ).build()
        }
    }
}