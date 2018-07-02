package nl.tudelft.pl2.data.builders


import java.util.Observer

import nl.tudelft.pl2.data.Graph.Options
import nl.tudelft.pl2.data.caches.SubCache
import nl.tudelft.pl2.data.indexing.{BytesIndexReader, Index, Indexer}
import nl.tudelft.pl2.data.loaders.GraphPathCollection
import nl.tudelft.pl2.data.storage.readers.CtagReader
import nl.tudelft.pl2.data.storage.writers.CtagWriter
import nl.tudelft.pl2.representation.external.Node
import nl.tudelft.pl2.representation.graph.LoadingState
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.mutable


/**
  * Builds the files needed for 1st semantic zoom level.
  *
  * @param paths Paths to generated files.
  */
class TwoZoomBuilder(paths: GraphPathCollection) extends ZoomBuilder with ZoomBuilderData {
  override val currLvlWriter = new CtagWriter(paths.twoFilePath)

  override val prevLvlIndex: Index = BytesIndexReader.loadIndex(paths.oneIndexPath)

  override val prevLvlReader = new CtagReader(paths.oneFilePath)

  override val LOGGER: Logger = LogManager.getLogger("TwoZoomBuilder")

  override val indexer = new Indexer(paths.twoIndexPath)

  override val chunksRetrieved: Array[Boolean] =
    Array.fill[Boolean](prevLvlIndex.size)(false)

  override val nodeDataByID: mutable.SortedMap[Int, BuilderNode] =
    mutable.SortedMap[Int, BuilderNode]()

  override val nodeDataByLayer: mutable.HashMap[Int, mutable.Set[Int]] with mutable.MultiMap[Int,
    Int] = new mutable.HashMap[Int, mutable.Set[Int]] with mutable.MultiMap[Int,
    Int]

  override def build(): Unit = {
    expandNodeDataMapsByID(0, this)
    var root = nodeDataByID(nodeDataByLayer.minBy(ndbl => ndbl._1)._2.min)
    //scalastyle:off while
    while (nodeDataByID.nonEmpty) {
      val outEdges = root.outgoing

      if (outEdges.size == 1) {
        val midsOpts = Map[String, (Char, String)]()
        val midID = outEdges.head


        expandNodeDataMapsByID(midID, this)

        val chainRoot = nodeDataByID(midID)
        var mid = nodeDataByID(midID)

        while (mid.incoming.size == 1 && mid.outgoing.size == 1) {
          //scalastyle:on while
          val nextMidID = mid.outgoing.head
          expandNodeDataMapsByID(nextMidID, this)
          nodeDataByID.remove(mid.id)
          nodeDataByLayer.removeBinding(mid.layer, mid.id)
          midsOpts ++ mid.options
          mid = nodeDataByID(nextMidID)

        }
        registerNode(root)
        if (chainRoot.id != mid.id) {
          registerChain(chainRoot, midsOpts, mid)
        }
      } else {
        registerNode(root)
      }
      if (nodeDataByID.nonEmpty) {
        root = nodeDataByID(nodeDataByLayer.minBy(ndbl => ndbl._1)._2.min)
      }
    }
  }

  /**
    * Registers a chain on disk.
    *
    * @param root      The root of the chain.
    * @param chainOpts The options that apply to this chain.
    * @param end       The end node of this chain.
    */
  def registerChain(root: BuilderNode, chainOpts: Options, end: BuilderNode): Unit = {
    val chainLen = currLvlWriter.storeChain(root.id, root.layer, chainOpts, root.incoming,
      end.id)
    indexer.indexNode(root.id, chainLen, root.layer)
    nodeDataByID.remove(root.id)
    nodeDataByLayer.removeBinding(root.layer, root.id)
  }

  /**
    * Flushes the [[ZeroZoomBuilder]] by storing and indexing
    * the [[Node]] that is currently being built, if there is
    * such a [[Node]].
    */
  override def flush(): Unit = {
    indexer.flush()
  }

  override def close(): Unit = {
    prevLvlReader.close()
    currLvlWriter.close()
    indexer.close()
  }

  override def registerNode(node: BuilderNode): Unit = {
    //TODO: filter options
    val nodeLen = if (node.isInstanceOf[BuilderBubble]) {
      val bb = node.asInstanceOf[BuilderBubble]
      currLvlWriter.storeBubble(bb.id, bb.layer, bb.name, bb.content, (bb.cHi, bb.cLo), bb
        .options, bb.incoming, bb.end)
    } else if (node.isInstanceOf[BuilderIndel]) {
      val bi = node.asInstanceOf[BuilderIndel]
      currLvlWriter.storeIndel(bi.id, bi.layer, bi.name, bi.content, bi.midContent, bi.options, bi
        .incoming, bi.end)
    } else {
      currLvlWriter.storeNode(node.id, node.name, node.layer, node.content,
        node.incoming, node.outgoing, node.options, node.genomes)
    }
    indexer.indexNode(node.id, nodeLen, node.layer)
    nodeDataByID.remove(node.id)
    nodeDataByLayer.removeBinding(node.layer, node.id)
    node.outgoing.foreach(
      id => expandNodeDataMapsByID(id, this)
    )
  }
}

/**
  * Companion object to the [[ZeroZoomBuilder]].
  */
object TwoZoomBuilder {
  /**
    * Builds the files needed for a 2-level [[SubCache]]
    *
    * @param paths Paths for generating files.
    */
  def buildFiles(paths: GraphPathCollection, observer: Observer): Unit = {
    val builder = new TwoZoomBuilder(paths)

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
