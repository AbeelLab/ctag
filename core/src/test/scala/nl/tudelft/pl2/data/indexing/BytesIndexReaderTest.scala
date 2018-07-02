package nl.tudelft.pl2.data.indexing

import java.io.File
import java.nio.file.{Path, Paths}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class BytesIndexReaderTest extends FunSuite with BeforeAndAfter {
  private val SEG_LEN = 10

  private var temp: File = _
  private var TEMP_IDXPATH: Path = _
  private var indexer: Indexer = _


  before {
    temp = File.createTempFile("temp", ".idx")
    TEMP_IDXPATH = Paths.get(temp.getAbsolutePath)
    indexer = new Indexer(TEMP_IDXPATH)

  }

  test("Add 10 nodes, write, then read") {
    for (i <- 0 until 10) {
      indexer.indexNode(i, SEG_LEN, i)
    }
    indexer.flush()
    val idx = BytesIndexReader.loadIndex(TEMP_IDXPATH)
    idx.size should be {
      1
    }
    idx.getIndexedChunkByIndex(0) should be {
      IndexChunk(0, SEG_LEN * 10, 0, (0, 9), (0, 9))
    }
  }

  test("Add a number times the max number of nodes, write, then read") {
    val times = 2
    for (i <- 0 until indexer.MAX_NODES * times) {
      //      println(i)
      indexer.indexNode(i, SEG_LEN, i)
    }
    indexer.flush()
    val idx = BytesIndexReader.loadIndex(TEMP_IDXPATH)
    idx.size should be {
      times
    }
    idx.getIndexedChunkByIndex(0) should be {
      IndexChunk(0,
        SEG_LEN * indexer.MAX_NODES,
        0,
        (0, indexer.MAX_NODES - 1),
        (0, indexer.MAX_NODES - 1))
    }
  }


}
