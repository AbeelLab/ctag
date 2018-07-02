package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import nl.tudelft.pl2.data.gff.Landmark;
import nl.tudelft.pl2.data.gff.Trait;
import nl.tudelft.pl2.representation.ui.TraitHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;

/**
 * This class is responsible for drawing traits under a node.
 */
public class DrawableTrait {

    /**
     * The default value for the arc_width of the node which is drawn. This can
     * be adjusted later when we start zooming etc.
     */
    private static final int ARC_WIDTH = 10;

    /**
     * The distance between the trait and the node to which this trait belongs.
     */
    private static final double GFF_DISTANCE_RATE = 0.1;

    /**
     * The height ratio of gff traits compared to normal nodes.
     */
    private static final double GFF_HEIGHT_RATIO = 0.3;

    /**
     * The offset between the traits.
     */
    private static final double GFF_OFFSET = 0.1;

    /**
     * The interpolation ratio for interpolating the edge color for traits.
     */
    private final double interpolation = 0.5;

    /**
     * The landmark to which this drawable trait belongs.
     */
    private Landmark landmark;

    /**
     * The trait to which this drawable trait belongs.
     */
    private Trait trait;

    /**
     * The center x, y coordinates at the start of the trait.
     */
    private Pair<Double, Double> startCenter;

    /**
     * The center x, y coordinates at the end of the trait.
     */
    private Pair<Double, Double> endCenter;

    /**
     * Constructor for a drawabletrait.
     *
     * @param landmarkIn The landmark belonging to this drawabletrait.
     * @param trt        The trait belonging to this drawabletrait.
     */
    DrawableTrait(final Landmark landmarkIn,
                  final Trait trt) {
        this.landmark = landmarkIn;
        this.trait = trt;
    }

    /**
     * @return The trait associated with this {@link DrawableTrait}.
     */
    public final Trait getTrait() {
        return trait;
    }

    /**
     * @return The landmark associated with this {@link DrawableTrait}.
     */
    public final Landmark getLandmark() {
        return landmark;
    }

    /**
     * This draws a trait under the node.
     *
     * @param gc        The graphics context with which this
     *                  trait needs to be drawn.
     * @param index     The number of other traits which are
     *                  above this trait.
     * @param node      The node to which this trait belongs.
     * @param partLeft  The left intersection part.
     * @param partRight The right intersection part.
     */
    final void draw(final GraphicsContext gc,
                    final int index,
                    final DrawableNode node,
                    final Double partLeft,
                    final Double partRight) {
        double startX = partLeft * node.adjustedNodeWidth()
                + node.getAdjustedStartX();
        double endX = partRight * node.adjustedNodeWidth()
                + node.getAdjustedStartX();

        int height = (int) (node.height() * GFF_HEIGHT_RATIO);
        int startY = (int) (node.getStartY() + node.height() + GFF_OFFSET
                * node.height() + ((height + GFF_DISTANCE_RATE * height)
                * index));

        Color color = UIHelper.getGraph().getTraitMap().getColors()
                .get(landmark.ty());

        gc.setFill(color);
        gc.fillRoundRect(startX, startY, endX - startX, height,
                ARC_WIDTH, ARC_WIDTH);

        this.startCenter = new Pair<>(startX, startY + height / 2.0);
        this.endCenter = new Pair<>(endX, startY + height / 2.0);

        if (TraitHelper.getSelectedTrait() != null
                && trait.equals(TraitHelper.getSelectedTrait().trait)) {
            Color lineColor = color.interpolate(Color.BLACK, interpolation);
            gc.setStroke(lineColor);
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(startX, startY, endX - startX, height,
                    ARC_WIDTH, ARC_WIDTH);
        }

        TraitHelper.add(this, (int) startX,
                startY, (int) endX, startY + height);
        gc.setStroke(Color.BLACK);
    }

    /**
     * @return The {@link #startCenter}
     */
    final Pair<Double, Double> getStartCenter() {
        return startCenter;
    }

    /**
     * @return The {@link #endCenter}
     */
    final Pair<Double, Double> getEndCenter() {
        return endCenter;
    }
}
