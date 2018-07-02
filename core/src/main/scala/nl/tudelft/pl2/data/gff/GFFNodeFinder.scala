package nl.tudelft.pl2.data.gff

import nl.tudelft.pl2.data.caches.{CacheChunk, SubCache}
import nl.tudelft.pl2.representation.external.{IntegerInterval, Node}
import nl.tudelft.pl2.representation.graph.GraphHandle
import org.apache.logging.log4j.LogManager

case class GFFNodeFinder(featurePair: FeaturePair,
                         graphHandle: GraphHandle) {
  /**
    * The logger for this class.
    */
  private val LOGGER = LogManager.getLogger("GFFNodeFinder")

  /**
    * A node based on the feature pair stored in the finder.
    *
    * @return A node that contains the annotation
    */
  def findNode(): Node = {
    val cache: SubCache = graphHandle.retrieveCache().caches(0)
    val genomes = graphHandle.getGenomes
    val genome: (Int, String) =
      genomes.map((genome) => (genomes.indexOf(genome), genome))
        .filter(t => t._2.contains(featurePair.id()))
        .reduce((t1, t2) => if (t1._2.length < t2._2.length) t1 else t2)

    val oldChunks: Set[CacheChunk] = cache.cachedChunks.toSet
    cache.cachedChunks.clear()

    val node = findNodeInRange(0, cache.maxLayer, cache, genome)

    cache.cachedChunks.clear()
    oldChunks.foreach(cache.cachedChunks.add)
    node
  }

  /**
    * Check the nodes in the given range for the annotation.
    *
    * @param min    THe lowest layer to check
    * @param max    The highest layer to check
    * @param cache  The cache to retrieve the nodes from
    * @param genome The genome to find
    * @return The node containing the annotation
    */
  def findNodeInRange(min: Int, max: Int,
                      cache: SubCache,
                      genome: (Int, String)): Node = {
    var left = min
    var right = max

    //scalastyle:off null
    var finalNode: Node = null
    //scalastyle:on null
    var foundLeftOfPair = true
    var foundRightOfPair = false

    LOGGER.debug("Starting loop")
    //scalastyle:off while
    while (finalNode == null && (foundLeftOfPair ^ foundRightOfPair)) {
      LOGGER.debug("Left : {}", left)
      LOGGER.debug("Right : {}", right)
      val avg = (left + right) / 2

      val (locationTuple, checkedLayers) =
        findNodeInLayer(avg, genome, cache)

      finalNode = locationTuple._1
      foundLeftOfPair = locationTuple._2
      foundRightOfPair = locationTuple._3

      if (finalNode != null) {
        LOGGER.debug("Annotation found in layer : {}", avg)
      } else if (foundLeftOfPair && foundRightOfPair) {
        finalNode = findNodeFromLayerSet(checkedLayers, genome, cache)
      } else if (foundLeftOfPair) {
        LOGGER.debug("Going right in graph search")
        left = avg
        right = right
      } else if (foundRightOfPair) {
        LOGGER.debug("Going left in graph search")
        left = left
        right = avg
      }
    }
    //scalastyle:on while
    finalNode
  }

  /**
    * Check the layer for the given node.
    *
    * @param layer  The layer to check
    * @param genome The genome to check for
    * @param cache  The cache to check in
    * @return Returns a tuple with as first argument a tuple that contains:
    *         _1: The node, if found
    *         _2: If the intervals in the check were
    *         to the left of the interval to find
    *         _3: If the intervals in the check were
    *         to the right of the interval to find
    *         And as second argument the layers that have been searched
    */
  def findNodeInLayer(layer: Int,
                      genome: (Int, String),
                      cache: SubCache):
  ((Node, Boolean, Boolean), Set[Integer]) = {
    val newChunks = cache.retrieveChunksByLayer(layer)
    val checkedLayers: Set[Integer] = newChunks.flatMap(_.layers()).toSet

    val nodeIntervals = newChunks.flatMap((chunk) => {
      chunk.cacheChunk.nodes
    }).filter(_.genomeCoordinates.contains(genome._1))
      .map((node) => (node.genomeCoordinates(genome._1), node))
      .map(tuple => (IntegerInterval(
        tuple._1, tuple._1 + tuple._2.content.length.toLong), tuple._2))

    //scalastyle:off null
    var returnTuple: (Node, Boolean, Boolean) = (null, false, false)
    //scalastyle:off null

    val interval = featurePair.interval
    nodeIntervals.foreach(tuple =>
      if (tuple._1.intersects(interval)) {
        returnTuple = (tuple._2, returnTuple._2, returnTuple._3)
      } else if (tuple._1.upperBound < interval.lowerBound) {
        returnTuple = (returnTuple._1, true, returnTuple._3)
      } else if (tuple._1.lowerBound > interval.upperBound) {
        returnTuple = (returnTuple._1, returnTuple._2, true)
      }
    )
    (returnTuple, checkedLayers)
  }

  /**
    * If a node should be in a layerSet but is not found the
    * layerSet should be searched more thoroughly.
    *
    * @param layerSet The set to check for holes
    * @return The node in the set
    */
  def findNodeFromLayerSet(layerSet: Set[Integer],
                           genome: (Int, String),
                           cache: SubCache): Node = {
    val min: Int = layerSet.min
    val max: Int = layerSet.max
    var layers: Array[Int] = (min to max).toList
      .filter(!layerSet.contains(_)).sorted.toArray
    //scalastyle:off null
    var finalNode: Node = null
    //scalastyle:on null

    //scalastyle:off while
    while (finalNode == null && layers.length > 1) {
      val layerAvg = layers.length / 2
      val (locationTuple, checkedLayers) =
        findNodeInLayer(layers(layerAvg), genome, cache)

      finalNode = locationTuple._1

      if (locationTuple._2) {
        layers = (layerAvg until layers.length).toList
          .filter(!layerSet.contains(_)).sorted.toArray
      } else {
        layers = (0 to layerAvg).toList
          .filter(!layerSet.contains(_)).sorted.toArray
      }
    }
    //scalastyle:on while

    if (layers.length == 1) {
      findNodeInLayer(layers(0), genome, cache)._1._1
    } else {
      finalNode
    }
  }
}
