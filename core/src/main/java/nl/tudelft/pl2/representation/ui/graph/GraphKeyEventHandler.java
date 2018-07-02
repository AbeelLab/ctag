package nl.tudelft.pl2.representation.ui.graph;

import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import nl.tudelft.pl2.representation.graph.ZoomDirection;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.UIHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Function;

/**
 * The {@link KeyEvent} {@link EventHandler} used for interpreting
 * key events aimed at the graph display panel.
 */
public class GraphKeyEventHandler implements EventHandler<KeyEvent> {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER = LogManager
            .getLogger("GraphKeyEventHandler");

    /**
     * The key combination to zoom in.
     */
    private static final KeyCombination ZOOM_IN_KC = new KeyCodeCombination(
            KeyCode.EQUALS, KeyCombination.CONTROL_DOWN);

    /**
     * The key combination to zoom out.
     */
    private static final KeyCombination ZOOM_OUT_KC = new KeyCodeCombination(
            KeyCode.MINUS, KeyCombination.CONTROL_DOWN);

    /**
     * Key combination to save a file.
     */
    private static final KeyCombination SAVE_TO_FILE = new KeyCodeCombination(
            KeyCode.S, KeyCombination.CONTROL_DOWN);

    /**
     * Matches the given {@link KeyEvent} against a number of key
     * combinations and fires if some combination matches the event.
     *
     * @param e The event to match against.
     * @return Whether the event matched some combination.
     */
    private static boolean matchCombinations(final KeyEvent e) {
        GraphController controller = ControllerManager
                .get(GraphController.class);
        if (ZOOM_IN_KC.match(e)) {
            controller.getDrawer().zoomByKey(ZoomDirection.IN, 1);
        } else if (ZOOM_OUT_KC.match(e)) {
            controller.getDrawer().zoomByKey(ZoomDirection.OUT, 1);
        } else if (SAVE_TO_FILE.match(e)) {
            LOGGER.debug("Opening export screen");
            FXMLLoader loader = new FXMLLoader();
            try {
                loader.load(Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(
                                "ui/graph/gfa-export-popup.fxml"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * The handler that consumes a key event and returns whether
     * it used that key event to perform some action or not.
     */
    @SuppressWarnings("checkstyle:javadocvariable")
    public static final Function<KeyEvent, Boolean> HANDLER = e -> {
        switch (e.getCode()) {
            case LEFT:
                UIHelper.drawer().translateGraphByArrowKey(1,
                        MoveDirection.LEFT);
                break;
            case RIGHT:
                UIHelper.drawer().translateGraphByArrowKey(1,
                        MoveDirection.RIGHT);
                break;
            case UP:
                UIHelper.drawer().translateGraphByArrowKey(1,
                        MoveDirection.UP);
                break;
            case DOWN:
                UIHelper.drawer().translateGraphByArrowKey(1,
                        MoveDirection.DOWN);
                break;
            case HOME:
                LOGGER.debug("Going to layer: " + 0);
                UIHelper.goToLayer(0);
                break;
            case PAGE_DOWN:
                UIHelper.goToLayer(Math.min(UIHelper.getGraph().getCentreLayer()
                                + UIHelper.drawer().getShownLayers().get(),
                        UIHelper.getGraph().getMaxLayer()));
                break;
            case PAGE_UP:
                UIHelper.goToLayer(Math.max(UIHelper.getGraph().getCentreLayer()
                        - UIHelper.drawer().getShownLayers().get(), 0));
                break;
            case END:
                UIHelper.goToLayer(UIHelper.getGraph().getMaxLayer());
                break;
            default:
                return matchCombinations(e);
        }
        return true;
    };

    @Override
    public final void handle(final KeyEvent event) {
        HANDLER.apply(event);
    }
}
