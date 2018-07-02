package nl.tudelft.pl2.representation.ui.menu;

import javafx.scene.Parent;
import javafx.scene.control.Menu;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Created by Just on 22-6-2018.
 */
public class AnnotationMenuController extends AbstractMenuController {

    /**
     * Constructor of AnnotationMenuController.
     *
     * @param annoTationMenu Menu
     */
    AnnotationMenuController(final Menu annoTationMenu) {
        super(annoTationMenu);
        initialize();
    }

    /**
     * Initialize FXML.
     */
    @Override
    public final void initializeFxml() {
        try {
            AnnotationController controller =
                    loadAndGetController("ui/menu/annotation-view.fxml");

            getMenu().getItems().forEach(menuItem -> {
                menuItem.setOnAction(event -> {
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
