package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import nl.tudelft.pl2.representation.GraphBuilderHelper;

/**
 * The class represents a bubble which needs to be drawn on the canvas.
 *
 * @author Cedric Willekens
 */
class DrawableBubble extends DrawableAbstractNode {

    /**
     * The number of corners present in a diamond shape.
     */
    private static final int NUMBER_OF_CORNERS_DIAMOND = 4;

    /**
     * The number of corners present in a triangle.
     */
    private static final int NUMBER_OF_CORNERS_TRIANGLE = 3;

    /**
     * The character which needs to be displayed in the top triangle
     * of the bubble.
     */
    private char topChar;

    /**
     * The character which needs to be displayed in the bottom triangle.
     */
    private char bottomChar;

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
     * @param top    The character which needs to be displayed in the top.
     * @param bottom The character which needs to be displayed in the bottom.
     */
    DrawableBubble(final NodeData dataIn, final GenomePainter gp,
                   final char top, final char bottom) {
        super(dataIn, gp);
        this.topChar = top;
        this.bottomChar = bottom;
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
        double startY = getStartY() + (height() / 2);

        double[] xCoordinates = new double[]{
                startX, startX + (width() / 2),
                getEndX(), startX + (width() / 2)
        };

        double[] yCoordinates = new double[]{
                startY, startY + (height() / 2),
                startY, startY - (height() / 2)
        };
        gc.strokePolygon(xCoordinates, yCoordinates, NUMBER_OF_CORNERS_DIAMOND);

        fillNode(gc, startX, startY);

        char drawTop = topChar;
        char drawBottom = bottomChar;
        if (PIXELS_PER_CHAR >= width() / 2) {
            drawTop = ' ';
            drawBottom = ' ';
        }
        gc.strokeText(String.valueOf(drawTop), startX + (width() / 2),
                startY - (height() / NUMBER_OF_CORNERS_DIAMOND));

        gc.strokeText(String.valueOf(drawBottom), startX + (width() / 2),
                startY + (height() / NUMBER_OF_CORNERS_DIAMOND));

        gc.strokeLine(startX, startY, getEndX(), startY);

        if (!thresholdReached) {
            (new GraphBuilderHelper())
                    .createJavaList(getNode().outgoing().toList())
                    .forEach(link -> drawEdge(link, gc));
        }
    }

    /**
     * This method colors the node depending on content of the bubble,
     * it will give the bubble a different color.
     *
     * @param gc     The {@link GraphicsContext} with which we draw
     *               on the canvas.
     * @param startX The starting x location of the node.
     * @param startY The starting y location of the node.
     */
    private void fillNode(final GraphicsContext gc,
                          final double startX,
                          final double startY) {
        double[] xFillTop = new double[] {
                startX, startX + (width() / 2),
                startX + (width())
        };

        double[] yFillTop = new double[]{
                startY, startY + (height() / 2),
                startY
        };

        gc.setFill(chooseColor(bottomChar));
        gc.fillPolygon(xFillTop, yFillTop, NUMBER_OF_CORNERS_TRIANGLE);

        double[] xFillBottom = new double[] {
                startX, startX + (width() / 2),
                startX + (width())
        };

        double[] yFillBottom = new double[]{
                startY, startY - (height() / 2),
                startY
        };

        gc.setFill(chooseColor(topChar));
        gc.fillPolygon(xFillBottom, yFillBottom, NUMBER_OF_CORNERS_TRIANGLE);
    }


    /**
     * Depeding on the input to this method it will pick a different color
     * in order to fill thee triangle with.
     * @param content The character which is displayed in the bubble.
     * @return The color which is used to color the bubble depending on the
     * character in the bubble.
     */
    private Color chooseColor(final char content) {
        switch (content) {
            case 'A': return Color.RED;
            case 'C': return Color.ORANGE;
            case 'T': return Color.AQUA;
            case 'G': return Color.IVORY;
            default: return Color.WHITE;
        }
    }
}
