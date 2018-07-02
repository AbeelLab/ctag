package nl.tudelft.pl2.data.storage

import org.apache.logging.log4j.{LogManager, Logger}

case class HeatMapInfo(var layers: Int, var sequences: Int)

/**
  * Composite class to represent a heat-map with
  * the layer with most nodes (and the number of
  * nodes in that layer) and the complete nodes-
  * per-layer mapping.
  *
  * @param maximum          Layer-Number of nodes pair
  *                         representing the layer with
  *                         most nodes in it.
  * @param nodesPerLayerMap Mapping of layers to the
  *                         number of nodes in each
  *                         layer.
  */
class HeatMap(val maximum: (Integer, HeatMapInfo),
              var nodesPerLayerMap: java.util.Map[Integer, HeatMapInfo]) {

  /**
    * Log4J [[org.apache.logging.log4j.Logger]] used to log
    * debug information and other significant events.
    */
  private val LOGGER: Logger = LogManager.getLogger("HeatMapReader")

  /**
    * The maximum layer available in the nodes-per-layer
    * mapping.
    */
  val maxLayer: Integer = nodesPerLayerMap.keySet.stream
    .reduce((a, b) => if (a > b) a else b).orElse(-1)

  /**
    * The mapping of layers to the number of nodes in
    * each layer as a list of the numbers of nodes.
    */
  val nodesPerLayer = new java.util.ArrayList[Integer](maxLayer + 1)


  /**
    * Initializes the internal nodes-per-layer mapping
    * as a list and clears the original dataset.
    */
  val _: Unit = {
    for (i <- 0 to maxLayer) {
      nodesPerLayer.add(nodesPerLayerMap.getOrDefault(i, HeatMapInfo(0, 0))
          .layers)
    }

    LOGGER.info("Created heatmap")

    nodesPerLayerMap.clear()
  }
}
