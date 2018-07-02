package nl.tudelft.pl2.data

import java.io.{BufferedReader, ByteArrayInputStream, InputStreamReader}
import java.util.{Observable, Observer}

import nl.tudelft.pl2.data.Gfa1ParserTest.{justParse, parseShouldVerify, parseShouldVerifyEachCall}
import nl.tudelft.pl2.data.builders.ZeroZoomBuilder
import nl.tudelft.pl2.data.caches.Cache
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.{mock, only, times, verify}
import org.scalatest.FunSuite
import org.scalatest.Matchers.{a, be}
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks.forAll
import org.scalatest.prop.TableDrivenPropertyChecks.Table

object Gfa1ParserTest {
  private val observer = new Observer {
    override def update(o: Observable, arg: scala.Any): Unit = Unit
  }

  /**
    * Parses the given string and calls the given verification
    * function with the resulting (mock) [[Cache]] with each of the
    * given checks.
    *
    * @param in      Input [[String]] to be parsed.
    * @param checks  A list of some data to be checked individually
    *                by the verify function.
    * @param fVerify The verification function called for each check.
    * @tparam R The type of the data given to the verification
    *           function and provided in the checks list. In our use-case
    *           this is some tuple containing call-information.
    */
  def parseShouldVerifyEachCall[R](in: String, checks: List[R])
                                  (fVerify: (R, ZeroZoomBuilder) => Unit):
  Unit =
    parseShouldVerify(in, mc =>
      checks.foreach(call =>
        fVerify(call, verify(mc, times(1)))))

  /**
    * Calls the parser on the given [[String]] and thereafter
    * calls the given verification function with a mocked [[ZeroZoomBuilder]].
    *
    * @param s      String to call the parser with.
    * @param verify Verification function to call after parsing.
    */
  def parseShouldVerify(s: String,
                        verify: ZeroZoomBuilder => Unit): Unit = {
    val mb: ZeroZoomBuilder = mock(classOf[ZeroZoomBuilder])
    parseString(s, mb)
    verify(mb)
  }

  /**
    * Calls the [[Gfa1Parser]] parse function with the given
    * [[String]] 's' and [[Cache]] 'c' as its inputs.
    *
    * @param s String to parse.
    * @param b Cache to store the results in.
    */
  def parseString(s: String, b: ZeroZoomBuilder): Unit = {
    val is = new BufferedReader(new InputStreamReader(
      new ByteArrayInputStream(s.getBytes)))

    Gfa1Parser.parse(is, b, observer, 1)
  }

  /**
    * Just parses the given input [[String]] with some mocked [[Cache]].
    * In our use-case this is used when exceptions are expected.
    *
    * @param in Input [[String]] to be parsed.
    */
  def justParse[C <: Cache](in: String): Unit =
    parseString(in, mock(classOf[ZeroZoomBuilder]))

  /**
    * Just parses file represented by
    * the input [[String]] to a supplied cache.
    *
    * @param in Input [[String]] to be parsed.
    * @param b  [[Cache]] to parse the [[String]] to.
    */
  def parseFile(in: String, b: ZeroZoomBuilder): Unit = {
    val is = new BufferedReader(new InputStreamReader(
      Thread.currentThread().getContextClassLoader.getResourceAsStream(in)
    ))

    Gfa1Parser.parse(is, b, observer, 1)
  }

}

/**
  * Test class for the Gfa1Parser.
  */
@RunWith(classOf[JUnitRunner])
class Gfa1ParserTest extends FunSuite {

  // A few options and corresponding mappings.
  private val OPT1 = ("OPT:Z:123456", "OPT" -> ('Z', "123456"))
  private val OPT2 = ("S:S:djbsbajd", "S" -> ('S', "djbsbajd"))
  private val OPT3 = ("D:D:mddmd", "D" -> ('D', "mddmd"))

  // A few names and contents for [[Segment]]s
  private val NAME1 = "nameeee1"
  private val CONT1 = "ACTGATGATCGGATC"
  private val NAME2 = "someother"
  private val CONT2 = "ACCGGATC"

  /**
    * Some input [[String]]s with corresponding results
    * that should create and register headers.
    */
  private val headers = Table(
    ("in", "calls"),
    (s"H\t${OPT1._1}", List(Map(OPT1._2))),
    (s"H\t${OPT2._1}", List(Map(OPT2._2))),
    (s"H\t${OPT1._1}\nH\t${OPT2._1}", List(Map(OPT1._2), Map(OPT2._2))),
    (s"H\t${OPT1._1}\t${OPT2._1}", List(Map(OPT1._2, OPT2._2))),
    (s"H\t${OPT2._1}\t${OPT1._1}\nH\t${OPT3._1}",
      List(Map(OPT2._2, OPT1._2), Map(OPT3._2)))
  )

