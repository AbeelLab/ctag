package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import nl.tudelft.pl2.data.IntervalTreeMap;
import nl.tudelft.pl2.data.gff.Landmark;
import nl.tudelft.pl2.data.gff.Trait;
import nl.tudelft.pl2.data.gff.TraitMap;
import nl.tudelft.pl2.representation.GraphBuilderHelper;
import nl.tudelft.pl2.representation.external.IntegerInterval;
import nl.tudelft.pl2.representation.ui.SelectionHelper;
import nl.tudelft.pl2.representation.ui.TraitHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import scala.Option;
import scala.Tuple2;
import scala.collection.JavaConverters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class represents a node which will be drawn on the canvas.
 * The location where it is drawn is stored in the attributes.
 *
 * @author Cedric Willekens
 */
class DrawableNode extends DrawableAbstractNode {

    /**
     * The default value for the arc_width of the node which is drawn. This can
     * be adjusted later when we start zooming etc.
     */
    private static final int ARC_WIDTH = 10;

    /**
     * Approximately the number of pixels one character occupies on the screen.
     */
    private static final int PIXELS_PER_CHAR = 10;


    /**
     * The minimum number of characters that should fit in the
     * node before displaying the text.
     */
    private static final int MINIMUM_CONTENT_CHARS = 3;

    /**
     * The maximum length of a sequence to use for calculation
     * of node width factors.
     */
    private static final int MAX_SEQUENCE_LENGTH = 1000;

    /**
     * The base of the log to use for node width factor calculation.
     */
    private static final double LOG_BASE = 2;

    /**
     * The minimum node width factor to use.
     */
    private static final double MIN_WIDTH_FACTOR = 0.2;

    /**
     * The map used to store which trait is selected in which layer.
     */
    private TreeMap<Integer, DrawableTrait> drawableTraitMap;

    /**
     * The constructor for a new node.
     *
     * @param dataIn {@link NodeData} This data class contains
     *               all the information which is needed to
     *               construct a node.
     * @param gp     The painter that has the color of the node
     * @param traitMap The map used to store all the traits which are
     *                 selected.
     */
    DrawableNode(final NodeData dataIn, final GenomePainter gp,
                 final TreeMap<Integer, DrawableTrait> traitMap) {
        super(dataIn, gp);
        this.drawableTraitMap = traitMap;
    }


    /**
     * Constructs a new node and chooses whether to have that
     * be a {@link DrawableDummyNode} or {@link DrawableNode}.
     *
     * @param dataIn {@link NodeData} The data class that contains
     *               all the information which is needed to
     *               construct a node.
     * @param gp     The painter that has the color of the node
     * @param drawableTraitMap The map used to store the triats which are
     *                         selected and in which layer they are.
     * @return New instance of a {@link DrawableNode} orø
     * {@link DrawableDummyNode}.
     */
    public static DrawableAbstractNode from(final NodeData dataIn,
                                            final GenomePainter gp,
                                            final TreeMap<Integer,
                                                    DrawableTrait>
                                                    drawableTraitMap) {
        if (dataIn.chunk().isDummy()) {
            return new DrawableDummyNode(dataIn, gp);
        } else {
            return new DrawableNode(dataIn, gp, drawableTraitMap);
        }
    }

    /**
     * Split one value into equal parts.
     *
     * @param whole The number that should be split
     * @param parts number of equal parts
     * @return list of equally split parts.
     */
    private static int[] splitIntoParts(final int whole, final int parts) {
        int[] arr = new int[parts];
        int remain = whole;
        int partsLeft = parts;
        for (int i = 0; partsLeft > 0; i++) {
            int size = (remain + partsLeft - 1) / partsLeft;
            arr[i] = size;
            remain -= size;
            partsLeft--;
        }
        return arr;
    }


    @Override
    void drawNode(final GraphicsContext gc,
                  final boolean thresholdReached) {
        setupForDrawing(gc);

        double startX = getAdjustedStartX();
        double startY = getStartY();
        double width = adjustedNodeWidth();

        if (thresholdReached) {
            gc.strokeLine(startX + width() / 2, startY,
                    startX + width() / 2, startY + height());
        } else {
            if (SelectionHelper.getSelectedNodes().contains(getNode().id())) {
                gc.setStroke(Color.web("#F44336"));
            }

            gc.strokeRoundRect(startX, startY,
                    width, height(),
                    ARC_WIDTH, ARC_WIDTH);

            String content = getNode().content();
            Set<Color> color;
            if (getGenomePainter() != null) {
                color = getGenomePainter().getColorById(getNode().id());
            } else {
                Set<Color> cl = new HashSet<>();
                cl.add(Color.web("#f4f4f4"));
                color = cl;
            }

            colorNode(gc, startX, startY, color, width);

            gc.setFill(Color.BLACK);

            if (Math.min(content.length(), MINIMUM_CONTENT_CHARS)
                    * PIXELS_PER_CHAR >= width) {
                content = "";
            } else if (content.length() * PIXELS_PER_CHAR >= width) {
                int charCount = (int) (width / PIXELS_PER_CHAR);
                content = content.substring(0, charCount) + "...";
            }

            gc.fillText(content, startX + width / 2,
                    startY + height() / 2);

            gc.setStroke(Color.BLACK);

            TraitMap traitMap = UIHelper.getGraph().getTraitMap();
            if (traitMap != null && !traitMap.isEmpty()) {
                drawTraitMap(traitMap, gc);
            }

            (new GraphBuilderHelper())
                    .createJavaList(getNode().outgoing().toList())
                    .forEach(link -> drawEdge(link, gc));
        }
    }

