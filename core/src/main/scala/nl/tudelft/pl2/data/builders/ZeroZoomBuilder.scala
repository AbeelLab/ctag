package nl.tudelft.pl2.data.builders

import java.io.{BufferedReader, File, FileReader}
import java.util.Observer
import java.util.logging.Logger

import nl.tudelft.pl2.data.Gfa1Parser
import nl.tudelft.pl2.data.Graph.{Coordinates, Options}
import nl.tudelft.pl2.data.caches.SubCache
import nl.tudelft.pl2.data.indexing.Indexer
import nl.tudelft.pl2.data.loaders.GraphPathCollection
import nl.tudelft.pl2.data.storage.writers.{CtagWriter, HeaderWriter, HeatMapWriter}
import nl.tudelft.pl2.representation.exceptions.CTagException
import nl.tudelft.pl2.representation.external.{Edge, Node}
import nl.tudelft.pl2.representation.ui.InfoSidePanel.SampleSelectionController
import org.apache.logging.log4j.LogManager

import scala.collection.mutable


case class EmptyBuildNodeException(reason: String, loc: String)
  extends CTagException(reason, loc)


case class EmptyGraphException(reason: String, loc: String)
  extends CTagException(reason, loc)

/**
  * Builds the files needed for 0th semantic zoom level.
  *
  * @param paths The collection of paths to different
  *              files used and written during building.
  */
class ZeroZoomBuilder(paths: GraphPathCollection) {
  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("ZeroZoomBuilder")

  /**
    * Writer of the header file.
    */
  private val headerWriter = new HeaderWriter(paths.headerPath)

  /**
    * Writer of the zoom file.
    */
  private val zoomWriter = new CtagWriter(paths.zeroFilePath)

  /**
    * Writer and builder of the index.
    */
  private val indexer = new Indexer(paths.zeroIndexPath)

  /**
    * Writer of the heat map.
    */
  private val heatMapWriter = new HeatMapWriter(paths.heatMapPath)

  /**
    * Maps [[Node]] names to their ID and layer.
    */
  private val nameToNodeDat = new mutable.TreeMap[String, (Int, Int)]()

  /**
    * Points from to to from.
    */
  private val incomingMap = new mutable.HashMap[Int, mutable.Set[Int]]
    with mutable.MultiMap[Int, Int]

  /**
    * The [[Node]] currently being built.
    */
  private var nodeBeingBuilt: Option[BuilderNode] = None

  /**
    * Currently registering links from this [[Node]].
    */
  private var registeringLinksFromNode = ""

  /**
    * The index of the current [[Node]].
    */
  private var nodeIndex: Int = 0

  /**
    * The mapping of genomes to their indices, or identifiers.
    */
  private val genomes: mutable.Map[String, Int] = mutable.HashMap[String, Int]()

  /**
    * The mapping of genome IDs to their respective current coordinate.
    */
  private val genomeCoordinates: mutable.Map[Int, Long] = mutable.HashMap[Int, Long]()

  /**
    * Passes a header from to the [[HeaderWriter]] to be
    * stored to disk.
    *
    * @param options The [[Options]] representing the header.
    */
  def registerHeader(options: Options): Unit = {
    headerWriter.storeHeader(options)

    options.filterKeys(_ == "ORI").foreach(pair => {
      genomes ++= pair._2._2.split(';').zipWithIndex
      genomeCoordinates ++= genomes.values.map(i => (i, 0L))
    })
  }

  private def getGenomes(options: Options): Array[Int] =
    options.getOrElse(SampleSelectionController.GENOME_TAG, (' ', ""))._2
      .split(';').filterNot(_.isEmpty).map(s => {
      if (s forall Character.isDigit) {
        s.toInt
      } else {
        genomes(s)
      }
    })

  /**
    * Builds a [[Node]] with [[Edge]] references to be
    * stored to disk.
    *
    * @param name    The [[Node]] name.
    * @param content The [[Node]] content.
    * @param options The [[Options]] that apply to this [[Node]].
    */
  def registerNode(name: String, content: String, options: Options): Unit = {
    registerCurrentNode()
    if (nameToNodeDat.contains(registeringLinksFromNode)) {
      nameToNodeDat.remove(registeringLinksFromNode)
    }

    val (id, layer) = if (nameToNodeDat.contains(name)) {
      nameToNodeDat(name)
    } else {
      nameToNodeDat.put(name, (nodeIndex, 0))
      nodeIndex += 1
      nameToNodeDat(name)
    }

    val nodeGenomes = getGenomes(options)

    nodeBeingBuilt = Some(new BuilderNode(id, name, layer, content,
      incomingMap.getOrElse(id, mutable.Set[Int]()).toBuffer, mutable.Buffer[Int](),
      options, nodeGenomes.map(gen => (new Integer(gen), genomeCoordinates.apply
      (gen))).toMap))

    incomingMap.remove(id)
    nodeGenomes.foreach(gen => genomeCoordinates(gen) += content.length)
  }

