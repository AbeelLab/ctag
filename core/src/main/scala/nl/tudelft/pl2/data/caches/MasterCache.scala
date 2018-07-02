package nl.tudelft.pl2.data.caches

import java.util
import java.util.logging.Logger

import nl.tudelft.pl2.data.Graph.Options
import nl.tudelft.pl2.data.storage.HeatMap
import nl.tudelft.pl2.representation.external.{Edge, Node}
import nl.tudelft.pl2.representation.external.chunking.Chunk
import org.apache.logging.log4j.{Logger, LogManager}

import scala.collection.mutable
import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * Point of contact between the GUI and sub-caches. Retrieves
  * different kinds of information based on the zoom level.
  *
  * @param headers The headers of the graph file.
  * @param heatMap The heat map of the graph.
  * @param caches  The sub-caches.
  */
case class MasterCache(headers: mutable.Buffer[Options],
                  heatMap: HeatMap,
                  caches: Array[SubCache]) extends Cache {

  /**
    * Log4J [[Logger]] used to log debug information
    * and other significant events.
    */
  private val LOGGER = LogManager.getLogger("MasterCache")
  LOGGER.info("Created master cache")
  /**
    * Decides from which sub-cache information
    * is retrieved.
    */
  private var zoomLevel = 0

  override def retrieveNodeID(nodeName: String): Int = {
    caches(zoomLevel).retrieveNodeID(nodeName)
  }

  override def retrieveNodeByID(id: Int): Node = {
    caches(zoomLevel).retrieveNodeByID(id)
  }

  override def retrieveMaxNodeID: Int = {
    caches(zoomLevel).retrieveMaxNodeID
  }

  /**
    * Retrieves the headers of the graph.
    *
    * @return The headers in the graph
    */
  def retrieveHeaderMap: util.Map[String, String] = {
    val scalaMap = new mutable.HashMap[String, String]()
    headers.foreach(optionsMap => {
      optionsMap.foreach { case (key: String, (_: Char, value: String)) =>
        scalaMap.put(key, value)
      }
    })
    scalaMap.asJava
  }

  override def updateMaxNodeID(id: Int): Unit = {
    caches.foreach(c => c.updateMaxNodeID(id))
  }

  override def createNodeList: List[Node] = {
    caches(zoomLevel).createNodeList
  }

  override def createEdgeList: List[Edge] = {
    caches(zoomLevel).createEdgeList
  }

  override def retrieveChunksByLayer(layer: Int): List[Chunk] = {
    caches(zoomLevel).retrieveChunksByLayer(layer)
  }

  /**
    * Sets the zoom level of the [[MasterCache]].
    *
    * @param zoomLevel The zoom level.
    * @return The zoom level.
    */
  def setZoomLevel(zoomLevel: Int): Int = {
    if (zoomLevel >= caches.length) {
      this.zoomLevel = caches.length - 1
      caches.length - 1
    } else if (zoomLevel < 0) {
      this.zoomLevel = 0
      0
    } else {
      this.zoomLevel = zoomLevel
      zoomLevel
    }
  }

  /**
    * Get the max layer of the graph from the zero zoom cache.
    *
    * @return The absolute max layer
    */
  def getMaxLayer: Integer =
    caches(0).asInstanceOf[SubCache].maxLayer

  override def close(): Unit = {
    caches.foreach(c => c.close())
  }

  def clear(): Unit =
    caches.foreach(_.clear)
}
