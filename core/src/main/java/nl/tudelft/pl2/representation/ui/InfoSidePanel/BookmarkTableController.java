package nl.tudelft.pl2.representation.ui.InfoSidePanel;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import nl.tudelft.pl2.representation.ui.SelectionHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.bookmark.Bookmark;
import nl.tudelft.pl2.representation.ui.bookmark.BookmarkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;
import org.controlsfx.glyphfont.Glyph;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;


/**
 * The controller for the bookmark list in the UI.
 */
public class BookmarkTableController implements Observer {
    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER
            = LogManager.getLogger("BookmarkTableController");

    /**
     * The list which contains all the bookmarks
     * for the bookmarkTable.
     */
    private FilteredList<BookmarkTable> bookmarks;

    /**
     * This the view containing all the bookmarks.
     */
    @FXML
    private TableView<BookmarkTable> bookmarkTable;

    /**
     * Bookmark description column.
     */
    @FXML
    private TableColumn<BookmarkTable, String> bookmarkColumn;

    /**
     * Export glyph column.
     */
    @FXML
    private TableColumn<BookmarkTable, Glyph> exportColumn;

    /**
     * Delete glyph column.
     */
    @FXML
    private TableColumn<BookmarkTable, Glyph> deleteColumn;



    /**
     * Search field for the bookmark.
     */
    @FXML
    private TextField searchField;

    /**
     * System clipboard used to copy bookmarks to clipboard.
     */
    private final Clipboard clipboard = Clipboard.getSystemClipboard();


    /**
     * The method which is called when the controller
     * is initialized.
     */
    @FXML
    private void initialize() {
        BookmarkManager.registerObserver(this);
        bookmarkColumn.setCellValueFactory(cellData -> {
            String des = cellData.getValue().getBookmark().description();
            if (des != null) {
                return new SimpleStringProperty(des);
            }
            return null;
        });

        exportColumn.setCellValueFactory(e ->
                e.getValue().uploadGlyphProperty());
        deleteColumn.setCellValueFactory(e ->
                e.getValue().deleteGlyphProperty());


        initCellFactoryBookmarkColumn();
        initCellFactoryDeleteColumn();
        initCellFactoryExportColumn();

        bookmarkTable.setItems(bookmarks);

    }


    /**
     * Initialize cell factory for the bookmark column.
     */
    private void initCellFactoryBookmarkColumn() {
        bookmarkColumn.setCellFactory(e -> {
            TableCell<BookmarkTable, String> cell =
                    new TableCell<BookmarkTable, String>() {
                @Override
                protected void updateItem(final String item,
                                          final boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                       setText(item);
                    }
                }
            };
            cell.addEventHandler(MouseEvent.MOUSE_CLICKED,
                    event -> selectBookmark(event));
            return cell;
        });
    }

    /**
     * Initialize cell factory for the delete column.
     */
    private void initCellFactoryDeleteColumn() {
        deleteColumn.setCellFactory(e ->
                new TableCell<BookmarkTable, Glyph>() {

                    @Override
                    protected void updateItem(final Glyph item,
                                              final boolean empty) {
                        super.updateItem(item, empty);
                        AnchorPane deletePane = new AnchorPane();
                        if (!empty || item != null) {
                            deletePane.getChildren().add(item);
                            setGraphic(deletePane);
                            deletePane.setCursor(Cursor.HAND);

                            setTooltip(new Tooltip("Delete bookmark"));

                            deletePane.setOnMouseClicked(e -> {
                                BookmarkTable bm = getTableView()
                                        .getItems().get(getIndex());
                                bookmarks.getSource().remove(bm);
                                BookmarkManager.removeBookmark(
                                        bm.getBookmark());
                            });
                        } else {
                            setGraphic(null);
                        }
                    }
                }
        );


    }

    /**
     * Initialize cell factory for the export column.
     */
    private void initCellFactoryExportColumn() {
        exportColumn.setCellFactory(e ->
                new TableCell<BookmarkTable, Glyph>() {
                    @Override
                    protected void updateItem(final Glyph item,
                                              final boolean empty) {
                        super.updateItem(item, empty);
                        AnchorPane deletePane = new AnchorPane();
                        if (!empty || item != null) {
                            deletePane.getChildren().add(item);
                            setGraphic(deletePane);
                            setTooltip(new Tooltip(
                                    "copy bookmark to clipboard"));
                            deletePane.setCursor(Cursor.HAND);
                            deletePane.setOnMouseClicked(e -> {
                                BookmarkTable bm = getTableView()
                                        .getItems().get(getIndex());

                                final ClipboardContent content =
                                        new ClipboardContent();
                                content.putString(bm.getBookmark().toString());
                                clipboard.setContent(content);

                                final int seconds = 3;
                                Notifications.create().owner(this.getParent())
                                        .text("Bookmark \""
                                                + bm.getBookmark().description()
                                                + "\" is copied to clipboard")
                                        .hideAfter(Duration.seconds(seconds))
                                        .show();
                            });
                        } else {
                            setGraphic(null);
                        }
                    }
                }
        );
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
        LOGGER.info("Updating bookmark list");
        if (arg instanceof ArrayList) {
            ArrayList<Bookmark> newBookmarks = (ArrayList<Bookmark>) arg;
            ArrayList<BookmarkTable> newBookmarkTables = new ArrayList<>();
            newBookmarks.forEach(bm ->
                    newBookmarkTables.add(new BookmarkTable(bm)));
            
            try {
                Platform.runLater(() -> {
                    this.bookmarks = new FilteredList<>(FXCollections
                            .observableArrayList(newBookmarkTables), p -> true);
                    this.bookmarkTable.setItems(this.bookmarks);
                });
            } catch (IllegalStateException e) {
                this.bookmarks = new FilteredList<>(FXCollections
                        .observableArrayList(newBookmarkTables), p -> true);
                this.bookmarkTable.setItems(this.bookmarks);
            }



            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.length() == 0) {
                    bookmarks.setPredicate(null);
                } else {
                    bookmarks.setPredicate(p ->
                            p.getBookmark().description().toLowerCase(Locale.US)
                                    .contains(newVal.toLowerCase(Locale.US)));
                }
            });
        }
    }

    /**
     * This method is called when a user clicks on the bookmark.
     *
     * @param mouseEvent The event that is created when the user clicks
     *                   on a bookmark.
     */
    private void selectBookmark(final MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            Bookmark currBookmark = this.bookmarkTable
                    .getSelectionModel().getSelectedItem().getBookmark();
            LOGGER.info("Displaying bookmark: {} with layer {}",
                    currBookmark.description(), currBookmark.layer());

            int newLayer = Math.min((int) currBookmark.layer(),
                    UIHelper.getGraph().getMaxLayer());
            UIHelper.goToLayer(Math.max(newLayer, 0));

            UIHelper.drawer().goToTranslationWithZoom(
                    currBookmark.layer(),
                    currBookmark.row(),
                    currBookmark.zoomLevel());
            SelectionHelper.addSelectedNodes(currBookmark.highlightNodes());
        }
    }
}
