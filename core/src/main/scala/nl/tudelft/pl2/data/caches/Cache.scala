package nl.tudelft.pl2.data.caches

import nl.tudelft.pl2.representation.external.chunking.Chunk
import nl.tudelft.pl2.representation.external.components.DummyNode
import nl.tudelft.pl2.representation.external.{Edge, Node}

/**
  * The Cache interface declares the functions that
  * may be used to retrieve graph information quickly
  * and efficiently.
  *
  * Implementations of this interface should define
  * behaviour that describes how to retrieve information,
  * and what to do with it.
  *
  * @author Chris Lemaire
  * @author Maaike Visser
  */
trait Cache {

  /**
    * Gets the ID of the segment with the supplied name.
    *
    * @param nodeName Name of the [[Node]]
    * @return The ID of the [[Node]]
    */
  def retrieveNodeID(nodeName: String): Int

  /**
    * Retrieves a [[Node]] from the cache by id.
    *
    * @param id The id of the [[Node]].
    * @return The [[Node]].
    */
  def retrieveNodeByID(id: Int): Node

  /**
    * Gets the highest ID of all [[Node]]s in the [[Cache]].
    *
    * @return The highest ID.
    */
  def retrieveMaxNodeID: Int

  /**
    * Updates the highest [[Node]] ID in the cache to the given id.
    * This functionality is needed to properly represent [[DummyNode]]s
    * in the [[Cache]] and the GUI.
    *
    * @param id The new highest id
    */
  def updateMaxNodeID(id: Int): Unit

  /**
    * Creates a list of [[Node]]s in the cache sorted by layer.
    *
    * @return The list of [[Node]]s in the cache
    */
  def createNodeList: List[Node]

  /**
    * Creates a list of [[Edge]]s from the linkSet.
    *
    * @return A list of [[Edge]]s in the cache
    */
  def createEdgeList: List[Edge]

  /**
    * Retrieves a list of [[Chunk]]s that contain [[Node]]s
    * in the specified layer.
    *
    * @param layer The layer in which the [[Node]]s of
    *              interest reside.
    * @return A  list of [[Chunk]]s containing the [[Node]]s
    *         of interest.
    **/
  def retrieveChunksByLayer(layer: Int): List[Chunk]

  /**
    * Closes the connections between the [[Cache]] and
    * any files.
    */
  def close(): Unit
}
