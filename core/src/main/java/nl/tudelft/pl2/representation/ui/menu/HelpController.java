package nl.tudelft.pl2.representation.ui.menu;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import nl.tudelft.pl2.representation.ui.Controller;

/**
 * The class used to control the help screen.
 */
public final class HelpController extends Controller {

    /**
     * The stage used by the screen to show info.
     */
    private Stage stage;

    /**
     * The pane containing all the info.
     */
    @FXML
    private BorderPane pane;

    /**
     * The gridpane in the screen.
     */
    @FXML
    private GridPane gridpane;


    /**
     * Initializes the private JavaFX context. This
     * is to initialize parameters for JavaFX elements,
     * adding event listeners to the elements, etc.
     */
    @Override
    public void initializeFxml() {
        stage = new Stage() {{
          this.setTitle("Help");
          this.getIcons().add(new Image("ui/images/logo.png"));
          this.setAlwaysOnTop(true);
          this.setScene(new Scene(pane));
        }};
        stage.getScene().getStylesheets().add(getClass()
                .getResource("/css/material.css").toExternalForm());
    }

    /**
     * Opens the stage with the help window.
     */
    public void show() {
        stage.showAndWait();
    }

    /**
     * Gets the parent of a controller.
     *
     * @return The parent of a controller.
     */
    @Override
    public Parent getWindow() {
        return pane.getParent();
    }
}
