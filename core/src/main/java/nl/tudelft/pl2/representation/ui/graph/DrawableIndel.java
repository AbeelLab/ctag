package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import nl.tudelft.pl2.representation.GraphBuilderHelper;

/**
 * The class represents an indel which needs to be drawn on the screen.
 *
 * @author Cedric Willekens
 */
class DrawableIndel extends DrawableAbstractNode {

    /**
     * The number of corners present in a triangle.
     */
    private static final int NUMBER_OF_CORNERS_TRIANGLE = 3;

    /**
     * The content which needs to go into the indel.
     */
    private String content;


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
     * @param top    The string which needs to be displayed in the top.
     */
    DrawableIndel(final NodeData dataIn, final GenomePainter gp,
                  final String top) {
        super(dataIn, gp);
        this.content = top;
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
        gc.setLineWidth(1.0);
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.BLACK);

        double startX = getStartX();
        double lineY = getStartY() + (height() / 2);

        double[] xCoordinates = new double[]{
                startX, startX + (width() / 2),
                getEndX()
        };

        double[] yCoordinates = new double[]{
                lineY, lineY - (height() / 2),
                lineY
        };
        gc.strokePolygon(xCoordinates, yCoordinates,
                NUMBER_OF_CORNERS_TRIANGLE);

        double[] xFillTop = new double[] {
                startX, startX + (width() / 2),
                startX + (width())
        };

        double[] yFillTop = new double[]{
                lineY, lineY - (height() / 2),
                lineY
        };

        gc.setFill(Color.YELLOW);
        gc.fillPolygon(xFillTop, yFillTop, NUMBER_OF_CORNERS_TRIANGLE);
        String drawContent = "";
        if (content.length() * PIXELS_PER_CHAR >= width() / 2) {
            int charCount = (int) (width() / 2.0) / PIXELS_PER_CHAR;
            drawContent = content.substring(0, charCount) + "...";
        }

        final int heightRatio = 4;

        gc.strokeText(String.valueOf(drawContent), startX + (width() / 2),
                lineY - (height() / heightRatio));

        if (!thresholdReached) {
            (new GraphBuilderHelper())
                    .createJavaList(getNode().outgoing().toList())
                    .forEach(link -> drawEdge(link, gc));
        }
    }

}
