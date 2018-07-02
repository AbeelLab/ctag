package nl.tudelft.pl2.representation.ui.menu;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import nl.tudelft.pl2.data.Gfa1Parser;
import nl.tudelft.pl2.data.Gff3Parser;
import nl.tudelft.pl2.representation.exceptions.CTagException;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.LastOpenedHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.graph.GraphController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Observer;
import java.util.function.Consumer;

/**
 * The class which helps with opening a GUI
 * in order to load a .gfa file.
 */
public class GraphLoaderHelper {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER = LogManager
            .getLogger("GraphLoaderHelper");

    /**
     * This method opens a fileChooser and lets the user
     * choose a file.
     *
     * @param additionalExecutable Additional code to run after updating
     *                             the graph.
     * @param type                 The type of file that should be loaded.
     *                             Currently we can only handle "gff" and
     *                             "gfa" files.
     */
    public static void openFileChooser(final Consumer<String>
                                                  additionalExecutable,
                                       final String type) {
        Stage stage = new Stage();
        stage.getIcons().add(new Image("ui/images/logo.png"));
        LOGGER.info("Opening {} file chooser", type);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose your " + type + " file");
        fileChooser.setInitialDirectory(UIHelper.getOpeningDir());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(type, "*." + type),
                new FileChooser.ExtensionFilter("All", "*.*")
        );

        File file = fileChooser.showOpenDialog(stage);
        LOGGER.info("File chosen is: '{}'", file);

        loadFile(additionalExecutable, file, type);
    }

    /**
     * Update the back-end with the file passed as argument.
     *
     * @param additionalExecutable The executable which needs to be run if
     *                             everything succeeds.
     * @param file                 The with which the back-end needs
     *                             to be updated.
     * @param type                 The type of file.
     */
    private static void loadFile(final Consumer<String> additionalExecutable,
                                 final File file,
                                 final String type) {
        try {
            if (file != null) {
                if ("gfa".equals(type)) {
                    ControllerManager.get(GraphController.class)
                            .resetNodeDrawer();
                    UIHelper.updateGraph(file);
                } else if ("gff".equals(type)) {
                    openGffFile(file);
                }
                additionalExecutable.accept(type);
            }
            LOGGER.info("Successfully loaded file: '{}'", file);
        } catch (CTagException ce) {
            LOGGER.error("Loading/Parsing of file went "
                    + "wrong", ce);

            showErrorPopup("We could not open and parse the "
                    + "requested GFA file for you\n"
                    + "Please choose a different file or clear "
                    + "all the compressed files and try again", ce);
        }
    }

    /**
     * Opens the given GFF file and updates all helpers about
     * the opening of that GFF file.
     *
     * @param file The GFF file to open.
     */
    public static void openGffFile(final File file) {
        DoubleProperty doubleProperty =
                new SimpleDoubleProperty(0.0);
        Observer observer = (o, m) -> {
            if (m instanceof LoadingState
                    && m.equals(LoadingState.MILESTONE)) {
                double prev = doubleProperty.get();
                doubleProperty.set(prev + 1.0
                        / (double) Gfa1Parser.MILESTONES());
            }
            if (m instanceof LoadingState
                    && m.equals(LoadingState.FULLY_LOADED_GFF)) {
                ControllerManager.get(GraphController.class)
                        .endGffProgressBar();
            }
        };

        ControllerManager.get(GraphController.class)
                .initGffProgressBar(doubleProperty);
        Gff3Parser.parseAsync(file.toPath(),
                UIHelper.getGraph().getTraitMap(),
                observer
        );

        ControllerManager.get(MenuBarController.class).enableUnloadGffMenu();
        LastOpenedHelper.addRecentGffFile(file.getAbsolutePath());
    }


    /**
     * Opens a popup for the user to let them know
     * that something went wrong whilst opening the gfa file.
     * <p>
     * Be careful since this is a blocking function!
     *
     * @param errorText The text you want to display to
     *                  make the error clearer.
     * @param e         The exception that was thrown.
     */
    private static void showErrorPopup(final String errorText,
                                       final CTagException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(errorText + "\n"
                + "The error is: " + e.toString());
        alert.showAndWait();
    }


    /**
     * Generates an {@link ActionEvent} handler that handles the loading
     * of a GFA file by first asking the user for which file to load, thereafter
     * loading that actual file and finally, when some error occurred, showing
     * the user an Alert that the error occurred.
     *
     * @param additionalExecutable Additionable code to run after updating
     *                             the graph.
     * @return An {@link ActionEvent} handler that performs aforementioned
     * tasks when invoked.
     */
    public final EventHandler<ActionEvent> generateLoadButtonHandler(
            final Consumer<String> additionalExecutable) {
        return (event) -> openFileChooser(additionalExecutable, "gfa");
    }
}
