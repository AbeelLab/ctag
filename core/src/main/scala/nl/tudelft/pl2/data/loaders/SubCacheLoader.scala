package nl.tudelft.pl2.data.loaders

import java.nio.file.Path
import java.util.logging.Logger

import nl.tudelft.pl2.data.caches.SubCache
import nl.tudelft.pl2.data.indexing.BytesIndexReader
import nl.tudelft.pl2.data.storage.readers.CtagReader
import org.apache.logging.log4j.LogManager

/**
  * Loads the zero zoom level cache.
  */
object SubCacheLoader {
  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("SubCacheLoader")

  /**
    * Loads the [[SubCache]].
    *
    * @param filePath  The path to the compressed file.
    * @param indexPath The path to the index file.
    * @return The [[SubCache]].
    */
  def loadSubCache(filePath: Path, indexPath: Path): SubCache = {
    new SubCache(BytesIndexReader.loadIndex(indexPath), new CtagReader(filePath))
  }
}
