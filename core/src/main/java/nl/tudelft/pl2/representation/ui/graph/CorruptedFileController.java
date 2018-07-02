package nl.tudelft.pl2.representation.ui.graph;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import nl.tudelft.pl2.data.loaders.MasterCacheLoader;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.UIHelper;

import java.io.File;

/**
 * Controller for the event when a corrupted file is
 * attempted to be loaded. This controller controls a
 * dialogue the user will enter once such a corrupted
 * file crashes the loading thread.
 */
public class CorruptedFileController extends Controller {

    /**
     * The main panel to be shown for the corrupted file
     * popup.
     */
    @FXML
    private BorderPane pane;

    /**
     * The scene that is popped up upon file corruption.
     */
    private Stage stage;

    /**
     * The GFA file that causes an exception during loading.
     */
    private File graphFile;

    @Override
    public final void initializeFxml() {
        stage = new Stage();
        stage.getIcons().add(new Image("ui/images/logo.png"));
        stage.setScene(new Scene(pane));
        stage.setAlwaysOnTop(true);
    }


    @Override
    public final Parent getWindow() {
        return pane.getParent();
    }

    /**
     * Popup the stage with dialogue to handle corrupted
     * files.
     *
     * @param graphFileIn The graph file that was attempted
     *                    to be opened.
     */
    public final void popup(final File graphFileIn) {
        this.graphFile = graphFileIn;
        stage.setTitle("Corrupted File: " + graphFileIn);
        stage.show();
    }

    /**
     * Retries the original load without changing cache files.
     */
    @FXML
    private void retryLoad() {
        UIHelper.updateGraph(graphFile);
        stage.close();
    }

    /**
     * Retries the original load after first removing all
     * cached files.
     */
    @FXML
    private void regenerateCache() {
        MasterCacheLoader.clearFiles(graphFile.toPath());
        retryLoad();
        stage.close();
    }

    /**
     * Cancels the current operation by simply closing the
     * corrupted-file popup dialogue.
     */
    @FXML
    private void cancel() {
        ControllerManager.get(GraphController.class).endGfaProgressBar();
        stage.close();
    }

}
