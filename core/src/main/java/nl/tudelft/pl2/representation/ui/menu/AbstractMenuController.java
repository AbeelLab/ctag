package nl.tudelft.pl2.representation.ui.menu;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import nl.tudelft.pl2.representation.ui.Controller;

import java.io.IOException;

/**
 * This class contains the basic functionality for
 * a menu controller.
 */
abstract class AbstractMenuController extends Controller {

    /**
     * The menu which this class is controlling.
     */
    private Menu menu;


    /**
     * Creates the object of this graph to control a
     * menu.
     *
     * @param controlMenu The menu which should be controlled.
     */
    AbstractMenuController(final Menu controlMenu) {
        this.menu = controlMenu;
    }

    /**
     * Finds a menu item by its name. Useful when the menu
     * already contains {@link MenuItem}s and the items were
     * not passed along.
     *
     * @param name The name of the {@link MenuItem} to find.
     *
     * @return The {@link MenuItem}.
     * @throws java.util.NoSuchElementException when the element
     * could not be found.
     */
    MenuItem findMenuItem(final String name) {
        return menu.getItems().stream()
                .filter(mi -> mi.getId().equals(name))
                .findFirst()
                .get();
    }

    /**
     * Loads a given resource and returns the controller used
     * by the loaded JavaFX elements.
     *
     * @param resource The FXML resource to load as a relative
     *                 resource-path.
     * @param <T>      The type of controller that this function
     *                 returns.
     *
     * @return The controller returned by the {@link FXMLLoader}
     * used in loading of type <code>T</code>.
     * @throws IOException when something goes wrong when
     *                     loading of the FXML resource.
     */
    <T> T loadAndGetController(final String resource)
            throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.load(Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resource));

        return loader.getController();
    }

    /**
     * Gets the menu object from this
     * class.
     *
     * @return An instance of object stored
     * in this class
     */
    Menu getMenu() {
        return menu;
    }

}

