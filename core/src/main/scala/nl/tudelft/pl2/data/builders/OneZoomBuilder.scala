package nl.tudelft.pl2.data.builders

import java.util.Observer

import nl.tudelft.pl2.data.caches.SubCache
import nl.tudelft.pl2.data.indexing.{BytesIndexReader, Index, Indexer}
import nl.tudelft.pl2.data.loaders.GraphPathCollection
import nl.tudelft.pl2.data.storage.readers.CtagReader
import nl.tudelft.pl2.data.storage.writers.{CtagWriter, HeaderWriter}
import nl.tudelft.pl2.representation.exceptions.CTagException
import nl.tudelft.pl2.representation.external.Node
import nl.tudelft.pl2.representation.graph.LoadingState
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.mutable

/**
  * Exception for when the different conditions on nodes lead to an
  * invalid state.
  *
  * @param str The error message.
  * @param loc The error location.
  */
case class InvalidConditionCombinationException(str: String, loc: String)
  extends CTagException(str, loc)

/**
  * Builds the files needed for 1st semantic zoom level.
  *
  * @param paths Paths to generated files.
  */
class OneZoomBuilder(paths: GraphPathCollection) extends ZoomBuilder with ZoomBuilderData {
  /**
    * Writer of the 1st zoom level file.
    */
  override val currLvlWriter = new CtagWriter(paths.oneFilePath)
  /**
    * The zero level Index.
    */
  override val prevLvlIndex: Index = BytesIndexReader.loadIndex(paths.zeroIndexPath)
  /**
    * Reads the 0-level compressed file.
    */
  override val prevLvlReader = new CtagReader(paths.zeroFilePath)
  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  override val LOGGER: Logger = LogManager.getLogger("OneZoomBuilder")
  /**
    * Builds the index.
    */

  override val indexer = new Indexer(paths.oneIndexPath)

  override val chunksRetrieved: Array[Boolean] = Array.fill[Boolean](this.prevLvlIndex.size)(false)

  override val nodeDataByID: mutable.SortedMap[Int, BuilderNode] = mutable.SortedMap[Int,
    BuilderNode]()

  override val nodeDataByLayer: mutable.HashMap[Int, mutable.Set[Int]] with mutable.MultiMap[Int,
    Int] = new mutable.HashMap[Int, mutable.Set[Int]] with mutable.MultiMap[Int, Int]

  /**
    * Builds the one-level compressed file.
    */
  override def build(): Unit = {
    expandNodeDataMapsByID(0, this)
    var root = nodeDataByID(nodeDataByLayer.minBy(ndbl => ndbl._1)._2.min)

    //scalastyle:off while
    while (nodeDataByID.nonEmpty) {
      //scalastyle:on while
      val outEdges = root.outgoing

      if (outEdges.size == 2) {
        regBubOrIndel(root, outEdges)
      } else {
        registerNode(root)
      }
      if (nodeDataByID.nonEmpty) {
        root = nodeDataByID(nodeDataByLayer.minBy(ndbl => ndbl._1)._2.min)
      }
    }
  }

  def regBubOrIndel(root: BuilderNode, outEdges: mutable.Buffer[Int]): Unit = {
    val hiID = outEdges.head
    val loID = outEdges.last

    expandNodeDataMapsByID(hiID, this)
    expandNodeDataMapsByID(loID, this)

    if (nodeDataByID.contains(hiID) && nodeDataByID.contains(loID)) {
      val hi = nodeDataByID(hiID)
      val lo = nodeDataByID(loID)

      if (isBubble(root, hi, lo)) {
        registerBubble(root, hi, lo)
      } else if (isIndel(root, hi, lo)) {
        if (hi.outgoing.size == 1 && hi.outgoing.head == lo.id) {
          registerIndel(root, hi, lo)
        } else {
          registerIndel(root, lo, hi)
        }
      } else {
        registerNode(root)
      }
    } else {
      registerNode(root)
    }
  }

  /**
    * Checks whether the BuilderNodes form a Bubble.
    *
    * @param root The root of the Bubble.
    * @param hi   The high mid of the Bubble.
    * @param lo   The low mid of the Bubble.
    * @return Whether the BuilderNodes form a Bubble.
    */
  def isBubble(root: BuilderNode, hi: BuilderNode, lo: BuilderNode): Boolean = {
    val hiInEdges = hi.incoming
    val hiOutEdges = hi.outgoing
    val loInEdges = lo.incoming
    val loOutEdges = lo.outgoing

    if (hiInEdges.size == 1 && loInEdges.size == 1 &&
      hiOutEdges.size == 1 && loOutEdges.size == 1 &&
      hiOutEdges.head == loOutEdges.head &&
      hi.content.length == 1 && lo.content.length == 1) {

      val endID = loOutEdges.head
      expandNodeDataMapsByID(endID, this)
      val end = nodeDataByID(endID)

      end.incoming.size == 2 && end.incoming.contains(hi.id) && end.incoming.contains(lo.id)
    } else {
      false
    }
  }

