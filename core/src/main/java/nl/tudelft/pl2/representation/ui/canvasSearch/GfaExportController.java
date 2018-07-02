package nl.tudelft.pl2.representation.ui.canvasSearch;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import nl.tudelft.pl2.data.Gfa1Builder;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.InfoSidePanel.InfoSidePanelController;
import nl.tudelft.pl2.representation.ui.SelectionHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * The controlelr for the gfa export screen.
 */
public final class GfaExportController extends Controller {

    /**
     * The width of the scene.
     */
    private static final int SCENE_WIDTH = 250;

    /**
     * The height of the scene.
     */
    private static final int SCENE_HEIGHT = 100;

    /**
     * The checkbox for the header into.
     */
    @FXML
    private CheckBox header;

    /**
     * The pane used to display all information.
     */
    @FXML
    private BorderPane pane;

    /**
     * Textfield to display the path.
     */
    @FXML
    private TextField path;

    /**
     * The stage used to open the file chooser.
     */
    private Stage stage;

    @Override
    public void initializeFxml() {
        stage = new Stage() {
            {
                this.setTitle("Export to gfa file");
                this.getIcons().add(new Image("ui/images/logo.png"));
                this.setScene(new Scene(pane, SCENE_WIDTH, SCENE_HEIGHT));
            }
        };
        stage.show();
    }

    /**
     * Gets the parent of a controller.
     *
     * @return The parent of a controller.
     */
    @Override
    public Parent getWindow() {
        return this.pane.getParent();
    }

    /**
     * Opens a new filechooser and lets the user choose a new file.
     *
     * @param event The event that gets created when an action is performed.
     */
    @FXML
    private void selectPath(final ActionEvent event) {
        path.setText("No file selected");
        Stage newStage = new Stage();
        newStage.getIcons().add(new Image("ui/images/logo.png"));

        FileChooser chooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter
                = new FileChooser.ExtensionFilter("GFA files (*.gfa)",
                "*.gfa");
        chooser.getExtensionFilters().add(extFilter);
        File file = chooser.showSaveDialog(newStage);

        if (file != null) {
            path.setText(file.getAbsolutePath());
        }
    }

    /**
     * This method saves the gfa to a file when the user clicks on the
     * save button.
     * @param event Never used.
     */
    @FXML
    private void saveToFile(final ActionEvent event) {
        File file = new File(path.getText());

        boolean headers = header.isSelected();
        List<Node> nodes = new LinkedList<>();
        SelectionHelper.getSelectedNodes().forEach(node -> {
            if ((int) node >= 0) {
                nodes.add(UIHelper.getGraph().retrieveCache().retrieveNodeByID(
                        (int) node));
            }
        });
        Gfa1Builder.writeGfaFileFromNodes(nodes, UIHelper.getGraph(),
                file.toPath(), headers);

        stage.close();

        Controller controller = ControllerManager.get(InfoSidePanelController
                .class);
        final int displayTime = 10;
        Notifications.create().owner(controller.getWindow())
                .text("Saved to file: " + file.getAbsolutePath()).hideAfter(
                        Duration.seconds(displayTime)).show();
    }
}
