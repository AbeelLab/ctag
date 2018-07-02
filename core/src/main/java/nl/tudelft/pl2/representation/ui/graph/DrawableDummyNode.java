package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.GraphicsContext;
import nl.tudelft.pl2.representation.GraphBuilderHelper;
import nl.tudelft.pl2.representation.external.components.DummyNode;

/**
 * {@link DrawableNode} for dummy nodes to have drawing happen
 * in a more specialized, more manageable manner.
 */
class DrawableDummyNode extends DrawableAbstractNode {

    /**
     * The fraction of the width of a dummy that may be used to make the curves
     * of incoming and outgoing edges of dummy nodes more smooth.
     */
    private static final double DUMMY_PRETTIFICATION_PART = 0.1;

    /**
     * The {@link DummyNode} which this {@link DrawableNode}
     * is representing.
     */
    private DummyNode node;

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
    DrawableDummyNode(final NodeData dataIn,
                      final GenomePainter gp) {
        super(dataIn, gp);

        node = (DummyNode) dataIn.chunk();
    }

    @Override
    final void drawNode(final GraphicsContext gc,
                        final boolean thresholdReached) {
        setupForDrawing(gc);

        double startX = getAdjustedStartX();
        double startY = getStartY();

        gc.setLineWidth(node.getWidth());

        if (!thresholdReached) {
            double lineY = startY + (height() / 2);

            gc.strokeLine(startX, lineY, getAdjustedEndX(), lineY);

            (new GraphBuilderHelper())
                    .createJavaList(node.outgoing().toList())
                    .forEach(link -> drawEdge(link, gc));
        }
    }


    @Override
    final double getAdjustedStartX() {
        return (int) (getStartX() + width() * DUMMY_PRETTIFICATION_PART);
    }

    @Override
    final double getAdjustedEndX() {
        return (int) (getEndX() - width() * DUMMY_PRETTIFICATION_PART);
    }

}
