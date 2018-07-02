package nl.tudelft.pl2.data.storage.readers

//scalastyle:off underscore.import
import java.io.File
import java.nio.file.Paths

import nl.tudelft.pl2.data.storage.writers.HeaderWriter
import org.junit.runner.RunWith
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable
//scalastyle:on underscore.import

import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class HeaderReaderTest extends FunSuite with BeforeAndAfter {
  private val TEMP: File = File.createTempFile("temp", ".hdr")


  private val PATH = Paths.get(TEMP.getAbsolutePath)
  private val SINGLE_OPTION = Map("A" -> ('B', "C"))
  private val DOUBLE_OPTION = Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2"))
  private val SINGLE_OPT_LEN = 10
  private val DOUBLE_OPT_LEN = 21

  private var writer: HeaderWriter = _
  before {
    writer = new HeaderWriter(PATH)
  }

  test("Store header with one option and read out") {
    writer.storeHeader(SINGLE_OPTION) should be {
      SINGLE_OPT_LEN
    }
    HeaderReader.readHeaders(PATH) should be {
      mutable.Buffer(Map("A" -> ('B', "C")))
    }
  }

  test("Store header with two options and read out") {
    writer.storeHeader(DOUBLE_OPTION) should be {
      DOUBLE_OPT_LEN
    }
    HeaderReader.readHeaders(PATH) should be {
      mutable.Buffer(Map("A1" -> ('B', "C1"), "A2" -> ('B', "C2")))
    }
  }

  test("Store two headers with one option and read out") {
    writer.storeHeader(SINGLE_OPTION) should be {
      SINGLE_OPT_LEN
    }
    writer.storeHeader(SINGLE_OPTION) should be {
      SINGLE_OPT_LEN
    }
    HeaderReader.readHeaders(PATH) should be {
      mutable.Buffer(Map("A" -> ('B', "C")), Map("A" -> ('B', "C")))
    }
  }

  after {
    writer.clearFile()
  }

}
