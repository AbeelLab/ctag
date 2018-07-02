package nl.tudelft.pl2.representation.ui.menu;

import javafx.scene.Parent;
import javafx.scene.control.Menu;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * The class controlling the GoToMenu.
 */
public class GoToMenuController extends AbstractMenuController {

    /**
     * Constructor for a new GoToMenuController.
     *
     * @param goToMenu The menu to control.
     */
    GoToMenuController(final Menu goToMenu) {
        super(goToMenu);

        initialize();
    }

    @Override
    public final void initializeFxml() {
        try {
            GoToViewController controller =
                    loadAndGetController("ui/menu/goto_view.fxml");

            getMenu().getItems().forEach(menuItem -> {
                menuItem.setDisable(true);
                menuItem.setOnAction(event -> {
                    String title = menuItem.getText();
                    controller.setContext(title);
                    controller.show();
                });
            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public final Parent getWindow() {
        return null;
    }
}
