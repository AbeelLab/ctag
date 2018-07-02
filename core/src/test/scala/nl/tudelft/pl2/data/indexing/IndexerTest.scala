package nl.tudelft.pl2.data.indexing

import java.io.File
import java.nio.file.{Path, Paths}

import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSuite}
//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import
import org.scalatest.junit.JUnitRunner

/**
  * Test class for the IndexManager.
  */
@RunWith(classOf[JUnitRunner])
class IndexerTest extends FunSuite with BeforeAndAfter {
  /**
    * Various values for testing.
    */

  private var temp: File = _
  private val SEG_LEN = 10
  private var TEMP_IDXPATH: Path = _
  private var indexer: Indexer = _

  before {
    temp = File.createTempFile("temp", ".idx")
    TEMP_IDXPATH = Paths.get(temp.getAbsolutePath)
    indexer = new Indexer(TEMP_IDXPATH)
  }

  test("Adding twice the maximum number of segments to an empty Index " +
    "should add two chunks to the index tree") {
    for (i <- 0 until 2 * indexer.MAX_NODES) {
      indexer.indexNode(i, SEG_LEN, i)
    }
    indexer.flush()
    temp.length() should be {
      IndexChunk.BYTES_PER_CHUNK * 2
    }
  }


  test("Add fewer segments than needed to fill a chunk and store the index") {
    for (i <- 1 to 10) {
      indexer.indexNode(i, SEG_LEN, i)
    }
    indexer.flush()
    temp.length() should be {
      IndexChunk.BYTES_PER_CHUNK
    }
  }

}
