package nl.tudelft.pl2.representation

import nl.tudelft.pl2.data.Graph
import nl.tudelft.pl2.data.caches.SubCache
import nl.tudelft.pl2.representation.external.{Bubble, Indel, Node}
import nl.tudelft.pl2.representation.external.components.DummyNode
import nl.tudelft.pl2.representation.graph.GraphHandle
import nl.tudelft.pl2.representation.ui.UIHelper

import scala.collection.JavaConverters.seqAsJavaListConverter

/**
  * Class that finds the node that belongs to a bubble or index.
  */
class NodeFinder {
  /**
    * Entry method for finding a node.
    *
    * @param dummyNode The dummy node to find the origin of
    * @return The origin node
    */
  def findNode(dummyNode: DummyNode): Node = {
    val handle: GraphHandle = UIHelper.getGraph
    if (handle == null || dummyNode.incoming.isEmpty) {
      dummyNode
    } else {
      val from: Int = dummyNode.incoming.head.from
      val root: Node = handle.retrieveCache().caches(1)
        .retrieveNodeByID(from)
      root match {
        case _: Indel =>
          findIndel(from, handle.retrieveCache().caches(0))
        case _: Bubble =>
          findBubble(from, handle.retrieveCache().caches(0))
        case _ =>
          dummyNode
      }
    }
  }

  /**
    * Find an indel from the from id.
    *
    * @param from  The root id of the indel
    * @param cache The cache to retrieve it from
    * @return The origin node
    */
  private def findIndel(from: Int,
                        cache: SubCache): Node = {
    val root: Node = cache.retrieveNodeByID(from)
    root.outgoing.map((edge) => edge.to).filter(_ >= 0).map(
      (index) => cache.retrieveNodeByID(index)
    ).minBy(_.layer)
  }

  /**
    * Find a bubble from the from id.
    *
    * @param from  The root id of the bubble
    * @param cache The cache to retrieve it from
    * @return The origin node
    */
  private def findBubble(from: Int,
                         cache: SubCache): Node = {
    val root: Node = cache.retrieveNodeByID(from)
    val nodes = root.outgoing.map((edge) => edge.to).filter(_ >= 0).map(
      (index) => cache.retrieveNodeByID(index)
    )
    val options: Graph.Options =
      nodes.flatMap(_.options).toMap
    val coordinates: Graph.Coordinates =
      nodes.flatMap(_.genomeCoordinates).toMap
    new Node(root.id,
      String.join(",", nodes.map(_.name).toList.asJava),
      nodes.map(_.layer).min,
      String.join(",", nodes.map(_.content).toList.asJava),
      nodes.flatMap(_.incoming),
      nodes.flatMap(_.outgoing),
      options,
      coordinates
    )
  }
}
