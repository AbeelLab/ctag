package nl.tudelft.pl2.representation.ui.menu;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.LastOpenedHelper;
import nl.tudelft.pl2.representation.ui.LastOpenedHelper$;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.graph.GraphController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Tuple2;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * This class is responsible for setting
 * up the menu on the top of the screen.
 *
 * This includes things such as adding
 * event listeners to the menu buttons.
 *
 * @author Cedric Willekens
 */

public class MenuBarController
        extends Controller
        implements Observer {

    /**
     * The maximum number of recent files.
     */
    public static final int NUMBER_OF_RECENT_FILES = 5;
    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("MenuBarController");
    /**
     * The offset used to parse the recent opened file
     * to a path.
     */
    private static final int BEGIN_INDEX = 3;
    /**
     * The object representing the menu item
     * to open a new gfa file.
     */
    @FXML
    private MenuItem openFile;

    /**
     * The object representing the menu item
     * to open a new gff file.
     */
    @FXML
    private MenuItem openGffFile;

    /**
     * The object representing the menu item
     * for closing the application.
     */
    @FXML
    private MenuItem closeApp;

    /**
     * The object representing the menu item
     * for searching.
     */
    @FXML
    private Menu goTo;

    /**
     * This contains the list of strings which
     * point to a graph which was last opened.
     */
    @FXML
    private List<MenuItem> lastOpenedList =
            FXCollections.observableArrayList(new ArrayList<>());
    /**
     * The menu which contains all the
     * bookmark menu items.
     */
    @FXML
    private Menu bookmarkMenu;

    /**
     * The menu which contains the
     * annotation stage.
     */
    @FXML
    private Menu annotationMenu;

    /**
     * FXML reference for the semantic zoom menu.
     */
    @FXML
    private Menu semanticZoom;

    /**
     * The menu item which contains the
     * list of last opened GFA files.
     */
    @FXML
    private Menu openLastFile;

    /**
     * The menu item which contains the
     * list of last opened GFF files.
     */
    @FXML
    private Menu openLastGffFile;

    /**
     * Menu item to unload the gff file.
     */
    @FXML
    private MenuItem unloadGffFile;

    /**
     * The help menu item.
     */
    @FXML
    private Menu helpMenu;

    @Override
    public final void initializeFxml() {
        LOGGER.info("Initializing menu.");
        closeApp.setOnAction(event -> Platform.exit());

        new BookmarkMenuController(bookmarkMenu);
        new GoToMenuController(goTo);
        new AnnotationMenuController(annotationMenu);
        new SemanticZoomController(semanticZoom);
        new HelpMenuController(helpMenu);


        openFile.setOnAction(new GraphLoaderHelper()
                .generateLoadButtonHandler((type) ->
                        ControllerManager.get(GraphController.class)
                                .removeWelcomeText(type)));
        openLastFile.getItems().addAll(lastOpenedList);
        LastOpenedHelper.addObserver(this);

        update(null, LastOpenedHelper.getMostRecentFiles());
    }

    /**
     * Gets the parent of a controller.
     *
     * @return The parent of a controller.
     */
    @Override
    public final Parent getWindow() {
        return null;
    }

    /**
     * Updates the menu bar menus after loading a GFA file.
     */
    public final void updateAfterGfaLoad() {
        openGffFile.setDisable(false);
        openLastGffFile.setDisable(false);
        LastOpenedHelper.notifyObservers();
    }

    /**
     * @param menu  The menu for which the key combination will be added
     *              to one of its items.
     * @param index Index of the recent file to add a combination to.
     * @return The combination that should be used to open the 'index'th
     * most recent file.
     */
    private KeyCodeCombination getComboForRecentFile(final Menu menu,
                                                     final int index) {
        if (menu == openLastFile) {
            if (index == 0) {
                return new KeyCodeCombination(
                        KeyCode.O,
                        KeyCombination.SHIFT_DOWN,
                        KeyCombination.CONTROL_DOWN);
            } else {
                return new KeyCodeCombination(
                        KeyCode.getKeyCode(String.valueOf(index + 1)),
                        KeyCombination.SHIFT_DOWN,
                        KeyCombination.CONTROL_DOWN);
            }
        } else if (menu == openLastGffFile) {
            if (index == 0) {
                return new KeyCodeCombination(
                        KeyCode.G,
                        KeyCombination.SHIFT_DOWN,
                        KeyCombination.CONTROL_DOWN);
            } else {
                return new KeyCodeCombination(
                        KeyCode.getKeyCode(String.valueOf(index + 1)),
                        KeyCombination.SHIFT_DOWN,
                        KeyCombination.CONTROL_DOWN,
                        KeyCombination.ALT_DOWN);
            }
        } else {
            throw new InputMismatchException("Expected either last opened "
                    + "GFA files menu or last opened GFF files menu, "
                    + "but got: " + menu);
        }
    }

    /**
     * Updates the last opened files in the given menu
     * with the given file list.
     *
     * @param loMenu         The menu for which to update child items.
     * @param fileList       The file list that should update the
     *                       menu items.
     * @param onClickHandler Handler to add for each of the added
     *                       menu items.
     */
    private void updateLastOpened(
            final Menu loMenu,
            final scala.collection.immutable.List<Tuple2<Integer, String>>
                    fileList,
            final EventHandler<ActionEvent> onClickHandler) {
        loMenu.getItems().clear();
        fileList.foreach(kv -> {
            String file = kv._1() + 1 + ". " + kv._2();

            MenuItem menuItem = new MenuItem(file);
            menuItem.setOnAction(onClickHandler);
            menuItem.setAccelerator(getComboForRecentFile(loMenu, kv._1()));
            loMenu.getItems().add(menuItem);

            return null;
        });

        if (loMenu.getItems().size() == 0) {
            MenuItem menuItem = new MenuItem("No recent files");
            menuItem.setDisable(true);
            loMenu.getItems().add(menuItem);
        }
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
        if (o instanceof LastOpenedHelper$) {
            LastOpenedHelper$ loHelper = (LastOpenedHelper$) o;

            updateLastOpened(openLastFile, loHelper.zippedGfaFiles(),
                    this::openFile);
            updateLastOpened(openLastGffFile, loHelper.zippedGfaFiles(),
                    this::openGffFile);

            if (UIHelper.getGraph() == null) {
                openGffFile.setDisable(true);
                openLastGffFile.getItems().clear();
                openLastGffFile.setDisable(true);
            } else {
                updateLastOpened(openLastGffFile, loHelper.zippedGffFiles(),
                        this::openGffFile);
            }
        }
    }

    /**
     * This method is used when the user clicks on the
     * the sub menu items and loads this as a graph.
     *
     * @param actionEvent The event that took place.
     */
    private void openFile(final ActionEvent actionEvent) {
        String file = ((MenuItem) actionEvent.getSource())
                .getText().substring(BEGIN_INDEX);

        ControllerManager.get(GraphController.class).resetNodeDrawer();
        UIHelper.updateGraph(new File(file));
        ControllerManager.get(GraphController.class).removeWelcomeText("gfa");
    }

    /**
     * This method is used when the user clicks on the
     * the sub menu items and loads this as a graph.
     *
     * @param actionEvent The event that took place.
     */
    private void openGffFile(final ActionEvent actionEvent) {
        String file = ((MenuItem) actionEvent.getSource())
                .getText().substring(BEGIN_INDEX);

        GraphLoaderHelper.openGffFile(Paths.get(file).toFile());
    }

    /**
     * Enables menu's after function call.
     */
    public final void enableMenuItems() {
        goTo.getItems().forEach(menuItem -> menuItem.setDisable(false));
        bookmarkMenu.getItems().forEach(menuItem -> menuItem.setDisable(false));
        semanticZoom.getItems().forEach(menuItem -> menuItem.setDisable(false));
    }

    /**
     * Enables the unload menu item.
     */
    public final void enableUnloadGffMenu() {
        unloadGffFile.setDisable(false);

    }

    /**
     * Method called when a user clicks on the open gff button.
     *
     * @param event The event created when the user clicks.
     */
    @FXML
    private void openGffFileChooser(final ActionEvent event) {
        GraphLoaderHelper.openFileChooser(type ->
                ControllerManager.get(GraphController.class)
                        .removeWelcomeText(type), "gff");
    }

    /**
     * Method called to unload gff file when menu item is clicked.
     * @param event event
     */
    @FXML
    private void unloadGffFile(final ActionEvent event) {
        UIHelper.getGraph().setTraitMap(null);
        UIHelper.drawer().redrawGraph();
        unloadGffFile.setDisable(true);
        event.consume();
    }

}
