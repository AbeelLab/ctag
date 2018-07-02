package nl.tudelft.pl2.representation

import nl.tudelft.pl2.representation.exceptions.NodeNotFoundException
import nl.tudelft.pl2.representation.external.{Node, Edge => ChunkLink}

import scala.collection.mutable
import scala.collection.JavaConverters.seqAsJavaListConverter

/**
  * Class for the scala part of building a graph.
  */
class GraphBuilderHelper {
  type ChunkNode = Node
  /**
    * Type for using a java set in scala.
    *
    * @tparam T The type of the set content.
    */
  type JavaSet[T] = java.util.HashSet[T]

  /**
    * Type for using a java map in scala.
    *
    * @tparam K The type of the key.
    * @tparam V The type of the value.
    */
  type JavaMap[K, V] = java.util.HashMap[K, V]

  /**
    * Map that maps IDs of GraphNodes to ChunkNodes.
    */
  final val nodeMap = new mutable.HashMap[Int, Node]()

  /**
    * Max layer encountered while building.
    */
  private var _maxLayer = Integer.MIN_VALUE
  /**
    * Min layer encountered while building.
    */
  private var _minLayer = Integer.MAX_VALUE

  /**
    * The current highest Node id.
    */
  private var _maxId = Integer.MIN_VALUE

  /**
    * Remove the link from the outgoing list in the Node.
    *
    * @param node The Node to remove the link from
    * @param edge The link to remove from the Node
    * @return Whether the link was removed
    */
  def removeOutgoing(node: Node, edge: ChunkLink): Boolean = {
    val oldNum = node.outgoing.size
    node.outgoing -= edge
    oldNum > node.outgoing.size
  }

  /**
    * Remove the link from the incoming list in the Node.
    *
    * @param node The Node to remove the link from
    * @param link The link to remove from the Node
    * @return Whether the link was removed
    */
  def removeIncoming(node: Node, link: ChunkLink): Boolean = {
    val oldNum = node.incoming.size
    node.incoming -= link
    oldNum > node.incoming.size
  }

  /**
    * Add the link to the outgoing list of the Node.
    *
    * @param chunkNode The Node to add the link to
    * @param edge      The link to add to the Node
    */
  def addOutgoing(chunkNode: Node, edge: ChunkLink): Unit = {
    if (!chunkNode.outgoing.contains(edge)) {
      chunkNode.outgoing += edge
    }
  }


  /**
    * A wrapper for the scala method asJava.
    *
    * @param list List to convert
    * @tparam T Type of the list
    * @return The new JavaList
    */
  def createJavaList[T](list: List[T]): java.util.List[T] =
    list.asJava

  /**
    * A wrapper for the scala method asJava.
    *
    * @param buf Buffer to convert
    * @tparam T Type of the list
    * @return The new JavaList
    */
  def createJavaListFromMutableBuffer[T](buf: mutable.Buffer[T]): java.util.List[T] = buf.asJava


  /**
    * Get the max layer as a Java Integer.
    *
    * @return The max layer
    */
  def getMaxLayer: Integer = this.maxLayer

  /**
    * Get the max layer as a Java Integer.
    *
    * @return The max layer
    */
  def maxLayer: Integer = this._maxLayer

  /**
    * Get the min layer as a Java Integer.
    *
    * @return The min layer
    */
  def getMinLayer: Integer = this.minLayer

  /**
    * Get the min layer as a Java Integer.
    *
    * @return The min layer
    */
  def minLayer: Integer = this._minLayer

  def updateLayers(layer: Integer): Unit = {
    _maxLayer = Math.max(_maxLayer, layer)
    _minLayer = Math.min(_minLayer, layer)
  }

  /**
    * Updates the maximum id in this graph.
    * The new maximum is the max of the provided id and the current max.
    *
    * @param id The new maximum id.
    */
  def updateMaxId(id: Integer): Unit =
    _maxId = Math.max(maxId, id)

  /**
    * Getter for the maximum encountered Node id in this graph.
    *
    * @return The maximum Node id in this graph.
    */
  def maxId: Integer = _maxId

  /**
    * Add the link to the incoming list of the Node.
    *
    * @param chunkNode The Node to add the link to
    * @param edge      The Edge to add to the Node
    */
  def addIncoming(chunkNode: Node, edge: ChunkLink): Unit = {
    if (!chunkNode.incoming.contains(edge)) {
      chunkNode.incoming += edge
    }
  }

  /**
    * Retrieve a chunkNode from the cache by Node.
    *
    * @param node The Node to retrieve
    * @return The Node with said id
    */
  def retrieveNode(node: Node): Node =
    nodeMap(node.id)

  /**
    * Check if the graphBuilderHelper
    * has the Node with the given ID loaded.
    *
    * @param id The id of the Node
    * @return
    */
  def hasNode(id: Integer): Boolean =
    nodeMap.contains(id)

  /**
    * Add a Node to the Node map stored at its id,
    *
    * @param node The Node to store
    */
  def addNodeToMap(node: ChunkNode): Option[Node] =
    nodeMap.put(node.id, node)

  /**
    * Remove the given segment from the map of segments.
    *
    * @param node The segment to remove
    */
  def removeSegmentFromMap(node: ChunkNode): Unit =
    nodeMap.remove(node.id)

  /**
    * Get a segment from the segmentMap by id
    *
    * @param id The id of the segment
    * @return The segment
    */
  def getNodeByID(id: Integer): Node =
    try {
      nodeMap(id.toInt)
    } catch {
      case _: Exception => throw new NodeNotFoundException("We could not find"
        + " the segment with id " + id,
        "When trying to find the segment with id: " + id)
    }

}
