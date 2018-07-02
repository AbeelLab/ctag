package nl.tudelft.pl2.representation.ui.graph

import java.util.Optional

import javafx.scene.canvas.GraphicsContext
import nl.tudelft.pl2.representation.external.Node
import nl.tudelft.pl2.representation.ui.UIHelper
//scalastyle:off underscore.import
import nl.tudelft.pl2.representation.ui.graph.GraphCoordinateSystem._
//scalastyle:on underscore.import

/**
  * Companion object with constants used by the coordinate
  * system.
  */
object GraphCoordinateSystem {

  /**
    * The height of the node in pixels we will be rendering.
    * This can be varied in the future when we work with
    * settings and zoom levels.
    */
  val NODE_HEIGHT = 50

  /**
    * The fraction of the layer that is used to display nodes.
    * If each layer is represented as follows:
    * <- Total layer width (totalWidth()) ->
    * <- nodeWidth -><-   partition part  ->
    * then totalWidth * NODE_PART = nodeWidth.
    */
  val NODE_PART = 0.8

  /**
    * The fraction of the row that is used to display nodes.
    */
  val VERTICAL_NODE_PART = 0.6

  /**
    * The default aspect ratio of width:height.
    */
  val DEFAULT_ASPECT_RATIO: Double = 1.0

  /**
    * The minimum height of the visible node.
    */
  val MIN_NODE_HEIGHT: Double = 10

  /**
    * The maximum height of the visible node.
    */
  val MAX_NODE_HEIGHT: Double = 60

  /**
    * The minimum offset between nodes in the y-direction.
    */
  val MIN_Y_OFFSET: Double = 5

  /**
    * The maximum offset between nodes in the y-direction.
    */
  val MAX_Y_OFFSET: Double = 30
}

/**
  * A coordinate system represented by a number of parameters.
  * The coordinate system is used to simulate a grid upon which
  * nodes and padding can be placed. The graph is drawn with nodes
  * in certain 'layers' and 'rows' representing horizontal and
  * vertical indices, respectively. The grid is translated into
  * on-screen heights, widths and coordinates.
  *
  * @param graphics    The graphics context used to determine properties
  *                    of the screen on which the graph is drawn.
  * @param shownLayers The number of layers shown on-screen.
  */
