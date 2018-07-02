package nl.tudelft.pl2.representation.ui.menu;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import nl.tudelft.pl2.representation.exceptions.NodeNotFoundException;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.InfoSidePanel.InfoSidePanelController;
import nl.tudelft.pl2.representation.ui.SelectionHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;

/**
 * The controller for the goto menu.
 */
public class GoToViewController extends Controller {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("GoToViewController");

    /**
     * The Height of the view used by the goto controller.
     */
    private static final int HEIGHT = 170;

    /**
     * The widht of the view used by the goto controller.
     */
    private static final int WIDTH = 200;

    /**
     * The context in which this view is used.
     */
    private String context;

    /**
     * The text field used in the view.
     */
    @FXML
    private Text text;

    /**
     * The texfield used in the view.
     */
    @FXML
    private TextField input;

    /**
     * The pane used by the view.
     */
    @FXML
    private GridPane gotoView;

    /**
     * The stage used by the view.
     */
    private Stage gotoStage;

    /**
     * The text used by the view to show an error.
     */
    @FXML
    private Text errorText;

    @Override
    public final void initializeFxml() {
        gotoStage = new Stage() {
            {
                this.setTitle("Go To");
                this.getIcons().add(new Image("ui/images/logo.png"));
                this.setAlwaysOnTop(true);
                this.setScene(new Scene(gotoView, WIDTH, HEIGHT));
                LOGGER.info("Created the GoToView ");

                input.textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(final ObservableValue<?
                            extends String> observable,
                                        final String oldValue,
                                        final String newValue) {
                        if (!newValue.matches("\\d*")) {
                            input.setText(newValue.replaceAll("[^\\d]", ""));
                        }
                    }
                });
            }
        };
        gotoStage.getScene().getStylesheets().add(getClass()
                .getResource("/css/material.css").toExternalForm());
    }

    @Override
    public final Parent getWindow() {
        return gotoView.getParent();
    }

    /**
     * Sets the context in which the goto panel is operating.
     *
     * @param cxt The type of search the goto pannel is
     *            doing.
     */
    final void setContext(final String cxt) {
        this.context = cxt;
        this.gotoStage.setTitle(context);
        this.text.setText(context);
    }

    /**
     * When the user clicks the goto button this method is fired.
     * It checks the context and parses the user input in order
     * to go to that location.
     */
    @FXML
    private void goTo() {
        errorText.setVisible(false);

        try {
            int inputInt = Integer.parseInt(input.getText());
            if ("Go to layer".equals(context)) {
                int maxLayer = UIHelper.getGraph().getMaxLayer();
                if (inputInt > maxLayer) {
                    UIHelper.goToLayer(maxLayer);
                    createNotification("The layer: " + inputInt
                            + " is out of" + " the range. We are "
                            + "jumping to the maximum layer: "
                            + maxLayer);
                } else if (inputInt < 0) {
                    UIHelper.goToLayer(0);
                    createNotification("The layer: " + inputInt
                            + " is out of" + " the range. We are "
                            + "jumping to the minimum layer: "
                            + 0);
                } else {
                    UIHelper.goToLayer(inputInt);
                    UIHelper.getGraph().getSegmentsFromLayer(inputInt)
                            .forEach(node ->
                                    SelectionHelper.addSelectedNode(
                                            node.id()));
                    gotoStage.close();
                }
            } else if ("Go to node id".equals(context)) {
                try {
                    UIHelper.goToSegmentById(inputInt);
                    SelectionHelper.addSelectedNode(inputInt);
                    gotoStage.close();
                } catch (NodeNotFoundException e) {
                    errorText.setText("Node not found");
                    errorText.setVisible(true);
                }
            }
        } catch (NumberFormatException e) {
            errorText.setText("Input is not a number");
            errorText.setVisible(true);
        }


    }

    /**
     * Creates  a notification on the main screen.
     *
     * @param notificationText The text displayed in the notification.
     */
    private void createNotification(final String notificationText) {
        Controller controller = ControllerManager.get(InfoSidePanelController
                .class);
        final int displayTime = 10;
        Notifications.create().owner(controller.getWindow())
                .text(notificationText).hideAfter(Duration.seconds(
                displayTime)).show();
    }

    /**
     * Calls the {@link #goTo()} method when the key pressed is enter.
     *
     * This method gets called when the user presses a key on the {@link
     * #gotoView} pane.
     *
     * @param event Used to check which key is pressed.
     */
    @FXML
    private void goToEnter(final KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            goTo();
        }
    }

    /**
     * Shows the stage connected
     * to this controller.
     */
    final void show() {
        this.gotoStage.show();
    }
}
