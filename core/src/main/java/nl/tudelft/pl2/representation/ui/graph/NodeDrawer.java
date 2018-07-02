package nl.tudelft.pl2.representation.ui.graph;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import nl.tudelft.pl2.representation.graph.ZoomDirection;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.SelectionHelper;
import nl.tudelft.pl2.representation.ui.TraitHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.heatmap.HeatMapController;
import nl.tudelft.pl2.representation.ui.navigationBar.NavigationBarController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Function0;
import scala.Function1;
import scala.Function2;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.TreeSet;

/**
 * A class which draws a DrawableNode on a Canvas.
 *
 * @author Cedric Willekens
 */
@SuppressWarnings("checkstyle:methodcount")
public class NodeDrawer extends Observable {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER = LogManager.getLogger("NodeDrawer");

    /**
     * Threshold for converting nodes to drawn lines.
     */
    private static final int THRESHOLD = 80;

    /**
     * The number of layers we divide the canvas in by default.
     */
    public static final int SHOW_DEFAULT = 5;

    /**
     * Minimum number of nodes to show at the same time.
     */
    private static final int SHOW_MINIMUM = 1;

    /**
     * The rate at which the graph is zoomed in and
     * out.
     */
    public static final double ZOOM_RATE = 1.047;

    /**
     * The offset off the upper border at which the first
     * row of nodes will be displayed at the start.
     */
    private static final int START_Y_OFFSET = 100;

    /**
     * The part of the current width of the screen to
     * translate each key press.
     */
    private static final double TRANSLATION_PART = 0.03;

    /**
     * The interpolation ratio used to color the edge of the selected trait.
     */
    private final double interpolation = 0.5;

    /**
     * The currently selected segment.
     */
    private Node selectedNode = null;

    /**
     * This variable keeps track of how far the
     * graph has moved before we need to
     * change the center node in the graph.
     * When this value is above or below
     * +/- {@code nodeWidth} / 2 then
     * it is set to the new offset
     * and the center node is updated.
     */
    private int distanceMoved = 0;

    /**
     * The last selected node by the user.
     * This gets updated on every click and
     * will be set to null if not clicked
     * in a node.
     */
    private Node lastSelectedNode;

    /**
     * The data object used in this {@link NodeDrawer}
     * instance to enclose data used in drawing.
     */
    private NodeDrawerData data;

    /**
     * The {@link DrawerUpdater} helper object to help
     * constructing and updating {@link DrawableNode}s.
     */
    private DrawerUpdater updater;

    /**
     * A helper object to support this class and
     * make it more readable.
     */
    private NodeDrawerHelper helper;

    /**
     * The list of observers observing this class.
     */
    private List<Observer> observers = new ArrayList<>();

    /**
     * The constructor for the node drawer class.
     * This object is responsible for managing the nodes
     * which are drawn on the screen and where they are drawn.
     * The nodes drawn on the graph are retrieved form the {@code graph}
     * where the center node for the graph will be drawn in the
     * center of the screen.
     *
     * @param cns This is the canvas on which the nodes are drawn.
     *            From this object also the scaling of the
     *            nodes are retrieved.
     */
    public NodeDrawer(final Canvas cns) {
        super();
        assert cns != null;

        data = new NodeDrawerData(cns.getGraphicsContext2D(),
                new GraphCoordinateSystem(
                        cns.getGraphicsContext2D(), SHOW_DEFAULT));
        updater = new DrawerUpdater(data);

        helper = new NodeDrawerHelper(data.graphicsContext(), this);

        data.shownLayers().addListener((ob, o, n) -> ControllerManager
                .get(NavigationBarController.class)
                .updateZoomSliderFromShownLayers(n));

        updater.setupGraph();
    }

    /**
     * This method clears the whole canvas to white.
     */
    private void cleanCanvas() {
        data.graphicsContext().clearRect(
                -data.graphicsContext().getTransform().getTx(),
                -data.graphicsContext().getTransform().getTy(),
                data.canvas().getWidth(),
                data.canvas().getHeight());
    }

    /**
     * First recalculates the shown graph.
     * And than displays the graph.
     */
    public final void redrawGraph() {
        helper.cleanCanvas();
        updater.setupGraph();
        drawGraph();
    }

    /**
     * First recalculates the shown graph after resizing
     * with the middle of the canvas as point to zoom around.
     * Thereafter, the graph is redrawn on the canvas.
     */
    final void resizeAndRedraw() {
        updater.setupGraphAfterZoom(data.canvas().getWidth() / 2.0,
                data.canvas().getHeight() / 2.0);
        redrawGraph();
    }

