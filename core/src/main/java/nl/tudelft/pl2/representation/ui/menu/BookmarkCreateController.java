package nl.tudelft.pl2.representation.ui.menu;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import nl.tudelft.pl2.representation.ui.SelectionHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.bookmark.BookmarkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * The class used to control the create
 * screen for a bookmark.
 */
public class BookmarkCreateController {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("BookmarkCreateController");

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
     * The textfield used to input the name
     * of the bookmark.
     */
    @FXML
    private TextField input;
    /**
     * The pane that is drawn as the stage.
     */
    @FXML
    private GridPane pane;

    /**
     * The {@link Stage} representing the
     * create-bookmark menu.
     */
    private Stage createStage;

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
            createStage.close();
        }
    }

    /**
     * Initializes the import {@link Stage}.
     */
    @FXML
    public final void initialize() {
        createStage = new Stage() {
            {
                this.setTitle("Create Bookmark");

                this.getIcons().add(new Image("ui/images/logo.png"));

                this.setScene(new Scene(pane, SCENE_WIDTH, SCENE_HEIGHT));
                this.setAlwaysOnTop(true);

                LOGGER.info("Opened the bookmark import screen");
            }
        };

        createStage.getScene().getStylesheets().add(getClass()
                .getResource("/css/material.css").toExternalForm());
    }

    /**
     * Called when the user clicks on the create bookmark button.
     *
     **/
    @FXML
    final void createBookmark() {
        Set<Object> treeSet = SelectionHelper.getSelectedNodes();

        BookmarkManager.buildBookmark(treeSet,
                UIHelper.drawer().getZoomLayer(),
                UIHelper.drawer().getMinLayer(),
                UIHelper.drawer().getMinRow(),
                UIHelper.getGraph().getGraphName(), input.getText(), true);

        createStage.close();
    }

    /**
     * @return Gets the stage used for viewing
     * the create bookmark view.
     */
    final Stage getStage() {
        return createStage;
    }

    /**
     * Calls the {@link #createBookmark()} method when the key pressed is enter.
     *
     * This method gets called when the user presses a key on the {@link
     * #pane} pane.
     * @param keyEvent Used to check which key is pressed.
     */
    @FXML
    private void createBookmarkEnter(final KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            createBookmark();
        }
    }
}
