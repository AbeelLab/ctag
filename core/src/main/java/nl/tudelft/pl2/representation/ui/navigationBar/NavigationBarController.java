package nl.tudelft.pl2.representation.ui.navigationBar;

import javafx.animation.PauseTransition;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.graph.NodeDrawer;

import java.util.Observer;

/**
 * Created by Just on 2-6-2018.
 */
public class NavigationBarController
        extends Controller
        implements Observer {

    /**
     * Hundred magic number constant.
     */
    private static final double HUNDRED = 100.0;

    /**
     * Navigation button to go left in the graph.
     */
    @FXML
    private Button navLeft;

    /**
     * Navigation button to go right in the graph.
     */
    @FXML
    private Button navRight;

    /**
     * Slider used to zoom in and out in the graph.
     */
    @FXML
    private Slider zoomSlider;

    /**
     * The navigation bar pane.
     */
    @FXML
    private AnchorPane navBarPane;

    /**
     * Artificial change variable.
     */
    private boolean isArtificialChange = false;

    @Override
    public final void initializeFxml() {
        UIHelper.addObserver(this);

        navLeft.setText("<");
        navRight.setText(">");

        addListeners();

        addPressAndHoldHandler(navLeft, Duration.millis(HUNDRED), event ->
                UIHelper.drawer()
                        .translateGraphByArrowKey(1, MoveDirection.LEFT));
    }

    /**
     * Gets the parent of a controller.
     *
     * @return The parent of a controller.
     */
    @Override
    public final Parent getWindow() {
        return navBarPane.getParent();
    }

    /**
     * Listener initializer.
     */
    private void addListeners() {
        navLeft.setOnMouseClicked(event -> UIHelper.drawer()
                .translateGraphByArrowKey(1, MoveDirection.LEFT));

        navRight.setOnMouseClicked(event -> UIHelper.drawer()
                .translateGraphByArrowKey(1, MoveDirection.RIGHT));

        zoomSlider.valueProperty().addListener((ob, o, newValue) ->
                updateShownLayersFromZoomSlider(newValue));
    }

    /**
     * Calculates the new number of shown layers from the current
     * value of the zoom slider and passes it to the shownLayers
     * property in {@link NodeDrawer}.
     *
     * @param newValue The new value for the zoom slider.
     */
    private void updateShownLayersFromZoomSlider(final Number newValue) {
        if (!isArtificialChange) {
            double sf = HUNDRED / (Math.log(
                    UIHelper.drawer().maxShownLayers())
                    / Math.log(NodeDrawer.ZOOM_RATE));
            int layersShown = (int) Math.pow(NodeDrawer.ZOOM_RATE,
                    newValue.doubleValue() / sf);

            isArtificialChange = true;
            UIHelper.drawer().setLayersShown(layersShown);
        }
        isArtificialChange = false;
    }

    /**
     * Calculates the new zoom slider value from the updates
     * number of layers shown on screen and updates it.
     *
     * @param newShownLayers The new number of shown layers.
     */
    public final void updateZoomSliderFromShownLayers(
            final Number newShownLayers) {
        if (!isArtificialChange) {
            double sf = Math.log(newShownLayers.doubleValue())
                    / Math.log(UIHelper.drawer().maxShownLayers());

            isArtificialChange = true;
            zoomSlider.setValue(HUNDRED * sf);
        }
        isArtificialChange = false;
    }

    /**
     * Used to add an press and hold event to an Javafx scene node.
     * The event is triggered every holdTime seconds.
     *
     * @param node     Javafx scene node.
     * @param holdTime Time between event triggers.
     * @param hand     Event handler.
     */
    private void addPressAndHoldHandler(final Node node,
                                        final Duration holdTime,
                                        final EventHandler<MouseEvent> hand) {
        /**
         * Wrapper class.
         * @param <T> type
         */
        class Wrapper<T> {
            private T content;
        }
        Wrapper<MouseEvent> eventWrapper = new Wrapper<>();

        PauseTransition holdTimer = new PauseTransition(holdTime);
        holdTimer.setOnFinished(event ->
                hand.handle(eventWrapper.content));

        node.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            eventWrapper.content = event;
            holdTimer.playFromStart();
        });
        node.addEventHandler(MouseEvent.MOUSE_RELEASED,
                event -> holdTimer.stop());
        node.addEventHandler(MouseEvent.DRAG_DETECTED,
                event -> holdTimer.stop());
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
            navBarPane.setVisible(true);
        }
    }


}