    /**
     * This method updates the graph which is being stored.
     *
     * @param newGraph The graph to which the screen
     *                 needs to be updated.
     * @return Whether the graph was a new reference
     */
    public final boolean updateGraph(final GraphHandle newGraph) {
        if (newGraph == null) {
            return false;
        }

        LOGGER.info("Updated graph reference. Old graph: {}. New graph: {}.",
                data.graph(), newGraph);

        boolean newFound = true;
        if (data.graph() == newGraph) {
            newFound = false;
        }
        data.graph_$eq(newGraph);
        data.graph().registerObserver((o, arg) -> {
            if (o instanceof GraphHandle && arg instanceof LoadingState
                    && (arg.equals(LoadingState.FULLY_LOADED)
                    || arg.equals(LoadingState.CHUNK_LOADED)
                    || arg.equals(LoadingState.FULLY_LOADED_GFF))) {
                LOGGER.debug("Redrawing graph because of update");
                redrawGraph();
            }
        });
        redrawGraph();
        return newFound;
    }

    /**
     * Draws all nodes on the canvas.
     */
    private void drawGraph() {
        // Skip empty layer maps.
        if (data.nodesByLayerMap().isEmpty()) {
            return;
        }

        this.data.graphicsContext().setLineDashes(0);
        data.coordinates().recalculateLayers(data.shownLayers().get());

        // After left-most layer recalculation, update the bounds in
        // the GraphHandle.
        if (data.graph() != null) {
            data.graph().updateBounds(data.coordinates().lowerBound(),
                    data.coordinates().upperBound());
        }

        data.graphicsContext().setTextAlign(TextAlignment.CENTER);
        data.graphicsContext().setTextBaseline(VPos.CENTER);

        TraitHelper.clearTree();
        data.nodesByLayerMap().values().stream()
                .flatMap(it -> it.values().stream())
                .forEach((node) -> node.drawNode(data.graphicsContext(),
                        data.shownLayers().get() > THRESHOLD));
        notifyObservers(Drawer.DRAWN);
        ControllerManager.get(HeatMapController.class).drawHeatMap();

        if (data.shownLayers().get() <= THRESHOLD) {
            drawTraitEdges();
        }

    }

    /**
     * Draws all the traits awhich are stored in the drawabletrait map which
     * is stored in {@link #data}.
     */
    private void drawTraitEdges() {
        ArrayList<Integer> sortedList = new ArrayList<>(new TreeSet<>(
                data.drawableTraitTreeMap().keySet()));

        if (sortedList.isEmpty()) {
            return;
        }

        int lowestLayer = sortedList.get(0);
        int highestLayer = sortedList.get(sortedList.size() - 1);

        for (int i = lowestLayer; i <= highestLayer; i++) {
            DrawableTrait trait = data.drawableTraitTreeMap().get(i);
            DrawableTrait nextTrait = null;
            int counter = 1;
            while (nextTrait == null && i + counter <= highestLayer) {
                nextTrait = data.drawableTraitTreeMap().getOrDefault(i
                        + counter, null);
                counter++;
            }

            final int dashedLineWidth = 10;
            if (nextTrait != null && trait != null) {
                Pair<Double, Double> end = nextTrait.getStartCenter();
                Pair<Double, Double> start = trait.getEndCenter();
                Color color = UIHelper.getGraph().getTraitMap().getColors()
                        .get(trait.getLandmark().ty());
                Color lineColor = color.interpolate(Color.BLACK, interpolation);
                this.data.graphicsContext().setStroke(lineColor);
                this.data.graphicsContext().setLineDashes(dashedLineWidth);
                this.data.graphicsContext().setLineWidth(2);
                this.data.graphicsContext().strokeLine(start.getKey(), start
                        .getValue(), end.getKey(), end.getValue());
                this.data.graphicsContext().setLineDashes(0);
                this.data.graphicsContext().setLineWidth(1);
                this.data.graphicsContext().setStroke(Color.BLACK);
            }
        }
    }

    /**
     * Zooms the graph in or out with the middle of the canvas
     * as the reference to zoom around.
     *
     * @param dir   The direction in which to zoom.
     * @param zooms The number of ticks to zoom in/out.
     */
    final void zoomByKey(final ZoomDirection dir,
                         final int zooms) {
        zoomGraph(data.canvas().getWidth() / 2.0,
                data.canvas().getHeight() / 2.0,
                dir, zooms);
    }

    /**
     * Zooms in/out the graph and redraws the canvas afterwards.
     *
     * @param x     The x-coordinate of the position to zoom around.
     * @param y     The y-coordinate of the position to zoom around.
     * @param dir   The direction in which to zoom (in/out).
     * @param zooms The number of zoom ticks to complete.
     */
    final void zoomGraph(final double x,
                         final double y,
                         final ZoomDirection dir,
                         final int zooms) {
        cleanCanvas();
        switch (dir) {
            case IN:
                data.shownLayers().set(Math.max(SHOW_MINIMUM,
                        data.shownLayers().get()
                                / Math.pow(ZOOM_RATE, zooms)));
                break;
            case OUT:
                data.shownLayers().set(Math.min(maxShownLayers(),
                        data.shownLayers().get()
                                * Math.pow(ZOOM_RATE, zooms)));
                break;
            default:
                throw new IllegalArgumentException("Expected ZoomDirection"
                        + " of either 'IN' or 'OUT', not: " + dir);
        }
        if (data.graph() != null) {
            data.graph().updateShownLayers(data.shownLayers().get());
        }
        updater.setupGraphAfterZoom(x, y);
        drawGraph();
    }

