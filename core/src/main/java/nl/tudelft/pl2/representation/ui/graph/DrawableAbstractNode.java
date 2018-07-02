package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import nl.tudelft.pl2.representation.external.Edge;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.external.components.DummyNode;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.ui.InfoSidePanel.SampleSelectionController;
import nl.tudelft.pl2.representation.ui.SelectionHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * The abstract class which represents all drawable nodes.
 *
 * @author Cedric Willekens
 */
abstract class DrawableAbstractNode {

    /**
     * Max width of an edge in the graph.
     */
    private static final double MAX_LINE_WIDTH = 4;

    /**
     * Min width of an edge in the graph.
     */
    private static final double MIN_LINE_WIDTH = 0.5;

    /**
     * The amount of pixels present per char.
     */
    static final int PIXELS_PER_CHAR = 10;

    /**
     * The {@link GraphCoordinateSystem} with which this
     * {@link DrawableNode} will be drawn.
     */
    private GraphCoordinateSystem coordinates;

    /**
     * This maps the id of a segment to their respective
     * {@link DrawableNode} representation in order to be able to draw
     * the edges  between two nodes.
     */
    private Map<Integer, DrawableAbstractNode> otherNodes;

    /**
     * The graphHandle the node belongs to.
     */
    private GraphHandle graphHandle;

    /**
     * The data object representing the data needed to be
     * present and passed to this {@link DrawableNode}.
     */
    private NodeData data;

    /**
     * Painter that contains the color of the node.
     */
    private GenomePainter genomePainter;

    /**
     * The {@link Node} which this {@link DrawableNode}
     * is representing.
     */
    private Node node;

    /**
     * The constructor for a new node.
     *
     * @param dataIn {@link NodeData} This data class contains
     *               all the information which is needed to
     *               construct a node. This includes a {@link DrawableNode}
     *               as well as the the start x and y coordinates
     *               together with a map which maps the name of the
     *               {@link DrawableNode} to the {@link DrawableNode}
     *               representation of this {@link DrawableNode}
     * @param gp     The painter that has the color of the node
     */
    DrawableAbstractNode(final NodeData dataIn, final GenomePainter gp) {
        this.data = dataIn;
        this.node = dataIn.chunk();
        this.coordinates = dataIn.coordinates();
        this.otherNodes = dataIn.nodes();

        this.genomePainter = gp;
        this.graphHandle = gp.getGraphHandle();
    }

    /**
     * Returns {@link Node}.
     *
     * @return The {@link Node}.
     */
    final synchronized Node getNode() {
        return node;
    }

    /**
     * This is a helper method which will draw an edge which is represented
     * by the {@code link}.
     *
     * @param link This is the link representation
     *             that will be drawn on the screen.
     * @param gc   This is the graphics context which
     */
    void drawEdge(final Edge link,
                  final GraphicsContext gc) {
        double edgeStartX = getAdjustedEndX();
        double edgeStartY = getStartY() + (height() / 2);

        DrawableAbstractNode otherNode = otherNodes.get(link.to());

        double edgeFinishX = edgeStartX;
        double edgeFinishY = edgeStartY;

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);

        if (otherNode != null) {
            gc.setLineWidth(calculateEdgeWidth(node, otherNode.node));

            edgeFinishX = otherNode.getAdjustedStartX();
            edgeFinishY = otherNode.getStartY() + (height() / 2);

            if (SelectionHelper.getSelectedNodes().contains(node.id())
                    && SelectionHelper.getSelectedNodes()
                    .contains(otherNode.node.id())) {
                gc.setStroke(Color.YELLOWGREEN);
            }
        }

        double halfX = (edgeFinishX + edgeStartX) / 2.0;

