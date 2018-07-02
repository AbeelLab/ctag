package nl.tudelft.pl2.data.indexing

import java.nio.file.Path

/**
  * The IndexReader interface declares the functions
  * that may be used to read in an index file with
  * a certain file format and parse it to an [[Index]]
  * object in memory.
  *
  * Implementations of this interface should define
  * behaviour that describes how index files with
  * a specific format should be read in.
  *
  * @author Maaike Visser
  */
trait IndexReader {

  /**
    * Loads an [[Index]] into memory from the specified file.
    *
    * @param indexPath The [[Path]] where the index file can
    *                  be found.
    * @return The new [[Index]].
    */
  def loadIndex(indexPath: Path): Index
}