    /**
     * Translates the graph by to the left or to the right, depending
     * on the given {@link MoveDirection} for the given number of steps.
     *
     * @param steps The number of steps to translate.
     * @param dir   The direction to move in.
     */
    public final void translateGraphByArrowKey(final int steps,
                                               final MoveDirection dir) {
        switch (dir) {
            case LEFT:
                data.coordinates().translate(
                        data.canvas().getWidth() * TRANSLATION_PART, 0);
                helper.translateGraph(steps, dir, data.graph());
                break;
            case RIGHT:
                data.coordinates().translate(
                        -data.canvas().getWidth() * TRANSLATION_PART, 0);
                helper.translateGraph(steps, dir, data.graph());
                break;
            case UP:
                data.coordinates().translate(
                        0, data.canvas().getHeight() * TRANSLATION_PART);
                break;
            case DOWN:
                data.coordinates().translate(
                        0, -data.canvas().getHeight() * TRANSLATION_PART);
                break;
            default:
                LOGGER.fatal("Unused MoveDirection provided: {}.", dir);
                throw new IllegalArgumentException("Illegal argument: " + dir);
        }

        redrawGraph();
    }

    /**
     * This method translates the graph nodes in a
     * {@code xdir} x and {@code ydir} y direction.
     * The whole graph gets moved and once the
     * center node is out of the layer which is
     * in the center of the screen,
     * this also gets updated in the graph.
     *
     * @param xDir The x value by which the graph should be
     *             translated.
     * @param yDir The x value by which the graph should be
     *             translated.
     */
    final void translateGraph(final int xDir, final int yDir) {
        if (data.graph() == null || data.nodesByLayerMap() == null) {
            return;
        }

        distanceMoved += xDir;
        final double fullWidth = data.coordinates().nodeWidth();
        if (distanceMoved > fullWidth) {
            LOGGER.debug("Movement will be towards the left.");
            helper.translateGraph((int) (distanceMoved / fullWidth),
                    MoveDirection.LEFT, data.graph());

        } else if (distanceMoved < -fullWidth) {
            LOGGER.debug("Movement will be towards the right.");
            helper.translateGraph((int) (-distanceMoved / fullWidth),
                    MoveDirection.RIGHT, data.graph());
        }
        distanceMoved -= (distanceMoved / fullWidth) * fullWidth;
        data.coordinates().translate(xDir, yDir);

        redrawGraph();
    }

    /**
     * Sets the number of layers shown within accepted bounds
     * (SHOW_MINIMUM, SHOW_MAXIMUM) and recalculates the grid
     * for the graph to reflect the new zoom.
     *
     * @param layersShown The number of layers that should be
     *                    shown after setting.
     */
    public final void setLayersShown(final int layersShown) {
        assert layersShown >= SHOW_MINIMUM;
        assert layersShown <= maxShownLayers();

        data.shownLayers().set(Math.min(layersShown, maxShownLayers()));
        data.coordinates().recalculateZoom(data.shownLayers().get(),
                data.coordinates().screenWidth() / 2.0,
                data.coordinates().screenHeight() / 2.0);

        this.redrawGraph();
    }

    /**
     * Creates Runnable Function2 to translate a layer and row to
     * the node in that layer/row if the node actually contains
     * the given mouseX position.
     *
     * @param adjustedMouseX The mouse x-coordinate adjusted for
     *                       graphics context translation.
     * @return The Runnable that can be passed to check nodes at
     * a given layer and row and return whether the given mouse
     * click is contained in them.
     */
    private Function2<Integer, Integer, Optional<Node>> checkIfNodeIsInMapAt(
            final int adjustedMouseX) {
        return (layer, row) -> {
            final DrawableAbstractNode def = data.nodesByLayerMap()
                    .getOrDefault(layer, new HashMap<>())
                    .getOrDefault(row, null);

            return Optional.ofNullable(def)
                    .filter(dan -> !dan.getNode().isDummy())
                    .filter(dan -> dan.getAdjustedStartX() <= adjustedMouseX
                            && adjustedMouseX <= dan.getAdjustedEndX())
                    .map(DrawableAbstractNode::getNode);
        };
    }

    /**
     * Runnable Function1 to select the given node.
     */
    private Function1<Node, BoxedUnit> setSelectedNode = node -> {
        selectedNode = node;
        lastSelectedNode = selectedNode;
        UIHelper.notifyObservers(selectedNode);
        return null;
    };