        gc.beginPath();
        gc.moveTo(edgeStartX, edgeStartY);
        gc.bezierCurveTo(halfX, edgeStartY,
                halfX, edgeFinishY, edgeFinishX, edgeFinishY);
        gc.stroke();
        gc.setStroke(Color.BLACK);
    }

    /**
     * This causes the node to be drawn when called.
     * It will cause the node to be drawn as well as the outgoing
     * edges to other nodes or else it will draw of the screen.
     *
     * @param gc               This graphics context will be used for
     *                         drawing the node.
     * @param thresholdReached Whether the threshold is reached
     *                         that states nodes will be represented
     *                         by lines instead of rectangles.
     */
    abstract void drawNode(GraphicsContext gc,
                           boolean thresholdReached);

    /**
     * Calculate the weight of and edge between the given segments.
     *
     * @param fromNode The segment the edge comes from
     * @param toNode   The segment the edge goes to
     * @return The width of the edge
     */
    double calculateEdgeWidth(final Node fromNode,
                              final Node toNode) {
        Map<String, String> fromOptions = fromNode.getOptions();
        Map<String, String> toOptions = toNode.getOptions();

        if (!fromOptions.containsKey(SampleSelectionController.GENOME_TAG)
                || !toOptions.containsKey(
                SampleSelectionController.GENOME_TAG)) {
            return 1.0;
        }

        String[] fromGenomesArray =
                fromOptions.get(SampleSelectionController.GENOME_TAG)
                        .split(";");
        String[] toGenomesArray =
                toOptions.get(SampleSelectionController.GENOME_TAG).split(";");

        HashSet<String> intersect =
                new HashSet<>(Arrays.asList(fromGenomesArray));

        intersect.retainAll(Arrays.asList(toGenomesArray));

        double iSize = Math.log(intersect.size());

        double maxWidth = Math.log(graphHandle.genomeCount());

        double width = (iSize / maxWidth)
                * (MAX_LINE_WIDTH - MIN_LINE_WIDTH) + MIN_LINE_WIDTH;

        if (node.isDummy()) {
            ((DummyNode) node).setWidth(width);
        }

        return width;
    }

    @Override
    public final boolean equals(final Object other) {
        if (other instanceof DrawableAbstractNode) {
            DrawableNode otherDrawableNode = (DrawableNode) other;
            return data.equals(otherDrawableNode.getData());
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return data.hashCode();
    }

    /**
     * Sets up the graphics context for drawing with the right
     * stroke color, filling color, line width etc.
     *
     * @param gc The {@link GraphicsContext} that should be
     *           prepared for drawing.
     */
    final void setupForDrawing(final GraphicsContext gc) {
        gc.setLineWidth(1.0);
        gc.setFill(Color.BLACK);

    }

    /**
     * @return The width of this node in pixels on-screen.
     */
    double width() {
        return coordinates.nodeWidth();
    }

    /**
     * @return The height of this node in pixels on-screen.
     */
    double height() {
        return coordinates.nodeHeight();
    }

    /**
     * @return The coordinate system used by the canvas to draw
     * nodes.
     */
    GraphCoordinateSystem getCoordinates() {
        return this.coordinates;
    }

    /**
     * @return The node which this drawable node is representing.
     */
    NodeData getData() {
        return data;
    }

    /**
     * @return The genome painter used by the node in order
     * to color the genomes.
     */
    GenomePainter getGenomePainter() {
        return genomePainter;
    }

    /**
     * @return The starting x coordinate of the node.
     */
    double getStartX() {
        return coordinates.xForLayer(data.layer());
    }

    /**
     * @return The starting y coordinate of the node.
     */
    double getStartY() {
        return coordinates.yForRow(data.row());
    }

    /**
     * @return The maximum x coordinate the node is taking up.
     */
    double getEndX() {
        return getStartX() + width();
    }

    /**
     * Calculates the adjusted left x-coordinate.
     * An adjusted x-coordinate makes up for dummy-prettification.
     *
     * @return The prettified left x-coordinate.
     */
    double getAdjustedStartX() {
        return getStartX();
    }

    /**
     * Calculates the adjusted right x-coordinate.
     * An adjusted x-coordinate makes up for dummy-prettification.
     *
     * @return The prettified right x-coordinate.
     */
    double getAdjustedEndX() {
        return getEndX();
    }
}
