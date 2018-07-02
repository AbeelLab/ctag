package nl.tudelft.pl2.representation.ui.InfoSidePanel;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import nl.tudelft.pl2.representation.GraphBuilderHelper;
import nl.tudelft.pl2.representation.NodeFinder;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.external.components.DummyNode;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.graph.GraphController;
import nl.tudelft.pl2.representation.ui.graph.GraphKeyEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Class that controls the informational side panel.
 */
public class InfoSidePanelController
        extends Controller
        implements Observer {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER = LogManager
            .getLogger("InfoSidePanelController");

    /**
     * Button for toggling the left pane visibility.
     */
    @FXML
    private ToggleButton leftToggleButton;

    /**
     * Left pane that is toggled by the leftToogleButton.
     */
    @FXML
    private ScrollPane leftPane;

    /**
     * Label for the node position.
     */
    @FXML
    private Label nodePosition;

    /**
     * Label for the node layer position.
     */
    @FXML
    private Label nodeLayer;

    /**
     * Label for the sequence of the selected node.
     */
    @FXML
    private Label sequence;

    /**
     * Label for the sequence length of the selected node.
     */
    @FXML
    private Label sequenceLength;

    /**
     * Label for the 'sequence' text.
     * If double clicked, the sequence is copied
     * to the clipboard.
     */
    @FXML
    private Label sequenceLabel;

    /**
     * Label for the number of incoming links.
     */
    @FXML
    private Label incoming;

    /**
     * Label for the number of outgoing links.
     */
    @FXML
    private Label outgoing;

    /**
     * Table view of the options of the selected node.
     */
    @FXML
    private TableView<TableOption> optionTable;

    /**
     * Column for the name property of the selected node's options.
     */
    @FXML
    private TableColumn<TableOption, String> optionName;

    /**
     * Column for the value property of the selected node's options.
     */
    @FXML
    private TableColumn<TableOption, String> optionValue =
            new TableColumn<>("Value");

    /**
     * Titled pane that can collapse.
     */
    @FXML
    private TitledPane nodeInformation;

    /**
     * The {@link Node} displayed in the info side panel.
     */
    private Node currentNode;

    @Override
    public final void initializeFxml() {
        UIHelper.addObserver(this);

        leftToggleButton.textProperty().bind(
                Bindings.when(leftToggleButton.selectedProperty())
                        .then("<")
                        .otherwise(">"));

        leftPane.visibleProperty()
                .bindBidirectional(leftToggleButton.selectedProperty());
        leftPane.managedProperty()
                .bindBidirectional(leftToggleButton.selectedProperty());

        leftToggleButton.setOnKeyPressed(new GraphKeyEventHandler());

        optionValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        optionValue.setCellFactory((TableColumn<TableOption, String> column) ->
                new TableCell<TableOption, String>() {
                    @Override
                    protected void updateItem(final String item,
                                              final boolean empty) {
                        super.updateItem(item, empty);
                        setText(item);
                        setTooltip(new Tooltip(splitAndNewLineString(item)));
                    }
                });
        optionName.setCellValueFactory(new PropertyValueFactory<>("name"));

        optionName.setCellFactory((TableColumn<TableOption, String> column) -> {
            TableCell<TableOption, String> nameCell =
                    new TableCell<TableOption, String>() {
                        @Override
                        protected void updateItem(final String item,
                                                  final boolean empty) {
                            super.updateItem(item, empty);
                            setText(item);
                            setTooltip(new Tooltip(
                                    "Double click to copy value."));
                        }
                    };

            nameCell.setOnMouseClicked(cell -> {
                if (cell.getClickCount() == 2) {
                    int index = nameCell.getTableRow().getIndex();
                    TableOption options =
                            nameCell.getTableView().getItems().get(index);
                    copyToClipboard(options.getValue(), "Information of "
                            + options.getName() + " is copied to clipboard.");
                }
            });

            return nameCell;
        });

        sequenceLabel.setTooltip(
                new Tooltip("Double click to copy sequence to clipboard."));

        new SidePanelWrapper(leftPane).prioritizeGraphKeyEventHandler();
    }


    /**
     * Takes a string, splits it on the ; and adds a new line on that position.
     *
     * @param value String
     * @return String
     */
    private String splitAndNewLineString(final String value) {
        if (value != null) {
            return value.replace(";", "\n");
        } else {
            return null;
        }
    }

    /**
     * Toggles the text wrap of the sequence label on a click event.
     */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void wrapSeqText() {
        if (!sequence.isWrapText()) {
            sequence.setWrapText(true);
        } else {
            sequence.setWrapText(false);
        }
    }

    /**
     * Toggles the sequence text to clipboard on double Click.
     *
     * @param event mouseEvent
     */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void copySequence(final MouseEvent event) {
        if (event.getClickCount() == 2) {
            if (!sequence.getText().equals("-")) {
                copyToClipboard(sequence.getText(), "Information of "
                        + sequenceLabel.getText()
                        + " is copied to clipboard.");
            }
        }
    }

    /**
     * Copies all node information of the node currently displayed
     * in the info side panel to clipboard.
     *
     * @param event The event fired upon clicking the 'copy' button.
     */
    @FXML
    private void copyAllNodeInfo(final ActionEvent event) {
        copyToClipboard(currentNode.lineSeparatedString(),
                "Node information for node '" + currentNode.name()
                        + "' is copied to clipboard.");
    }

    /**
     * Copies node information as presenting in the source GFA file
     * to clipboard.
     *
     * @param event The event fired upon clicking the 'copy as GFA'
     *              button.
     */
    @FXML
    private void copyAllNodeInfoAsGfa(final ActionEvent event) {
        copyToClipboard(currentNode.gfaString(),
                "Node information for node '" + currentNode.name()
                        + "' is copied to clipboard as a GFA string.");
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
        if (arg instanceof Node) {
            currentNode = (Node) arg;
            LOGGER.info("Currently displaying segment: '{}' in side-panel",
                    currentNode.name());

            if (currentNode instanceof DummyNode) {
                currentNode =
                        new NodeFinder().findNode((DummyNode) currentNode);
            }

            GraphBuilderHelper helper = new GraphBuilderHelper();
            nodePosition.setText("" + currentNode.id());
            nodeLayer.setText("" + currentNode.layer());
            sequence.setText(currentNode.content());
            sequenceLength.setText("" + currentNode.content().length());
            incoming.setText("" + helper.createJavaListFromMutableBuffer(
                    currentNode.incoming()).size());
            outgoing.setText("" + helper.createJavaListFromMutableBuffer(
                    currentNode.outgoing()).size());
            createTableOptions(currentNode.getOptions());

            if (!leftPane.isVisible()
                    && ControllerManager.get(GraphController.class)
                    .getDoubleClicked()) {
                leftPane.setVisible(true);
                leftPane.setManaged(true);
                if (!nodeInformation.isExpanded()) {
                    nodeInformation.setExpanded(true);
                }
            }
        }
    }


    /**
     * Initializes the Tableview and adds data to it's columns.
     *
     * @param options ap with all the options of a node
     */
    private void createTableOptions(final Map<String, String> options) {
        optionTable.setItems(createTableOptionsList(options));
    }


    /**
     * Creates an ObservableList from the node's options map.
     * This functions creates the data needed to display
     * the node's option properties in a Table view.
     *
     * @param options Map with all the options of a node
     * @return ObservableList
     */
    private ObservableList<TableOption> createTableOptionsList(
            final Map<String, String> options) {
        ObservableList<TableOption> tableOptions =
                FXCollections.observableArrayList();
        options.forEach((k, v) -> tableOptions.add(new TableOption(k, v)));
        return tableOptions;
    }

    /**
     * Copied the given content to clipboard and shows a
     * notification with the given message to signal success.
     *
     * @param content The String that should be copied to clipboard.
     * @param message The String that should be displayed in a
     *                notification upon successful copy.
     */
    private void copyToClipboard(final String content,
                                 final String message) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent clipContent = new ClipboardContent();

        clipContent.putString(content);
        clipboard.setContent(clipContent);

        final int seconds = 4;
        Notifications.create().owner(leftPane.getParent())
                .text(message)
                .hideAfter(Duration.seconds(seconds))
                .show();
    }

    @Override
    public final Parent getWindow() {
        return leftPane.getParent();
    }
}