    /**
     * Runnable Function0 to deselect all selected nodes.
     */
    private Function0<BoxedUnit> deselectAllSelectedNodes = () -> {
        SelectionHelper.clearSelectedNodes();
        lastSelectedNode = null;
        UIHelper.notifyObservers();
        return null;
    };

    /**
     * Takes a x and y coordinate and finds the segment belonging
     * at this coordinate. And sets the selected node as this
     * found node.
     *
     * @param mouseX The x-coordinate over which the mouse hovers.
     * @param mouseY The y-coordinate over which the mouse hovers.
     */
    final void setSegmentAt(final int mouseX, final int mouseY) {
        data.coordinates().intersectsNodeAt(mouseX, mouseY,
                checkIfNodeIsInMapAt(mouseX),
                setSelectedNode,
                deselectAllSelectedNodes);
    }

    /**
     * returns the currently selected node.
     *
     * @return Segment
     */
    final Node getSelectedNode() {
        return this.selectedNode;
    }

    /**
     * Sets the centre node for the graph and for the
     * coordinate system used in this {@link NodeDrawer}.
     *
     * @param node The node to set as the centre.
     */
    public final void setCentre(final Node node) {
        data.graph().setCentre(node);
        data.coordinates().setCentre(node.layer());

        redrawGraph();
    }

    /**
     * Shown layer getter.
     *
     * @return SimpleIntegerProperty
     */
    final SimpleDoubleProperty getShownLayers() {
        return data.shownLayers();
    }

    /**
     * @return The maximum number of shown layers represented as a double.
     */
    public final double maxShownLayers() {
        return 2 * data.coordinates().screenWidth();
    }

    /**
     * @return The canvas from which the used
     * {@link javafx.scene.canvas.GraphicsContext} is derived.
     */
    public final Canvas getCanvas() {
        return data.graphicsContext().getCanvas();
    }

    /**
     * Sets the minlayer and adjusts to a zoom level for the user to see.
     *
     * @param minLayer The minLayer that the coordinate system should be set to.
     * @param minRow   The minRow that the coordinate system should be set to.
     * @param zoom     The amount of layers which are visible to the user.
     */
    public final void goToTranslationWithZoom(final double minLayer,
                                              final double minRow,
                                              final double zoom) {
        data.shownLayers().set(Math.min(zoom, maxShownLayers()));
        resizeAndRedraw();

        data.coordinates().minLayer_$eq(minLayer);
        data.coordinates().minRow_$eq(minRow);

        redrawGraph();
    }

    /**
     * @return The zoom layer currently in use by the node drawer.
     */
    public final double getZoomLayer() {
        return data.shownLayers().get();
    }

    /**
     * @return The minimum layer currently used by the coordinates
     * object of the node drawer.
     */
    public final double getMinLayer() {
        return data.coordinates().minLayer();
    }

    /**
     * @return The minimum row currently used by the coordinates
     * object of the node drawer.
     */
    public final double getMinRow() {
        return data.coordinates().minRow();
    }

    /**
     * Set the painter of the nodeDrawer.
     *
     * @param painter The painter of the graph that this drawer belongs to
     */
    public final void setPainter(final GenomePainter painter) {
        updater.setPainter(painter);
    }

    /**
     * Get the last selected node of the drawer.
     *
     * @return The last selected node
     */
    final Node getLastSelectedNode() {
        return lastSelectedNode;
    }


    /**
     * This method gets the minimum and maximum layer which is currently
     * displayed on the screen.
     *
     * @return A pair with the first element the minimum layer
     * and the second element the maximum layer.
     */
    public final Pair<Double, Double> getMinMaxLayer() {
        return new Pair<>(data.coordinates().minLayer(),
                data.shownLayers().add(data.coordinates().minLayer()).get());
    }

    /**
     * This method adds an observer to the NodeDrawer.
     *
     * @param observer The object which is observing
     *                 this class.
     */
    public final void addObserver(final Observer observer) {
        this.observers.add(observer);
    }

    /**
     * This method causes the observers to be notified
     * off any variable.
     *
     * @param arg an object
     */
    public final void notifyObservers(final Object arg) {
        observers.forEach(observer -> observer.update(this, arg));
    }

    /**
     * @return The {@link DrawerUpdater} used to update node
     * representations for this {@link NodeDrawer}.
     */
    public final DrawerUpdater getUpdater() {
        return updater;
    }

    /**
     * Resets the {@link NodeDrawer} without adjusting the
     * coordinate system.
     */
    public final void reset() {
        TraitHelper.clearTree();
        data.nodesByNameMap().clear();
        data.nodesByLayerMap().clear();
        data.layersLoaded().clear();

        updater.setupGraph();
    }
}