class GraphCoordinateSystem(val graphics: GraphicsContext,
                            var shownLayers: Double = NodeDrawer.SHOW_DEFAULT) {

  /**
    * The left-most layer shown on-screen as a fraction
    * to also represent the amount the layer is off-screen.
    */
  private var _minLayer: Double = -shownLayers / 3.0

  /**
    * The upper-most row shown on-screen.
    * Also represented as a fraction.
    */
  private var _minRow: Double = _minLayer

  /**
    * The width of nodes measured in on-screen pixels.
    */
  var nodeWidth: Double = 0.0

  /**
    * The height of nodes measured in on-screen pixels.
    */
  var nodeHeight: Double = NODE_HEIGHT

  /**
    * The amount of padding in between nodes in the x-direction.
    */
  var padding: Double = 0.0

  /**
    * The y-offset/padding between nodes.
    */
  var offset: Double = 0

  /**
    * Initializes the nodeWidth and padding for the
    * [[GraphCoordinateSystem]] directly after object creation.
    */
  private val _: Unit = {
    val (newTotalWidth, newTotalHeight) = calculateNewGrid(shownLayers)
    calculateNodePartsOfGrid(newTotalWidth, newTotalHeight)
  }

  /**
    * Limits the target number between the given bounds.
    *
    * @param target The number to limit.
    * @param min    The minimum limit to bind to.
    * @param max    The maximum limit to bind to.
    * @return The bounded integer.
    */
  private def limit(target: Double, min: Double, max: Double): Double =
    Math.min(max, Math.max(min, target))

  /**
    * Calculates the new nodeWidth, nodeHeight, padding and
    * offset values based on the new total width and height.
    *
    * @param newTotalWidth  The new total width.
    * @param newTotalHeight The new total height.
    */
  private def calculateNodePartsOfGrid(newTotalWidth: Double, newTotalHeight: Double): Unit = {
    nodeWidth = Math.max(1.0, newTotalWidth * NODE_PART)
    nodeHeight = limit(newTotalHeight * VERTICAL_NODE_PART,
      MIN_NODE_HEIGHT, MAX_NODE_HEIGHT)

    padding = newTotalWidth - nodeWidth
    offset = limit(newTotalHeight - nodeHeight,
      MIN_Y_OFFSET, MAX_Y_OFFSET)
  }

  /**
    * Calculates the new grid column and row sizes.
    *
    * @return Tuple of the new total width and new total height.
    */
  private def calculateNewGrid(shownLayers: Double): (Double, Double) = {
    val newTotalWidth = screenWidth / shownLayers
    val newTotalHeight = limit(screenWidth / (shownLayers * DEFAULT_ASPECT_RATIO),
      MIN_NODE_HEIGHT + MIN_Y_OFFSET, MAX_NODE_HEIGHT + MAX_Y_OFFSET)

    (newTotalWidth, newTotalHeight)
  }

  /**
    * Calculates the layer under a certain on-screen x-position.
    *
    * @param x The on-screen x-position to check.
    * @return The layer under the given x-position.
    */
  def layerAt(x: Double): Double = x / totalWidth + minLayer

  /**
    * Calculates the row under a certain on-screen y-position.
    *
    * @param y The on-screen y-position to check.
    * @return The row under the given y-position.
    */
  def rowAt(y: Double): Double = y / totalHeight + minRow

  /**
    * Calculates which node is drawn/intersected at a given
    * on-screen position. The layer and row indices are
    * returned if one is intersected, (-1, -1) is returned
    * when no actual node was clicked.
    *
    * @param x               The x-coordinate of the position to check.
    * @param y               The y-coordinate of the position to check.
    * @param checkIsInNode   Callback that should check the given
    *                        layer and row for a node and return a
    *                        non-empty optional with the node if it
    *                        is found.
    * @param foundSuccessful Callback that is called with the found
    *                        node once it is verified that the node
    *                        is intersected.
    * @param couldNotFind    Callback that is called when the
    *                        given x and y coordinates are found
    *                        to have no node in them.
    * @return A tuple representing the layer-and row-index,
    *         or (-1, -1) when no node intersected.
    */
  def intersectsNodeAt(x: Double, y: Double,
                       checkIsInNode: (Integer, Integer) => Optional[Node],
                       foundSuccessful: Node => Unit,
                       couldNotFind: () => Unit): Unit = {
    val layer = layerAt(x)
    val xPixels = (layer - layer.floor) * totalWidth

    val row = rowAt(y)
    val yPixels = (row - row.floor) * totalHeight

    var inNode: Optional[Node] = Optional.empty()
    if (x >= 0 && y >= 0
      && 0 <= xPixels && xPixels <= nodeWidth
      && 0 <= yPixels && yPixels <= nodeHeight) {

      inNode = checkIsInNode(layer.floor.toInt, row.floor.toInt)
    }

    if (inNode.isPresent) {
      foundSuccessful(inNode.get())
    } else {
      couldNotFind()
    }
  }

  /**
    * Recalculates the coordinate system based on a new
    * shownLayers after zooming in/out value and the
    * on-screen position at which the zoom was applied.
    *
    * @param shownLayers The number of layers that should
    *                    be shown on-screen.
    * @param x           The x-coordinate at which the zoom
    *                    was applied.
    * @param y           The y-coordinate at which the zoom
    *                    was applied.
    */
  def recalculateZoom(shownLayers: Double, x: Double, y: Double): Unit = {
    val (newTotalWidth, newTotalHeight) = calculateNewGrid(shownLayers)

    val xLayer = layerAt(x)
    val yRow = rowAt(y)

    calculateNodePartsOfGrid(newTotalWidth, newTotalHeight)

    minLayer = xLayer - x / totalWidth
    minRow = yRow - y / totalHeight

    recalculateLayers(shownLayers)
  }

  /**
    * Recalculates the leftLayer value based on the current
    * translation and total layer width.
    *
    * @param shownLayers The number of layers that should be
    *                    shown on-screen.
    */
  def recalculateLayers(shownLayers: Double): Unit =
    this.shownLayers = shownLayers

  /**
    * Returns the total width of a layer, defined by the width
    * of each node plus the padding in between nodes.
    *
    * @return The node width plus padding between nodes.
    */
  def totalWidth: Double = nodeWidth + padding

  /**
    * Returns the total height of a layer, defines by the height
    * of each node plus the height-offset in between nodes.
    *
    * @return The node height plus offset between nodes.
    */
  def totalHeight: Double = nodeHeight + offset

  /**
    * @return The width of the canvas.
    */
  def screenWidth: Double = graphics.getCanvas.getWidth

  /**
    * @return The height of the canvas.
    */
  def screenHeight: Double = graphics.getCanvas.getHeight

  /**
    * Calculates the on-screen x-position corresponding
    * to the given layer.
    *
    * @param layer The layer for which to lookup the
    *              x-position.
    * @return The on-screen x-position of the given layer
    *         as a Double.
    */
  def xForLayer(layer: Double): Double = (layer - _minLayer) * totalWidth

  /**
    * Calculates the on-screen y-position corresponding
    * to the given row.
    *
    * @param row The row for which to lookup the x-position.
    * @return The on-screen y-position of the given row
    *         as a Double.
    */
  def yForRow(row: Double): Double = (row - minRow) * totalHeight

  /**
    * Translates the graph grid by (tx, ty) in the x-and
    * y-direction. Performing this action recalculates the
    * minimum layer and minimum row visible.
    *
    * @param tx The translation in the x-direction in pixels.
    * @param ty The translation in the y-direction in pixels.
    */
  def translate(tx: Double, ty: Double): Unit = {
    minLayer_$eq(minLayer - tx / totalWidth)
    minRow_$eq(minRow - ty / totalHeight)
  }

  /**
    * Calculates the lower-bound layer that should be drawn
    * on-screen. This lower-bound includes a small margin to
    * draw off-screen.
    *
    * @return The lowest layer index to be drawn.
    */
  def lowerBound: Int =
    (minLayer - 1).floor.toInt

  /**
    * Calculates the upper-bound layer that should be drawn
    * on-screen. This upper-bound includes a small margin to
    * draw off-screen.
    *
    * @return The highest layer index to be drawn.
    */
  def upperBound: Int =
    (minLayer + shownLayers + 1).ceil.toInt

  /**
    * Sets the left-most shown layer by calculating it from
    * the intended centre layer.
    *
    * @param centreLayer The intended centre layer.
    */
  def setCentre(centreLayer: Double): Unit =
    minLayer_$eq(centreLayer - (shownLayers / 2.0) + NODE_PART * 0.5)

  /**
    * @return The current minimum layer.
    */
  def minLayer: Double = _minLayer

  /**
    * @return The current minimum row.
    */
  def minRow: Double = _minRow

  /**
    * Sets the minimum layer bounded by the minimum and
    * maximum layers in the graph.
    *
    * @param minLayerIn The value to set the minimum layer
    *                   to.
    */
  def minLayer_$eq(minLayerIn: Double): Unit =
    if (UIHelper.getGraph != null) {
      _minLayer = limit(minLayerIn,
        -screenWidth / totalWidth / 2.0,
        UIHelper.getGraph.getMaxLayer + screenWidth / totalWidth / 2.0)
    } else {
      _minLayer = minLayerIn
    }


  /**
    * Sets the minimum row bounded by the minimum and
    * maximum rows in the graph at the time of calling.
    *
    * @param minRowIn The value to set the minimum row
    *                 to.
    */
  def minRow_$eq(minRowIn: Double): Unit =
    if (UIHelper.getGraph != null) {
      _minRow = limit(minRowIn,
        -screenHeight / totalHeight / 2.0,
        UIHelper.getGraph.getMaxRow - screenHeight / totalHeight / 2.0)
    } else {
      _minRow = minRowIn
    }
}