  /**
    * Stores the current node being built, ending the building
    * of that node. Also calls the indexNode method for the
    * indexer to ensure the node gets indexed.
    */
  private def registerCurrentNode(): Unit =
    if (nodeBeingBuilt.isDefined) {
      val builtNode = nodeBeingBuilt.get
      val nodeLen = zoomWriter.storeNode(
        builtNode.id,
        builtNode.name,
        builtNode.layer,
        builtNode.content,
        builtNode.incoming,
        builtNode.outgoing,
        builtNode.options,
        builtNode.genomes)

      indexer.indexNode(builtNode.id, nodeLen, builtNode.layer)
      builtNode.outgoing.foreach(e => incomingMap.addBinding(e, builtNode.id))
      heatMapWriter.incrementLayerAt(builtNode.layer)
      nodeBeingBuilt = None: Option[BuilderNode]
    }

  /**
    * Adds a reference to the [[Edge]] to the [[Node]] currently
    * being built for later storage.
    *
    * @param from         The origin [[Node]] of this [[Edge]].
    * @param reversedFrom Whether the origin [[Node]] is reversed.
    * @param to           The destination [[Node]] of this [[Edge]].
    * @param reversedTo   Whether the destination [[Node]] is reversed.
    * @param options      The [[Options]] that apply to this [[Edge]].
    */
  def registerEdge(from: String,
                   reversedFrom: Boolean,
                   to: String,
                   reversedTo: Boolean,
                   options: Options): Unit = {
    registeringLinksFromNode = from
    val (fromId, fromLayer) = nameToNodeDat(from)
    var layer = -1
    if (nameToNodeDat.contains(to)) {
      val (toId, toLayer) = nameToNodeDat(to)
      layer = Math.max(toLayer, fromLayer + 1)
      nameToNodeDat.put(to, (toId, layer))
    } else {
      layer = fromLayer + 1
      nameToNodeDat.put(to, (nodeIndex, layer))
      nodeIndex += 1
    }

    val node = nodeBeingBuilt.getOrElse(
      throw EmptyBuildNodeException("You tried adding links to an empty BuilderNode.",
        "In ZeroZoomBuilder."))

    assert(fromId == node.id)

    node.outgoing += nameToNodeDat(to)._1
  }

  /**
    * Flushes the [[ZeroZoomBuilder]] by storing and indexing
    * the [[Node]] that is currently being built, if there is
    * such a [[Node]].
    */
  def flush(): Unit = {
    registerCurrentNode()
    if (zoomWriter.getFileLength == 0) {
      close()
      throw EmptyGraphException(paths.zeroFilePath + " contains no segments.",
        "In ZeroZoomBuilder.")
    }
    indexer.flush()
    heatMapWriter.flush()
  }

  /**
    * Closes the [[HeaderWriter]], [[CtagWriter]], and
    * [[Indexer]] associated with this [[ZeroZoomBuilder]].
    */
  def close(): Unit = {
    zoomWriter.close()
    headerWriter.close()
    indexer.close()
    heatMapWriter.close()
    LOGGER.debug("Closed all files.")
  }

  /**
    * Counts the number of times a substring is in a full string.
    *
    * @param str    The full string.
    * @param substr The substring.
    * @return The number of times the substring is in a full string.
    */
  def countSubstring(str: String, substr: String): Int
  = substr.r.findAllMatchIn(str).length
}

/**
  * Companion object to the [[ZeroZoomBuilder]].
  */
object ZeroZoomBuilder {
  /**
    * Builds the files needed for a [[SubCache]]
    *
    * @param paths The paths used for building additional files.
    */
  def buildFiles(paths: GraphPathCollection, observer: Observer): Unit = {
    val builder = new ZeroZoomBuilder(paths)
    val size = new File(paths.gfaPath.toUri).length()
    val reader = new BufferedReader(new FileReader(paths.gfaPath.toString))
    try {
      Gfa1Parser.parse(reader, builder, observer, size)
    } catch {
      case any: Any => any.printStackTrace()
    } finally {
      reader.close()
      try {
        builder.flush()
      } finally {
        builder.close()
      }
    }
  }
}
