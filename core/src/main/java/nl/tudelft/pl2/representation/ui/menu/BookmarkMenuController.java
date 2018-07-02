package nl.tudelft.pl2.representation.ui.menu;

import javafx.scene.Parent;
import javafx.scene.control.Menu;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.bookmark.BookmarkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * The controller for the bookmark menu.
 */
public class BookmarkMenuController
        extends AbstractMenuController
        implements Observer {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER
            = LogManager.getLogger("BookmarkMenuController");

    /**
     * The method creates the object of this graph to control
     * the menu.
     *
     * @param newMenu The menu to be controlled.
     */
    BookmarkMenuController(final Menu newMenu) {
        super(newMenu);

        UIHelper.addObserver(this);

        initialize();
    }

    @Override
    public final void initializeFxml() {
        try {
            BookmarkImportController importController =
                    loadAndGetController("ui/menu/bookmark_import.fxml");
            BookmarkExportController exportController =
                    loadAndGetController("ui/menu/bookmark_export.fxml");

            BookmarkCreateController createController =
                    loadAndGetController("ui/menu/bookmark_create.fxml");

            BookmarkManager.registerObserver(this);

            UIHelper.addObserver(this);

            findMenuItem("importBookmarkMenuItem").setDisable(true);
            findMenuItem("importBookmarkMenuItem").setOnAction(event -> {
                importController.getImportStage().show();
                LOGGER.info("Clicked on the import menu for bookmarks");
            });

            findMenuItem("exportBookmarkMenuItem").setDisable(true);
            findMenuItem("exportBookmarkMenuItem").setOnAction(event -> {
                exportController.getExportStage().show();
                LOGGER.info("Clicked on the export menu for bookmarks");
            });

            findMenuItem("createBookmarkMenuItem").setDisable(true);
            findMenuItem("createBookmarkMenuItem").setOnAction(event -> {
                createController.getStage().showAndWait();
                LOGGER.info("Clicked on the create menu for bookmarks");
            });
        } catch (IOException e) {
            // IOException should be unchecked because nothing should be
            // going wrong here on runtime unless it is by programmer error.
            throw new UncheckedIOException(e);
        }
    }


    @Override
    public final Parent getWindow() {
        return null;
    }

    @Override
    public final void update(final Observable o, final Object arg) {
        if (!(arg instanceof ArrayList)) {
            LOGGER.info("Setting create bookmark to enabled.");
            findMenuItem("importBookmarkMenuItem").setDisable(false);
            findMenuItem("createBookmarkMenuItem").setDisable(false);
            findMenuItem("createBookmarkMenuItem").setDisable(false);
        }
    }

}