  /**
    * Checks whether the BuilderNodes form an Indel.
    *
    * @param root The root of the Indel.
    * @param hi   The high mid of the Indel.
    * @param lo   The low mid of the Indel.
    * @return Whether the BuilderNodes form an Indel.
    */
  def isIndel(root: BuilderNode, hi: BuilderNode, lo: BuilderNode): Boolean = {
    val hiInEdges = hi.incoming
    val hiOutEdges = hi.outgoing
    val loInEdges = lo.incoming
    val loOutEdges = lo.outgoing
    ((hiInEdges.size == 1 && hiOutEdges.size == 1 && loInEdges.size == 2 && hiOutEdges.head == lo
      .id)
      || (loInEdges.size == 1 && loOutEdges.size == 1 && hiInEdges.size == 2 && loOutEdges.head
      == hi.id))
  }

  /**
    * Stores and indexes a 1-level Node.
    *
    * @param node The BuilderNode to register.
    */
  override def registerNode(node: BuilderNode): Unit = {
    //TODO: filter options
    val nodeLen = currLvlWriter.storeNode(node.id, node.name, node.layer, node.content,
      node.incoming, node.outgoing, node.options, node.genomes)
    indexer.indexNode(node.id, nodeLen, node.layer)

    nodeDataByID.remove(node.id)
    nodeDataByLayer.removeBinding(node.layer, node.id)

    node.outgoing.foreach(
      id => expandNodeDataMapsByID(id, this)
    )
  }

  /**
    * Registers a Bubble.
    *
    * @param root The start Node.
    * @param hi   The first middle Node.
    * @param lo   The second middle Node.
    */
  def registerBubble(root: BuilderNode, hi: BuilderNode, lo: BuilderNode): Unit = {
    val bubLen = currLvlWriter.storeBubble(
      root.id, root.layer,
      root.name, root.content,
      (hi.content(0), lo.content(0)),
      mergeOptions(root.options, hi.options, lo.options), root.incoming, lo.outgoing.head)
    indexer.indexNode(root.id, bubLen, root.layer)

    nodeDataByID.remove(root.id)
    nodeDataByID.remove(hi.id)
    nodeDataByID.remove(lo.id)

    nodeDataByLayer.removeBinding(root.layer, root.id)
    nodeDataByLayer.removeBinding(hi.layer, hi.id)
    nodeDataByLayer.removeBinding(lo.layer, lo.id)

    val endID = lo.outgoing.head
    nodeDataByID(endID).incoming -= hi.id
    nodeDataByID(endID).incoming -= lo.id
    nodeDataByID(endID).incoming += root.id
  }

  /**
    * Registers an Indel.
    *
    * @param root The start Node.
    * @param mid  The middle Node.
    * @param end  The end Node.
    */
  def registerIndel(root: BuilderNode, mid: BuilderNode, end: BuilderNode): Unit = {
    val indelLen = currLvlWriter.storeIndel(root.id, root.layer, root.name,
      root.content, mid.content,
      mergeOptions(root.options, mid.options, Map()),
      root.incoming, end.id)
    indexer.indexNode(root.id, indelLen, root.layer)

    nodeDataByID.remove(root.id)
    nodeDataByID.remove(mid.id)

    nodeDataByLayer.removeBinding(root.layer, root.id)
    nodeDataByLayer.removeBinding(mid.layer, mid.id)

    nodeDataByID(end.id).incoming -= mid.id
  }

  /**
    * Flushes the [[ZeroZoomBuilder]] by storing and indexing
    * the [[Node]] that is currently being built, if there is
    * such a [[Node]].
    */
  override def flush(): Unit = {
    indexer.flush()
  }

  /**
    * Closes the [[HeaderWriter]], [[CtagWriter]], and
    * [[Indexer]] associated with this [[ZeroZoomBuilder]].
    */
  override def close(): Unit = {
    prevLvlReader.close()
    currLvlWriter.close()
    indexer.close()
  }
}

/**
  * Companion object to the [[ZeroZoomBuilder]].
  */
object OneZoomBuilder {
  /**
    * Builds the files needed for a [[SubCache]]
    *
    * @param paths Paths for generating files.
    */
  def buildFiles(paths: GraphPathCollection, observer: Observer): Unit = {
    val builder = new OneZoomBuilder(paths)

    //scalastyle:off null
    observer.update(null, LoadingState.INDEX_WRITTEN)
    //scalastyle:on null

    try {
      builder.build()
      builder.flush()
    } finally {
      builder.close()
    }
  }
}
