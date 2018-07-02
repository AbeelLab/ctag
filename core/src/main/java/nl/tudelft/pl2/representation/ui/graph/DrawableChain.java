package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.GraphicsContext;
import nl.tudelft.pl2.representation.GraphBuilderHelper;

/**
 * Class responsible for drawing a chain. The chain will be drawn as a
 * straight line.
 */
class DrawableChain extends DrawableAbstractNode {

    /**
     * The minimum node width factor to use.
     */
    private static final double MIN_WIDTH_FACTOR = 0.2;

    /**
     * The default value for the arc_width of the node which is drawn. This can
     * be adjusted later when we start zooming etc.
     */
    private static final int ARC_WIDTH = 10;

    /**
     * The minimum number of characters that should fit in the
     * node before displaying the text.
     */
    private static final int MINIMUM_CONTENT_CHARS = 3;


    /**
     * The base of the log to use for node width factor calculation.
     */
    private static final double LOG_BASE = 2;

    /**
     * The maximum length of a sequence to use for calculation
     * of node width factors.
     */
    private static final int MAX_SEQUENCE_LENGTH = 1000;

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
    DrawableChain(final NodeData dataIn, final GenomePainter gp) {
        super(dataIn, gp);
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
     */
    @Override
    void drawNode(final GraphicsContext gc, final boolean thresholdReached) {
        setupForDrawing(gc);

        double startX = getAdjustedStartX();
        double startY = getStartY();

        gc.setLineWidth(1);

        if (!thresholdReached) {
            double lineY = startY + (height() / 2);

            gc.strokeLine(startX, lineY, getAdjustedEndX(), lineY);

            (new GraphBuilderHelper())
                    .createJavaList(getNode().outgoing().toList())
                    .forEach(link -> drawEdge(link, gc));
        }
    }

    /**
     * @return The node width adjusted for content length.
     */
    private double adjustedNodeWidth() {
        return width() * Math.min(1.0,
                Math.max(MIN_WIDTH_FACTOR, nodeWidthFactor()));
    }

    /**
     * @return The factor by which the node width must
     * be multiplied to adjust for content length.
     */
    private double nodeWidthFactor() {
        double a = (LOG_BASE - Math.pow(LOG_BASE, MIN_WIDTH_FACTOR))
                / MAX_SEQUENCE_LENGTH;
        double b = -Math.pow(LOG_BASE, MIN_WIDTH_FACTOR) / a;

        return Math.log(a * (getNode().content().length() - b));
    }
}
