package nl.tudelft.pl2.data.indexing

import java.nio.file.Paths

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.collection.mutable
//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

@RunWith(classOf[JUnitRunner])
class IndexTest extends FunSuite with BeforeAndAfter {
  /**
    * Some values for testing.
    */
  val OFFSET = 0L
  val LENGTH = 1
  val FIRST_INDEX = 0
  val SECOND_INDEX = 1
  val MIN_LAYER1 = 2
  val MAX_LAYER1 = 3
  val MIN_SEG1 = 3
  val MAX_SEG1 = 4
  val MIN_LAYER2 = 1
  val MIN_SEG2 = 1
  val MAX_SEG2 = 2
  val MAX_LAYER2 = 4

  private val CHUNK1 = IndexChunk(
    FIRST_INDEX,
    LENGTH,
    OFFSET,
    (MIN_LAYER1, MAX_LAYER1),
    (MIN_SEG1, MAX_SEG1))

  private val CHUNK2 = IndexChunk(
    SECOND_INDEX,
    LENGTH,
    OFFSET,
    (MIN_LAYER2, MAX_LAYER2),
    (MIN_SEG2, MAX_SEG2))

  private var IDX: Index = _

  before {
    IDX = new Index(Paths.get(""))
    IDX.insertChunk(CHUNK1)
    IDX.insertChunk(CHUNK2)
  }

  /**
    * We want to make sure that the [[IndexChunk]]s in the different
    * index trees are ordered by the specified ordering (instead of
    * by just the first field)
    */
  test("Inserting chunks should lead to the correct ordering") {
    IDX.minLayerTreeMap.firstKey() should be {
      MIN_LAYER2
    }
    IDX.minNodeTreeMap.firstKey() should be {
      MIN_SEG2
    }
    val idxmap = IDX.indexTreeMap
    idxmap.firstKey() should be {
      FIRST_INDEX
    }
  }

  test("Getting indexed chunk by layer") {
    IDX.getIndexedChunksByLayer(3) should be {
      mutable.Buffer(CHUNK2, CHUNK1)
    }
  }

  test("Getting indexed chunk by node id") {
    val chunks = IDX.getIndexedChunksByNodeID(MAX_SEG1)
    chunks should be {
      mutable.Buffer(CHUNK1)
    }
  }

  test("Getting indexed chunk by id") {
    val chunk = IDX.getIndexedChunkByIndex(0)
    chunk should be {
      CHUNK1
    }
  }
}
