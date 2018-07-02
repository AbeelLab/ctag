package nl.tudelft.pl2.data.indexing

/**
  * The IndexChunkWriter interface declares functions
  * that may be used to store an [[IndexChunk]] on disk
  * in a certain format.
  *
  * Implementations of this interface should describe
  * in which ways [[IndexChunk]]s are stored on disk.
  *
  * @author Maaike Visser
  */
trait IndexChunkWriter {

  /**
    * Writes an [[IndexChunk]] to file as a string of bytes.
    *
    * @param indexChunk The [[IndexChunk]] to write.
    */
  def writeIndexChunk(indexChunk: IndexChunk): Unit
}
