package nl.tudelft.pl2.representation.ui.menu;

import javafx.scene.Parent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.graph.NodeDrawer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller for the semantic zooming.
 */
public class SemanticZoomController extends AbstractMenuController {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("SemanticZoomController");

    /**
     * The indels/bubble check-button menu item.
     */
    private CheckMenuItem bubblesAndIndels;

    /**
     * The chain check-button menu item.
     */
    private CheckMenuItem chain;

    /**
     * Creates the object of this graph to control a
     * menu.
     *
     * @param controlMenu The menu which should be controlled.
     */
    SemanticZoomController(final Menu controlMenu) {
        super(controlMenu);
        initialize();
    }

    /**
     * Sets the semantic zoom level to the given zoom level
     * and sets the given other item to false if both items
     * are selected.
     *
     * @param zoom      The semantic zoom level to set to.
     * @param otherItem The other menu item to reset.
     */
    private void setZoomLevelTo(final int zoom,
                                final CheckMenuItem otherItem) {
        otherItem.setSelected(false);
        if (!chain.isSelected() && !bubblesAndIndels.isSelected()) {
            UIHelper.getGraph().setSemanticZoom(0);
        } else {
            UIHelper.getGraph().setSemanticZoom(zoom);
        }
        setupNodeDrawer();
    }

    /**
     * Sets up Node Drawer to draw nodes at a different zoom
     * level.
     */
    private void setupNodeDrawer() {
        NodeDrawer drawer = UIHelper.drawer();
        drawer.reset();
        drawer.updateGraph(UIHelper.getGraph());
        drawer.redrawGraph();
    }

    /**
     * Initializes the private JavaFX context. This
     * is to initialize parameters for JavaFX elements,
     * adding event listeners to the elements, etc.
     */
    @Override
    public final void initializeFxml() {
        LOGGER.debug("finding menu items");
        bubblesAndIndels = (CheckMenuItem) findMenuItem("bubblesAndIndels");
        chain = (CheckMenuItem) findMenuItem("chain");

        LOGGER.debug("Adding event handlers");

        bubblesAndIndels.setOnAction(event -> {
            LOGGER.debug("Clicked on bubblesAndIndels");
            setZoomLevelTo(1, chain);
        });

        chain.setOnAction(event -> {
            LOGGER.debug("Clicked on chain");
            setZoomLevelTo(2, bubblesAndIndels);
        });
        bubblesAndIndels.setDisable(true);
        chain.setDisable(true);

    }

    /**
     * Gets the parent of a controller.
     *
     * @return The parent of a controller.
     */
    @Override
    public final Parent getWindow() {
        return null;
    }
}
