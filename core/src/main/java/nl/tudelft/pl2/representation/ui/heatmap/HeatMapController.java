package nl.tudelft.pl2.representation.ui.heatmap;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import nl.tudelft.pl2.data.storage.HeatMap;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.kernels.NormalKernel;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.graph.Drawer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;

/**
 * Responsible for the drawing of the heatmap
 * on the screen.
 *
 * @author Cedric Willekens
 */
public class HeatMapController extends Controller implements Observer {

    /**
     * The logger used by this class.
     */
    private static final Logger LOGGER
            = LogManager.getLogger("HeatMapController");

    /**
     * The radius used for the average kernel.
     */
    private static final int KERNEL_RADIUS = 10;
    /**
     * The number of pixels to move before we consider a mouse click
     * to represent dragging the screen.
     */
    private static final int DRAG_THRESHOLD = KERNEL_RADIUS;


    /**
     * The division of sequence spectrum in order to color them.
     */
    private static final double COLOR_DEVISION = 3.0;

    /**
     * The multiplier applied to the scaling factor
     * so that they become less high.
     */
    private static final double HEIGHT_MULTIPLIER = 0.80;

    /**
     * The original x and y coordinates which are being set once
     * a user starts dragging.
     */
    private double origX;

    /**
     * The amount of layers one pixel renders.
     * This is dependent on the ratio between the
     * number of layers and the number of pixels in the
     * X-direction the canvas has.
     */
    private double layersPerPixel;

    /**
     * Whether the last click is a dragging click.
     */
    private Boolean isDragged = false;

    /**
     * The heat-map used by the controller.
     */
    private HeatMap heatmap;

    /**
     * The map which contains the information regarding the
     * heatMapMap after applying the kernel.
     */
    private TreeMap<Integer, Double> heatMapMap;

    /**
     * The map which contains the amount of sequences in a layer
     * after applying the kernel.
     */
    private TreeMap<Integer, Double> avgMap;

    /**
     * The pane which contains the canvas.
     */
    @FXML
    private AnchorPane heatMapPane;

    /**
     * The canvas on which everything gets drawn.
     */
    @FXML
    private Canvas heatMapCanvas;


    /**
     * Initializes the private JavaFX context. This
     * is to initialize parameters for JavaFX elements,
     * adding event listeners to the elements, etc.
     */
    @Override
    public final void initializeFxml() {
        UIHelper.addObserver(this);

        heatMapCanvas.widthProperty().bind(heatMapPane.widthProperty());
        heatMapCanvas.widthProperty().addListener(
                (obs1, newVale1, oldVal1) -> updateHeatMap());

        heatMapCanvas.heightProperty().bind(heatMapPane.heightProperty());
        heatMapCanvas.widthProperty().addListener(
                (obs, newVale, oldVal) -> updateHeatMap());
    }


    @Override
    public final Parent getWindow() {
        return heatMapPane.getParent();
    }


    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     */
    @Override
    public final void update(final Observable o, final Object arg) {
        if (arg instanceof GraphHandle) {
            LOGGER.debug("New graph is starting to load");
            ((GraphHandle) arg).registerObserver((obj, args) -> {
                if (obj instanceof GraphHandle
                        && LoadingState.FULLY_LOADED.equals(args)) {

                    this.heatmap = ((GraphHandle) obj).getHeatMap();
                    LOGGER.debug("The graph was fully loaded in the heatmap");
                    updateHeatMap();
                }
            });
        } else if (Drawer.DRAWN.equals(arg)) {
            drawHeatMap();
        }
    }

    /**
     * Recalculates the kernel maps and redraws the heat-map as well.
     *
     */
    private void updateHeatMap() {

        if (UIHelper.getGraph() == null) {
            return;
        }

        LOGGER.info("Updating heatmap");

        this.layersPerPixel = UIHelper.getGraph().getMaxLayer()
                / heatMapCanvas.getWidth();
        this.heatMapMap = new TreeMap<>();
        this.avgMap = new TreeMap<>();
        NormalKernel kernel = new NormalKernel((int) layersPerPixel / 2);

        int[] mapCounter = new int[]{0};
        kernel.apply(this.heatmap.nodesPerLayer(),
                (int) this.heatMapCanvas.getWidth())
                .forEach((index, amount) -> {
                    this.heatMapMap
                            .put(index, (Double) amount);
                    mapCounter[0]++;
                });

        NormalKernel avgKernel = new NormalKernel((int) layersPerPixel / 2);
        int[] avgCounter = new int[]{0};
        avgKernel.apply(this.heatmap.nodesPerLayer(),
                (int) this.heatMapCanvas.getWidth()).forEach(
                (index, amount) -> {
                    this.avgMap.put(
                            index, (Double) amount);
                    avgCounter[0]++;
                });
        drawHeatMap();
    }

