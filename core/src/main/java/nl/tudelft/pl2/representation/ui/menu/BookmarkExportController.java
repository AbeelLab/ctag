package nl.tudelft.pl2.representation.ui.menu;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.bookmark.Bookmark;
import nl.tudelft.pl2.representation.ui.bookmark.BookmarkManager;
import nl.tudelft.pl2.representation.ui.bookmark.BookmarkStringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * The bookmark export controller used by the bookmark_export.fxml.
 */
public class BookmarkExportController
        extends Controller
        implements Observer {

    /**
     * The logger used by this class.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("BookmarkExportController");

    /**
     * The {@link GridPane} used for displaying ui elements.
     */
    @FXML
    private GridPane exportPane;

    /**
     * The dropdown menu for selecting a bookmark.
     */
    @FXML
    private ComboBox<Bookmark> bookmarkSelect;

    /**
     * The textfield containing the description of the
     * selected bookmark.
     */
    @FXML
    private TextField descriptionText;

    /**
     * The text field containing the name of the graph
     * of the selected bookmark.
     */
    @FXML
    private TextField graphText;

    /**
     * The text field containing the nodes
     * of the selected bookmark.
     */
    @FXML
    private TextField nodeText;

    /**
     * The text field containing the zoomlevel
     * of the bookmark.
     */
    @FXML
    private TextField zoomLevelText;

    /**
     * The text field containing the translation
     * text.
     */
    @FXML
    private TextField translationText;

    /**
     * The text field containing the
     * export string of the bookmark.
     */
    @FXML
    private TextField exportText;

    /**
     * The stage used to display the grid-pane.
     */
    private Stage exportStage;

    /**
     * The list of bookmarks used in the observable
     * list.
     */
    private ArrayList<Bookmark> bookmarks = new ArrayList<>();

    /**
     * The observable list which notifies the ui when
     * there is a change in order to update the ui.
     */
    private ObservableList<Bookmark> bookmarksObservable
            = FXCollections.observableArrayList(bookmarks);

    /**
     * The number of seconds the popup remains.
     */
    private static final int POPUP_TIME_SPAN = 5;

    @Override
    public final void initializeFxml() {

        BookmarkManager.registerObserver(this);

        exportStage = new Stage() {
            {
                this.setTitle("Export bookmarks");
                this.getIcons().add(new Image("ui/images/logo.png"));
                final int width = 300;
                final int height = 370;
                this.setScene(new Scene(exportPane, width, height));
                this.setAlwaysOnTop(true);
                LOGGER.info("Opened the export bookmark stage");
            }
        };

        exportStage.getScene().getStylesheets().add(getClass()
                .getResource("/css/material.css").toExternalForm());

        bookmarkSelect.setItems(bookmarksObservable);
        bookmarkSelect.setConverter(new BookmarkStringConverter());
        bookmarkSelect.valueProperty().addListener((ob, o, n) ->
                bookmarkSelectListener(n));
    }

    @Override
    public final Parent getWindow() {
        return exportPane.getParent();
    }

    /**
     * When the user types in the description of the export bookmark
     * the export string gets updated.
     *
     * @param newValue        The new bookmark.
     */
    private void bookmarkSelectListener(final Bookmark newValue) {
        if (newValue == null) {
            return;
        }
        LOGGER.info("The '{}' bookmark was selected.",
                newValue.description());
        descriptionText.setText(newValue.description());
        descriptionText.textProperty().addListener(((observable1,
                                                     oldValue1,
                                                     newValue1) -> {
            LOGGER.info("Updating the description from '{}' to '{}'",
                    oldValue1, newValue1);
            String newExport;
            if ("".equals(oldValue1)) {
                newExport = exportText.getText().substring(0,
                        exportText.getText().length() - 1) + newValue1
                        + ";";
            } else {
                newExport = exportText.getText().replace(oldValue1,
                        newValue1);
            }
            exportText.setText(newExport);
        }));
        graphText.setText(newValue.graphName());
        nodeText.setText(newValue.highlightNodes().toString());
        zoomLevelText.setText(String.valueOf(newValue.zoomLevel()));
        translationText.setText(String.valueOf(newValue.layer()));
        exportText.setText(newValue.toString());
    }

    /**
     * This gets the current stage from the UI element.
     *
     * @return The stage used by this controller.
     */
    final Stage getExportStage() {
        return exportStage;
    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     */
    @Override
    public final void update(final Observable o, final Object arg) {
        if (arg instanceof ArrayList) {
            bookmarksObservable.clear();
            for (Object bookmark : (List) arg) {
                bookmarksObservable.add((Bookmark) bookmark);
            }
            LOGGER.info("Add all bookmarks to the bookmark list.");
        }
    }

    /**
     * The method which handles the key event
     * for escape key.
     *
     * @param keyEvent The key event passed to the
     *                 {@link GridPane}.
     */
    @FXML
    public final void closeMenu(final KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            exportStage.close();
        }
    }

    /**
     * Copies string to clipboard.
     *
     * @param event The event created by
     *              the user clicking on the button.
     */
    @FXML
    private void copyToClipboard(final ActionEvent event) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        content.putString(exportText.getText());
        clipboard.setContent(content);

        exportStage.close();
        Notifications.create()
                .owner(exportPane.getParent())
                .hideAfter(Duration.seconds(POPUP_TIME_SPAN))
                .text("Bookmark \""
                        + descriptionText.getText()
                        + "\" is copied to clipboard")
                .show();
    }

    /**
     * Copies the bookmark to the clipboard.
     * @param keyEvent The event that is created.
     */
    @FXML
    private void enterPressed(final KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            copyToClipboard(null);
        }
    }

    /**
     * Opens the combo box with all the bookmarks.
     * @param keyEvent The event that is created.
     */
    @FXML
    private void openBox(final KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            this.bookmarkSelect.show();
        }
    }
}
