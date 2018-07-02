package nl.tudelft.pl2.data.loaders

import java.nio.file.{Files, Path}
import java.util.Observer

import javafx.application.Platform
import nl.tudelft.pl2.data.builders.{OneZoomBuilder, TwoZoomBuilder, ZeroZoomBuilder}
import nl.tudelft.pl2.data.caches.MasterCache
import nl.tudelft.pl2.data.storage.readers.{BookmarkReader, HeaderReader, HeatMapReader}
import nl.tudelft.pl2.representation.graph.LoadingState
import nl.tudelft.pl2.representation.ui.bookmark.BookmarkManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

/**
  * Loads a [[MasterCache]] associated with a certain .gfa file.
  * If the necessary compressed files and index files do not exist,
  * they are created.
  */
object MasterCacheLoader {

  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("MasterCacheLoader")

  /**
    * Loads a graph into a [[MasterCache]]. Creates any necessary files.
    *
    * @param gfaPath The path to the graph file.
    * @return A [[MasterCache]] for the graph file.
    */
  def loadGraph(gfaPath: Path, observer: Observer): MasterCache = {
    LOGGER.info("Loading the cache for " + gfaPath)

    val paths = new GraphPathCollection(gfaPath)

    if (paths.stream().anyMatch(p => !Files.exists(p))) {
      clearFiles(gfaPath)

      paths.forEach(p => LOGGER.debug("Building: {}.", p.getFileName))
      LOGGER.debug("Creating ZeroZoomBuilder...")

      ZeroZoomBuilder.buildFiles(paths, observer)
      OneZoomBuilder.buildFiles(paths, observer)
      TwoZoomBuilder.buildFiles(paths, observer)

      LOGGER.debug("Done building.")
    } else {
      //scalastyle:off null
      observer.update(null, LoadingState.FILES_PRESENT)
      //scalastyle:on null
    }

    //scalastyle:off null
    val headers = HeaderReader.readHeaders(paths.headerPath)
    observer.update(null, LoadingState.HEADERS_READ)
    val heatMap = HeatMapReader.read(paths.heatMapPath)
    observer.update(null, LoadingState.HEATMAP_READ)

    loadBookmarks(paths)

    val masterCache = MasterCache(
      headers,
      heatMap,
      Array(
        SubCacheLoader.loadSubCache(paths.zeroFilePath, paths.zeroIndexPath),
        SubCacheLoader.loadSubCache(paths.oneFilePath, paths.oneIndexPath),
        SubCacheLoader.loadSubCache(paths.twoFilePath, paths.twoIndexPath)
      ))

    observer.update(null, LoadingState.CACHES_LOADED)
    //scalastyle:on null
    masterCache
  }

  /**
    * Load the bookmarks belonging to this graph into the bookmark controller.
    *
    * @param paths The graphPathCollection needed to locate the bookmarks
    */
  def loadBookmarks(paths: GraphPathCollection): Unit = {
    try {
      Platform.runLater(() => {
        BookmarkManager.setBookmarks(BookmarkReader.readBookmarks(paths.bookmarkPath))
        BookmarkManager.createBookmarkWriter(paths.bookmarkPath)
      })
    } catch {
      case _: IllegalStateException =>
        BookmarkManager.setBookmarks(BookmarkReader.readBookmarks(paths.bookmarkPath))
        BookmarkManager.createBookmarkWriter(paths.bookmarkPath)
      case any: Throwable => throw any
    }
  }

  /**
    * Clears all the generated files for this graph.
    *
    * @param gfaPath The path to the graph whose
    *                generated files are cleared.
    */
  def clearFiles(gfaPath: Path): Unit =
    new GraphPathCollection(gfaPath).forEach(p => deleteIfExists(p))

  /**
    * Unloads the [[MasterCache]] by closing all its
    * subcaches.
    *
    * @param cache The [[MasterCache]] to unload.
    */
  def unload(cache: MasterCache): Unit = {
    if (cache != null) {
      cache.close()
    }
  }

  /**
    * Safely deletes a file if it exists.
    *
    * @param path The file to the path.
    */
  private def deleteIfExists(path: Path): Boolean = {
    try {
      Files.deleteIfExists(path)
    } catch {
      case any: Throwable =>
        LOGGER.error("Could not load {}.", path)
        LOGGER.error("Threw: ", any)
        false
    }
  }
}
