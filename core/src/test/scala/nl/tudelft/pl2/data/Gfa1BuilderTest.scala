package nl.tudelft.pl2.data

import java.io.{BufferedReader, File, FileReader}
import java.nio.file.{Path, Paths}

import nl.tudelft.pl2.representation.external.Node
import nl.tudelft.pl2.representation.graph.handles.FullGraph
import nl.tudelft.pl2.representation.graph.loaders.FullGraphLoader
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters.{asScalaBufferConverter, asScalaSetConverter, seqAsJavaListConverter}

/**
  * Test class for the gfa1 builder singleton.
  */
@RunWith(classOf[JUnitRunner])
class Gfa1BuilderTest extends FunSuite {
  val TB10_SMALL: Path = Paths.get(Thread
    .currentThread
    .getContextClassLoader
    .getResource("TB10_small.gfa").toURI)

  /**
    * Setup method for the tests
    *
    * @return The objects needed to build a file
    */
  def before(): (List[Node], FullGraph, File) = {
    val loader = new FullGraphLoader()
    val file = new File(TB10_SMALL.toUri)
    val fullgraph: FullGraph =
      loader.load(TB10_SMALL, new GraphUpdateCompleter(file))
        .asInstanceOf[FullGraph]

    loader.getLoadFutures.asScala.foreach(_.get())

    val nodeList: List[Node] = fullgraph.getLayerSet.asScala
      .flatMap(fullgraph.getSegmentsFromLayer(_).asScala).toList

    fullgraph.unload()
    (nodeList, fullgraph, file)
  }

  /**
    * Test the string produced by the builder.
    */
  test("Test file replication in command line") {
    val (nodeList: List[Node], fullgraph: FullGraph, file: File) = before()
    val reader: BufferedReader = new BufferedReader(new FileReader(file))
    Gfa1Builder.buildGfaFromNodesWHeaders(
      nodeList.asJava,
      fullgraph.retrieveCache().headers).split("\n").foreach((line) => {
      val readLine = reader.readLine()
      val testLine = readLine.split("\t")
      val newLine = line.split('\t')

      assertResult(testLine(0)) {
        newLine(0)
      }
      assertResult(testLine(1)) {
        newLine(1)
      }
    })
  }

  /**
    * Test writing to file.
    */
  test("Test file replication in file") {
    val (nodeList: List[Node], fullgraph: FullGraph, file: File) = before()
    val reader: BufferedReader = new BufferedReader(new FileReader(file))
    val path = file.toURI.getPath
    val newFile = new File(path.substring(0, path.lastIndexOf('.')) + "_replica.gfa")
    Gfa1Builder.writeGfaFileFromNodes(nodeList.asJava, fullgraph, newFile.toPath, addHeaders = true)
    val newFileReader: BufferedReader = new BufferedReader(new FileReader(newFile))
    //scalastyle:off while
    while (reader.ready()) {
      val readerLine = reader.readLine()
      val madeLine = newFileReader.readLine()

      assertResult(readerLine(0)) {
        madeLine(0)
      }
      assertResult(readerLine(1)) {
        madeLine(1)
      }
    }
    //scalastyle:on while
  }
}