  /**
    * Some input [[String]]s with corresponding results
    * that should create and register
    * [[nl.tudelft.pl2.representation.external.Node]]s.
    */
  private val segments = Table[String, List[(String, String, Map[String, (Char, String)])]](
    ("in", "calls"),
    (s"S\t$NAME1\t$CONT1\t*\t${OPT1._1}",
      List((NAME1, CONT1, Map(OPT1._2)))),
    (s"S\t$NAME2\t$CONT2\t*\t${OPT2._1}\t${OPT1._1}",
      List((NAME2, CONT2, Map(OPT2._2, OPT1._2)))),
    (s"S\t$NAME1\t$CONT2\t*",
      List((NAME1, CONT2, Map()))),
    (s"S\t$NAME1\t$CONT1\t*\nS\t$NAME2\t$CONT1\t*",
      List((NAME1, CONT1, Map()), (NAME2, CONT1, Map())))
  )

  /**
    * Some input [[String]]s with corresponding results
    * that should create and register
    * [[nl.tudelft.pl2.representation.external.Edge]]s.
    */
  private val links = Table[String,
    List[(String, Boolean, String, Boolean, Map[String, (Char, String)])]
    ](
    ("in", "calls"),
    (s"L\t$NAME1\t+\t$NAME2\t-\t0M\t${OPT1._1}",
      List((NAME1, false, NAME2, true, Map(OPT1._2)))),
    (s"L\t$NAME1\t-\t$NAME2\t+\t0M\t${OPT1._1}",
      List((NAME1, true, NAME2, false, Map(OPT1._2)))),
    (s"L\t$NAME1\t+\t$NAME2\t+\t0M\nL\t$NAME1\t-\t$NAME2\t-\t0M",
      List((NAME1, false, NAME2, false, Map()),
        (NAME1, true, NAME2, true, Map())))
  )

  /**
    * Tests some simple correct input header strings.
    */
  test("Simple headers should be parsed correctly") {
    forAll(headers) { (in, calls) =>
      parseShouldVerifyEachCall(in, calls) { (call, mc) =>
        mc.registerHeader(call)
      }
    }
  }

  /**
    * Tests some simple correct input
    * [[nl.tudelft.pl2.representation.external.Node]] strings.
    */
  test("Simple segments should be parsed correctly") {
    forAll(segments) { (in, calls) =>
      parseShouldVerifyEachCall(in, calls) { (call, mc) =>
        mc.registerNode(call._1, call._2, call._3)
      }
    }
  }

  /**
    * Tests some simple correct input
    * [[nl.tudelft.pl2.representation.external.Edge]] strings.
    */
  test("Simple links should be parsed correctly") {
    forAll(links) { (in, calls) =>
      parseShouldVerifyEachCall(in, calls) { (call, mc) =>
        mc.registerEdge(call._1, call._2, call._3, call._4, call._5)
      }
    }
  }

  /**
    * Simple comment line test.
    */
  test("Comment lines should be ignored") {
    parseShouldVerify("S\tb\tGCT\t*\n#\tsome comment here", mc =>
      verify(mc, only).registerNode("b", "GCT", Map()))
  }

  /**
    * Simple test to ensure exclusion of containments.
    */
  test("Containment lines should not be parsed") {
    a[Gfa1ParseException] should be thrownBy {
      parseShouldVerify("C\tname\t+\tname2\t-\t0\t0M", _ => {})
    }
  }

  /**
    * Simple test to ensure exclusion of paths.
    */
  test("Path lines should not be parsed") {
    a[Gfa1ParseException] should be thrownBy {
      parseShouldVerify("P\tname\tnames\t0M", _ => {})
    }
  }


  // Tests to ensure invalid strings generate an exception.
  test("Segment parsing - too little arguments") {
    a[Gfa1ParseException] should be thrownBy {
      justParse("S\tname\tTCG")
    }
  }


  test("Link parsing - too little arguments") {
    a[Gfa1ParseException] should be thrownBy {
      justParse("L\tname1\t+\tname2\t+")
    }
  }

  test("Link parsing - invalid polarity 1") {
    a[Gfa1ParseException] should be thrownBy {
      justParse("L\tname1\t4\tname2\t+")
    }
  }

  test("Link parsing - invalid polarity 1: too long") {
    a[Gfa1ParseException] should be thrownBy {
      justParse("L\tname1\t--\tname2\t+")
    }
  }

  test("Link parsing - invalid polarity 2") {
    a[Gfa1ParseException] should be thrownBy {
      justParse("L\tname1\t+\tname2\td")
    }
  }

  test("Link parsing - invalid polarity 2: too long") {
    a[Gfa1ParseException] should be thrownBy {
      justParse("L\tname1\t+\tname2\t++")
    }
  }
}
