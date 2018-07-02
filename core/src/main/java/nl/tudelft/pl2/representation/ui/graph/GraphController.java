package nl.tudelft.pl2.representation.ui.graph;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.graph.ZoomDirection;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.SelectionHelper;
import nl.tudelft.pl2.representation.ui.TraitHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.menu.GraphLoaderHelper;
import nl.tudelft.pl2.representation.ui.menu.MenuBarController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Observer;
import java.util.Set;

/**
 * A class functioning as a graph controller.
 * It is responsible for retrieving the nodes from the
 * graph and then converting them to UI representation
 * of the nodes.
 *
 * This means that the class will also be calculating where
 * nodes are placed on the screen as well as updating the screen
 * whenever there is a rescaling happening.
 *
 * @author Cedric Willekens
 */
public class GraphController
        extends Controller
        implements Observer {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER = LogManager
            .getLogger("GraphController");

    /**
     * The offset used by the fileChooserButton to create
     * some spacing between the fileChooserButton and the text.
     */
    private static final int BUTTON_OFFSET = 150;

    /**
     * The number of pixels worth of scroll required per zoom level.
     */
    private static final int PIXELS_PER_ZOOM = 10;

    /**
     * The number of pixels to move before we consider a mouse click
     * to represent dragging the screen.
     */
    private static final int DRAG_THRESHOLD = 10;

    /**
     * Grey color.
     */
    private static final int GREYCOLOR = 244;

    /**
     * The opacity of the progress overlay screen.
     */
    private static final double OPACITY = 0.8;

    /**
     * The fileChooserButton which the user can use to open the file
     * chooser to choose a new file.
     */
    private Button fileChooserButton;
    /**
     * The welcome text for the user with a small explanation of
     * what he should be doing.
     */
    private Text welcomeText;
    /**
     * The canvas from the UI on which all the nodes and edges are drawn.
     *
     * This canvas is later used to get the graphicscontext from in order
     * to be able to draw.
     */
    @FXML
    private Canvas graphCanvas;
    /**
     * The pane form the UI where the canvas is stored.
     */
    @FXML
    private StackPane graphPane;
    /**
     * The event handler which should be used when the user wants to choose
     * a new file.
     *
     * The {@code fileChooserButtonHandler} creates a fileChooser screen
     * in which the user can choose a file. The default directory
     * for this is retrieved from {@link UIHelper}. Once the
     * user has chosen a file, the default directory in the {@link
     * UIHelper} gets updated and the graph will be loaded.
     *
     * When an exception is thrown we will display an alert notice.
     */
    private final EventHandler<ActionEvent> fileChooserButtonHandler =
            new GraphLoaderHelper()
                    .generateLoadButtonHandler(this::removeWelcomeText);
    /**
     * The id of the first selected node when
     * doing a shift-click.
     */
    private Integer firstSelectedNode = null;

    /**
     * Whether the last click is a dragging click.
     */
    private boolean isDragged = false;

    /**
     * Progress bar.
     */
    @FXML
    private ProgressBar progressBar;

    /**
     * Progress bar for gff.
     */
    @FXML
    private ProgressBar progressBarGff;

    /**
     * Progress Vbox for displaying progress information.
     */
    @FXML
    private VBox progressBox;

    /**
     * Progress label for displaying loading progress states.
     */
    @FXML
    private Label progressState;

    /**
     * The original x and y coordinates which are being set once
     * a user starts dragging.
     */
    private double origX, origY;

    /**
     *
     */
    private boolean doubleClicked = false;

    /**
     * The event handler which updates the node locations when the mouse
     * is dragging over the screen.
     */
    @SuppressWarnings("checkstyle:javadocvariable")
    private final EventHandler<MouseEvent> translateOnDrag = e -> {
        double draggedX = e.getX() - origX;
        double draggedY = e.getY() - origY;

        if (Math.abs(draggedX) + Math.abs(draggedY) > DRAG_THRESHOLD) {
            isDragged = true;
        }

        // Only start translating once past the DRAG_THRESHOLD.
        if (isDragged) {
            origX = e.getX();
            origY = e.getY();
            getDrawer().translateGraph((int) draggedX, (int) draggedY);
        }

        if (e.getClickCount() == 2 && UIHelper
                .drawer().getSelectedNode() != null) {
            UIHelper.goToSegmentById(UIHelper
                    .drawer().getSelectedNode().id());
        }
    };

    /**
     * Current unhandled delta of the mouse scroll wheel.
     */
    private int scrollDelta = 0;

    /**
     * Handles scroll (up/down) events. These events indicate
     * an in/out-zoom action is requested.
     */
    private final EventHandler<ScrollEvent> mouseScroll = e -> {
        scrollDelta += e.getDeltaY();

        if (scrollDelta > PIXELS_PER_ZOOM) {
            getDrawer().zoomGraph(e.getX(), e.getY(), ZoomDirection.IN,
                    scrollDelta / PIXELS_PER_ZOOM);
            scrollDelta -= (scrollDelta / PIXELS_PER_ZOOM) * PIXELS_PER_ZOOM;
        } else if (scrollDelta < -PIXELS_PER_ZOOM) {
            getDrawer().zoomGraph(e.getX(), e.getY(), ZoomDirection.OUT,
                    -scrollDelta / PIXELS_PER_ZOOM);
            scrollDelta -= (scrollDelta / PIXELS_PER_ZOOM) * PIXELS_PER_ZOOM;
        }
    };
    /**
     * Node drawer.
     */
    private NodeDrawer drawer;

    /**
     * This method removes the welcome text from
     * the home screen.
     *
     * @param type The type of file loaded.
     */
    public final void removeWelcomeText(final String type) {
        if ("gfa".equals(type)) {
            graphPane.getChildren().remove(fileChooserButton);
            graphPane.getChildren().remove(welcomeText);
            ControllerManager.get(MenuBarController.class)
                    .updateAfterGfaLoad();

        }
    }

    @Override
    public final void initializeFxml() {
        UIHelper.addObserver(this);
        graphCanvas.setFocusTraversable(true);
        drawer = new NodeDrawer(graphCanvas);

        welcomeText = new Text("                     "
                + "  Welcome to C-TAG \n \n "
                + "Go to file -> Open gfa file to load a new graph. \n"
                + " Or click on the button below to open a graph.");

        fileChooserButton = new Button("Open gfa file!");

        graphPane.getChildren().addAll(welcomeText, fileChooserButton);

        StackPane.setAlignment(welcomeText, Pos.CENTER);

        StackPane.setAlignment(fileChooserButton, Pos.CENTER);
        StackPane.setMargin(fileChooserButton, new Insets(BUTTON_OFFSET,
                0, 0, 0));

        fileChooserButton.setOnAction(fileChooserButtonHandler);

        graphCanvas.widthProperty().bind(graphPane.widthProperty());
        graphCanvas.heightProperty().bind(graphPane.heightProperty());

        graphPane.widthProperty().addListener((e, oldW, newW) -> {
            getDrawer().resizeAndRedraw();
            LOGGER.debug("Adjusted width from {} to {}.", oldW, newW);
            LOGGER.debug("Canvas width is {}.", graphCanvas.getWidth());
        });
        graphPane.heightProperty().addListener((e, oldH, newH) -> {
            getDrawer().resizeAndRedraw();
            LOGGER.debug("Adjusted height from {} to {}.", oldH, newH);
            LOGGER.debug("Canvas height is {}.", graphCanvas.getHeight());
        });

        graphCanvas.setOnScroll(mouseScroll);
        graphCanvas.setOnKeyPressed(new GraphKeyEventHandler());

        graphCanvas.setOnMousePressed(e -> {
            origY = e.getY();
            origX = e.getX();
        });

        graphCanvas.setOnMouseDragged(translateOnDrag);

    }

    @Override
    public final Parent getWindow() {
        return graphPane.getParent();
    }

    /**
     * @return The drawer which is being used in order to
     * draw a graph.
     */
    public final NodeDrawer getDrawer() {
        return drawer;
    }

    /**
     * Resets the {@link NodeDrawer} and its coordinate system.
     */
    public final void resetNodeDrawer() {
        drawer = new NodeDrawer(graphCanvas);
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
    public final void update(final java.util.Observable o, final Object arg) {
        if (arg instanceof GraphHandle) {
            GraphHandle newGraph = (GraphHandle) arg;

            if (!getDrawer().updateGraph(newGraph)) {
                return;
            }

            graphCanvas.setCursor(Cursor.WAIT);
            progressBox.setVisible(true);
            progressBox.setManaged(true);
            progressBar.setVisible(true);
            progressState.setText("Parsing GFA file");
            progressBar.progressProperty().bind(
                    UIHelper.getGraphLoader().getLoadingProperty());

            UIHelper.getGraphLoader().getLoadingState()
                    .addListener((obj, oldV, newV) -> {
                        try {
                            Platform.runLater(() -> updateLoadingText(newV));
                        } catch (IllegalStateException e) {
                            updateLoadingText(newV);
                        }
                    });

            if (UIHelper.getGraphLoader().getLoadingState().get()
                    == LoadingState.CACHES_LOADED) {
                endGfaProgressBar();
            }
        }
    }

    /**
     * Kills off the GFA progress bar.
     */
    public final void endGfaProgressBar() {
        progressBox.setVisible(false);
        progressBox.setManaged(false);
        progressBar.setVisible(false);
        graphCanvas.setCursor(Cursor.DEFAULT);
    }

    /**
     * Initialize the gff progress bar.
     *
     * @param doubleProperty DoubleProperty to be kept track
     *                       of in the progress bar.
     */
    public final void initGffProgressBar(final DoubleProperty doubleProperty) {
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        progressBox.setBackground(new Background(new BackgroundFill(
                Color.rgb(GREYCOLOR, GREYCOLOR, GREYCOLOR, OPACITY),
                null, null)));
        progressBox.toFront();
        progressBarGff.setVisible(true);
        progressState.setText("Parsing Gff file");
        progressBarGff.progressProperty().bind(doubleProperty);
    }

    /**
     * Kill off the GFF progress bar.
     */
    public final void endGffProgressBar() {
        progressBox.setVisible(false);
        progressBox.setManaged(false);
        progressBarGff.setVisible(false);
    }

    /**
     * Update the text above the loading bar.
     *
     * @param newState The new state of the loading
     */
    private void updateLoadingText(final LoadingState newState) {
        switch (newState) {
            case FULLY_PARSED:
                progressState.setText("Creating indexes");
                break;
            case INDEX_WRITTEN:
                progressState.setText("Reading headers");
                break;
            case HEADERS_READ:
                progressState.setText("Reading Heatmap");
                break;
            case HEATMAP_READ:
                progressState.setText("Loading indexes");
                break;
            case CACHES_LOADED:
                progressBox.setVisible(false);
                progressBox.setManaged(false);
                progressBar.setVisible(false);
                graphCanvas.setCursor(Cursor.DEFAULT);
                break;
            default:
                break;
        }
    }

    /**
     * Sets the selected nodes through ctrl+shift selection.
     * When no nodes have been selected yet, the entire
     * layer is selected. When a node has been selected
     * before, the range of layers between the last and
     * the current selected node is added to the selection.
     */
    private void ctrlShiftSelect() {
        if (this.firstSelectedNode == null) {
            this.firstSelectedNode = getDrawer().getSelectedNode().id();

            int startLayer = UIHelper.getGraph()
                    .getSegmentLayer(firstSelectedNode);

            UIHelper.getGraph()
                    .getSegmentsFromLayer(startLayer)
                    .forEach(node -> SelectionHelper.toggleSelected(node.id()));

            LOGGER.debug("Setting the first selected node to: {}",
                    getDrawer().getSelectedNode().id());

            this.firstSelectedNode = getDrawer().getSelectedNode().id();
        } else {
            addShiftSelectedLayers();
        }
    }

    /**
     * Sets the selected nodes through shift selection.
     * When no nodes have been selected yet, the entire
     * layer is selected. When a node has been selected
     * before, the range of layers between the last and
     * the current selected node is selected instead.
     */
    private void shiftSelect() {
        SelectionHelper.clearSelectedNodes();
        ctrlShiftSelect();
    }

    /**
     * Toggles the selected node into or out of the selected
     * state after a CTRL click is performed.
     */
    private void ctrlSelect() {
        LOGGER.debug("Updating the selected status of node: {}",
                getDrawer().getSelectedNode().id());

        SelectionHelper.toggleSelected(getDrawer().getSelectedNode().id());
        this.firstSelectedNode = getDrawer().getSelectedNode().id();
    }

    /**
     * The code that will be executed when a mouse is clicked.
     * It will update the location of the mouse as well as
     * try to find a segment.
     *
     * With this segment we can eventually retrieve the extra
     * information that we want to display when a user clicks on
     * a node.
     *
     * @param e The mouse click event which is captured by the
     *          Canvas.
     */
    @FXML
    public final void graphClick(final MouseEvent e) {
        if (!graphCanvas.isFocused()) {
            graphCanvas.requestFocus();
        }

        doubleClicked = e.getClickCount() == 2;

        if (isDragged) {
            isDragged = false;
            return;
        }

        origX = e.getX();
        origY = e.getY();
        getDrawer().setSegmentAt((int) origX, (int) origY);
        TraitHelper.search(
                (int) origX - (int) graphCanvas.getGraphicsContext2D()
                        .getTransform().getTx(),
                (int) origY - (int) graphCanvas.getGraphicsContext2D()
                        .getTransform().getTy());

        if (getDrawer().getLastSelectedNode() != null) {
            Node selectedNode = getDrawer().getSelectedNode();
            LOGGER.debug("User clicked on node '{}' with id {}.",
                    getDrawer().getSelectedNode().name(),
                    getDrawer().getSelectedNode().id());
            getDrawer().getSelectedNode().outgoing().foreach(
                    (out) -> {
                        LOGGER.debug("Outgoing link: {}", out);
                        return null;
                    }
            );
            if (doubleClicked) {
                UIHelper.goToLayer(selectedNode.layer());
            }

            if (e.isShiftDown() && e.isControlDown()) {
                ctrlShiftSelect();
            } else if (e.isShiftDown()) {
                shiftSelect();
            } else if (e.isControlDown()) {
                ctrlSelect();
            } else {
                SelectionHelper.clearSelectedNodes();
                SelectionHelper.addSelectedNode(
                        getDrawer().getSelectedNode().id());
                this.firstSelectedNode = getDrawer().getSelectedNode().id();
            }
        } else {
            SelectionHelper.clearSelectedNodes();
            this.firstSelectedNode = null;

            LOGGER.debug("User tried clicking on a Segment at ({}, {}), but "
                    + "there was none there.", origX, origY);
        }


    }

    /**
     * This method updates the selected nodes in the
     * {@link UIHelper} based on a shift-click. This adds
     * all nodes between the first selected node and the
     * currently selected node.
     */
    private void addShiftSelectedLayers() {
        int startLayer = UIHelper.getGraph().getSegmentLayer(
                firstSelectedNode);

        int endLayer = UIHelper.getGraph().getSegmentLayer(
                getDrawer().getSelectedNode().id());

        for (int i = Math.min(startLayer, endLayer);
             i <= Math.max(startLayer, endLayer); i++) {
            Set<Node> nodesInLayer = UIHelper.getGraph()
                    .getSegmentsFromLayer(i);
            nodesInLayer.forEach(node ->
                    SelectionHelper.addSelectedNode(node.id()));
        }

        LOGGER.info("Selected nodes from layer {} up to layer {}",
                startLayer, endLayer);
    }

    /**
     * Getter for double clicked.
     *
     * @return boolean.
     */
    public final boolean getDoubleClicked() {
        return doubleClicked;
    }

    /**
     * Requests focus on the graph panel.
     */
    public final void focus() {
        graphPane.requestFocus();
    }
}
