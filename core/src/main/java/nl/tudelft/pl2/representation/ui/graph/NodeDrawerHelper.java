package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.GraphicsContext;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class acts as a helper for the
 * node drawer in order to make that class
 * simpler and more readable.
 */
final class NodeDrawerHelper {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("NodeDrawerHelper");

    /**
     * The graphics context used by the canvas which is controlled by
     * the node drawer.
     */
    private GraphicsContext gc;

    /**
     * The node drawer object which this object is helping.
     */
    private NodeDrawer drawer;

    /**
     * Contructor for node drawer helper class.
     * This object is responsible for the cleaning of the canvas
     * and for the translating of the graph.
     * @param graphicsContext The context from the graph which
     *                        is used to controll the canvas
     * @param drw The Node drawer which this class is helping.
     */
    NodeDrawerHelper(final GraphicsContext graphicsContext,
                     final NodeDrawer drw) {
        this.gc = graphicsContext;
        this.drawer = drw;
    }

    /**
     * Cleans the canvas.
     */
    void cleanCanvas() {
        gc.clearRect(
                -gc.getTransform().getTx(),
                -gc.getTransform().getTy(),
                gc.getCanvas().getWidth(),
                gc.getCanvas().getHeight());
    }


    /**
     * This moves the graph over the canvas.
     *
     * @param steps The number of steps that should be moved.
     * @param dir   The direction in which to move defined
     *              by @link{MoveDirection} class.
     * @param graph The graph which is being translated.
     */
    void translateGraph(final int steps, final MoveDirection dir,
                              final GraphHandle graph) {
        LOGGER.debug("Moved visible graph {} steps in direction: {}.",
                graph.move(steps, dir), dir);
    }
}