    /**
     * The method which is called when a mouse key is pressed on the canvas.
     *
     * @param mouseEvent The event created when the mouse is pressed.
     */
    @FXML
    private void goToLayer(final MouseEvent mouseEvent) {
        if (isDragged) {
            isDragged = false;
            return;
        }

        origX = mouseEvent.getX();
        LOGGER.debug("The user clicked on coordinate: {}", mouseEvent.getX());

        double layer = mouseEvent.getX() * layersPerPixel;
        LOGGER.debug("Going to layer: {}", layer);
        UIHelper.goToLayer((int) layer);
        drawHeatMap();

    }

    /**
     * Draws the heat-map which is stored in {@link #heatmap}
     * on the canvas.
     */
    public final void drawHeatMap() {
        GraphicsContext context = heatMapCanvas.getGraphicsContext2D();
        context.clearRect(0, 0,
                heatMapCanvas.getWidth(), heatMapCanvas.getHeight());
        if (heatmap == null) {
            return;
        }
        double maxLayer = this.heatmap.maximum()._1 / layersPerPixel;

        LOGGER.debug("The maximum layer {} gets mapped to {}", this.heatmap
                .maximum()._1, (int) maxLayer);

        LOGGER.debug("the map does: {} contain the element", this.heatMapMap
                .containsKey((int) maxLayer));

        double maxValue =  heatMapMap.values().stream().max(Double::compare)
                .get();

        double maxAvgValue = this.avgMap.values().stream().max(Double::compare)
                .get();

        double scalingFactor = (heatMapCanvas.getHeight()
                / maxValue) * HEIGHT_MULTIPLIER;

        this.heatMapMap.forEach((i, amount) -> {
            double height = amount * scalingFactor;
            double avgCount = this.avgMap.get(i);
            if (avgCount < maxAvgValue / COLOR_DEVISION) {
                context.setStroke(Color.GREEN);
            } else if (avgCount > maxAvgValue / COLOR_DEVISION
                    && avgCount < 2 * maxAvgValue / COLOR_DEVISION) {
                context.setStroke(Color.ORANGE);
            } else {
                context.setStroke(Color.RED);
            }
            context.setLineWidth(1);
            context.strokeLine(i / layersPerPixel, 0, i
                    / layersPerPixel, height);
        });
        drawCurrentLayer();
    }

    /**
     * This method calculates where the user currently is in the heatmap
     * and draws a box with the layers visible.
     */
    private void drawCurrentLayer() {
        Pair<Double, Double> minMax = UIHelper.drawer().getMinMaxLayer();
        double minLayer = Math.min(minMax.getKey() / layersPerPixel,
                heatMapCanvas.getWidth() - 2);

        double maxLayer = Math.min(minMax.getValue() / layersPerPixel,
                heatMapCanvas.getWidth() - 1);

        minLayer = Math.max(minLayer, 0);
        maxLayer = Math.min(maxLayer, heatMapCanvas.getWidth());

        LOGGER.debug("Going from layer: {} to layer: {}", minMax
        .getKey(), minMax.getValue());
        LOGGER.debug("Going from pixel: {} to pixel: {}", minLayer, maxLayer);

        GraphicsContext context = heatMapCanvas.getGraphicsContext2D();
        context.setLineWidth(2);
        context.setStroke(Color.web("#00a5bbff"));
        context.strokeRect(minLayer, 0, maxLayer - minLayer,
                heatMapCanvas.getHeight());
    }

    /**
     * The method called when the mouse is dragged.
     * @param mouseEvent The event created when the mouse
     *                   is dragged.
     */
    @FXML
    private void draggedMouse(final MouseEvent mouseEvent) {
        double draggedX = mouseEvent.getX() - origX;

        if (Math.abs(draggedX) > DRAG_THRESHOLD) {
            isDragged = true;
        }

        LOGGER.debug("The user dragged the mouse.");

        // Only start translating once past the DRAG_THRESHOLD.
        if (isDragged && heatMapCanvas.contains(
                mouseEvent.getX(), mouseEvent.getY())) {
            origX = mouseEvent.getX();
            int currCenter = UIHelper.getGraph().getCentreLayer();
            double newCenterLayer = draggedX * layersPerPixel + currCenter;
            LOGGER.debug("We are moving {} layers", newCenterLayer);
            UIHelper.goToLayer((int) newCenterLayer);
            drawHeatMap();
        }

    }
}
