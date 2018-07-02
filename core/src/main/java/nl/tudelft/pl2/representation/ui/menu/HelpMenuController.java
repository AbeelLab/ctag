package nl.tudelft.pl2.representation.ui.menu;

import javafx.scene.Parent;
import javafx.scene.control.Menu;

import java.io.IOException;

/**
 * The controller which will control the help menu.
 */
public final class HelpMenuController extends AbstractMenuController {
    /**
     * Creates the object of this graph to control a
     * menu.
     *
     * @param controlMenu The menu which should be controlled.
     */
    HelpMenuController(final Menu controlMenu) {
        super(controlMenu);
        initialize();
    }

    /**
     * Initializes the private JavaFX context. This
     * is to initialize parameters for JavaFX elements,
     * adding event listeners to the elements, etc.
     */
    @Override
    public void initializeFxml() {
        try {
            HelpController controller =
                    loadAndGetController("ui/help/help_screen.fxml");
            findMenuItem("openShortKey").setOnAction(event -> {
                controller.show();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the parent of a controller.
     *
     * @return The parent of a controller.
     */
    @Override
    public Parent getWindow() {
        return null;
    }
}
