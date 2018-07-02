package nl.tudelft.pl2.representation.ui.menu;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.bookmark.BookmarkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class representing the stage for a
 * bookmark import screen.
 */
public class BookmarkImportController extends Controller {

    /**
     * The pane that is drawn as the stage.
     */
    @FXML
    private GridPane pane;

    /**
     * The input text field in which users can paste
     * their bookmark strings.
     */
    @FXML
    private TextField input;

    /**
     * The confirmation button to import a bookmark.
     */
    @FXML
    private Button importButton;

    /**
     * The stage that is opened when the import button in
     * the bookmarks menu is clicked.
     */
    private Stage importStage;

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("BookmarkImportController");

    /**
     * The width of the scene which is created
     * to import bookmarks.
     */
    private static final int SCENE_WIDTH = 300;

    /**
     * The height of the scene which is created
     * to import bookmarks.
     */
    private static final int SCENE_HEIGHT = 140;


    /**
     * Imports the bookmark string currently in the input
     * text box.
     */
    @FXML
    final void importBookmarkString() {
        String inputString = input.getText();
        LOGGER.info("Trying to import bookmark: {}", inputString);

        BookmarkManager.buildBookmark(inputString, true);

        Stage stage = (Stage) importButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Reacts to a {@link KeyEvent} happening in the main
     * {@link GridPane} of the import {@link Stage}.
     *
     * @param event The {@link KeyEvent} that caused this
     *              callback to be called.
     */
    @FXML
    final void paneKeyPressed(final KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            importStage.close();
        } else if (event.getCode() == KeyCode.ENTER) {
            importBookmarkString();
        }
    }

    @Override
    public final void initializeFxml() {
        importStage = new Stage() {
            {
                this.setTitle("create Bookmark");
                this.getIcons().add(new Image("ui/images/logo.png"));
                this.setScene(new Scene(pane, SCENE_WIDTH, SCENE_HEIGHT));
                this.setAlwaysOnTop(true);

                LOGGER.info("Opened the bookmark create screen");
            }
        };

        importStage.getScene().getStylesheets().add(getClass()
                .getResource("/css/material.css").toExternalForm());
    }


    @Override
    public final Parent getWindow() {
        return pane.getParent();
    }

    /**
     * Returns the import {@link Stage} that should be shown when
     * certain keys or buttons are pressed.
     *
     * @return The import {@link Stage}.
     */
    final Stage getImportStage() {
        return importStage;
    }

}