    /**
     * Draws a trait given an intersection with the given
     * {@link GraphicsContext} and coordinates.
     *
     * @param gc          The {@link GraphicsContext} to draw on.
     * @param counter     The counter that should be incremented
     *                    to keep track of trait index.
     * @param coordinates The genome coordinates of this node.
     * @param tuple       The tuple of {@link Landmark} and {@link Trait}
     *                    that is supposed to be drawn.
     */
    private void drawTrait(final GraphicsContext gc,
                           final int[] counter,
                           final IntegerInterval coordinates,
                           final Tuple2<Landmark, Trait> tuple) {
        coordinates.intersectionWith(tuple._2.start(), tuple._2.end())
                .map(intersection -> intersection.asPartOf(coordinates))
                .foreach(partOf -> {
                    DrawableTrait drawableTrait =
                            new DrawableTrait(tuple._1, tuple._2);
                    HashSet<Trait> filteredTraits =
                            TraitHelper.getFilteredTraits();

                    if (TraitHelper.getSelectedTrait() != null && drawableTrait
                            .getTrait().equals(TraitHelper
                                    .getSelectedTrait().getTrait())) {
                        this.drawableTraitMap.put(super.getData().layer(),
                                drawableTrait);
                    }

                    if (!(filteredTraits == null)
                            && !(filteredTraits.isEmpty())
                            && !filteredTraits.contains(
                                    drawableTrait.getTrait())) {
                        return null;
                    }

                    drawableTrait.draw(gc, counter[0]++, this,
                            (double) partOf._1(),
                            (double) partOf._2());

                    return null;
                });
    }

    /**
     * This method draws a traitmap belonging to the node. It first looks for
     * the correct traits and then draws them under the node by creating a
     * drawable triat.
     *
     * @param traitMap The traitmap which needs to be drawn.
     * @param gc       The graphicscontext with which needs to be drawn.
     */
    private void drawTraitMap(final TraitMap traitMap,
                              final GraphicsContext gc) {
        Map<Integer, Object> genomeIndexes = JavaConverters
                .mapAsJavaMap(getNode().genomeCoordinates());

        int[] counter = new int[]{0};

        genomeIndexes.forEach((index, coordinate) -> {
            String genome = UIHelper.getGraph().getGenomes()[index];

            Option<IntervalTreeMap<Long, Tuple2<Landmark, Trait>>> map =
                    traitMap.get(genome);
            if (map.isEmpty() && genome.contains(".")) {
                map = traitMap.get(genome.substring(0,
                        genome.lastIndexOf('.')));
            }

            if (!map.isEmpty()) {
                IntegerInterval coordinates = new IntegerInterval(
                        (long) coordinate,
                        (long) coordinate + getNode().content().length());

                map.get().valuesIntersecting(coordinates).foreach(tuple -> {
                    drawTrait(gc, counter, coordinates, tuple);
                    return null;
                });
            }
        });
    }

    /**
     * Color a node with all the different colors in the colormap.
     *
     * @param gc     GraphicsContext
     * @param startX starting x of node
     * @param startY starting y of node
     * @param colors the colors
     * @param width  The width of the fill
     */
    private void colorNode(final GraphicsContext gc,
                           final double startX,
                           final double startY,
                           final Set<Color> colors,
                           final double width) {
        double barHeight = height() / colors.size();
        double currentY = startY;
        Iterator<Color> it = colors.iterator();
        for (int i = 0; i < colors.size(); i++) {
            if (it.hasNext()) {
                gc.setFill(it.next());
            }
            gc.fillRoundRect(startX, currentY,
                    width, barHeight, ARC_WIDTH, ARC_WIDTH);
            currentY += barHeight;
        }
    }


    /**
     * Calculates the adjusted left x-coordinate.
     * An adjusted x-coordinate makes up for dummy-prettification.
     *
     * @return The prettified left x-coordinate.
     */
    double getAdjustedStartX() {
        return (int) (getStartX() + adjustedWidthOffset());
    }

    @Override
    double getAdjustedEndX() {
        return (int) (getEndX() - adjustedWidthOffset());
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

    /**
     * @return The node width adjusted for content length.
     */
    double adjustedNodeWidth() {
        return width() * Math.min(1.0,
                Math.max(MIN_WIDTH_FACTOR, nodeWidthFactor()));
    }

    /**
     * @return The offset from the layer-border at which the
     * node should be drawn.
     */
    private double adjustedWidthOffset() {
        return (width() - adjustedNodeWidth()) / 2.0;
    }

}
