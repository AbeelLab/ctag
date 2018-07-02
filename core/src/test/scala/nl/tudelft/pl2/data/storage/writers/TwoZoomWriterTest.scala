package nl.tudelft.pl2.data.storage.writers

import java.io.File
import java.nio.file.{Path, Paths}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

//scalastyle:off underscore.import
import org.scalatest.Matchers._
//scalastyle:on underscore.import

import nl.tudelft.pl2.data.storage.{ReadWriteConstants => RWC, ReadWriteMethods => RWM}
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class TwoZoomWriterTest extends FunSuite with BeforeAndAfter {

  val TEMP: File = File.createTempFile("temp", ".ctg2")
  val TEMP_PATH: Path = Paths.get(TEMP.getAbsolutePath)
  var writer: CtagWriter = _

  before {
    writer = new CtagWriter(TEMP_PATH)
  }

  test("Store one node with no options or links") {
    writer.storeNode(1, "name",
      1, "content",
      mutable.Buffer(),
      mutable.Buffer(),
      Map(),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length
        + "content".length)
    }
  }
  test("Store chain with no options") {
    writer.storeChain(1, 1,
      Map(),
      ListBuffer(),
      0) should be {
      (RWC.CHAR_BYTES
        + RWC.C_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.C_INT_FIELDS * RWC.INT_BYTES)
    }
  }

  test("Store node, chain with options") {
    writer.storeNode(1,
      "name",
      1,
      "content",
      mutable.Buffer(),
      mutable.Buffer(),
      Map("A" -> ('B', "C")),
      Map()) should be {
      (RWC.CHAR_BYTES
        + RWC.N_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.N_INT_FIELDS * RWC.INT_BYTES
        + "name".length
        + "content".length
        + RWM.optionLength(Map("A" -> ('B', "C"))))
    }
    writer.storeChain(1,
      1,
      Map("A" -> ('B', "C")),
      ListBuffer(),
      0) should be {
      (RWC.CHAR_BYTES
        + RWC.C_SHORT_FIELDS * RWC.SHORT_BYTES
        + RWC.C_INT_FIELDS * RWC.INT_BYTES
        + RWM.optionLength(Map("A" -> ('B', "C"))))
    }
  }

  after {
    writer.clearFile()
    writer.close()
  }
}

