package nl.tudelft.pl2.representation.ui.InfoSidePanel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.UIHelper$;
import nl.tudelft.pl2.representation.ui.graph.GenomePainter;
import nl.tudelft.pl2.representation.ui.graph.NodeDrawer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Just on 6-6-2018.
 */
public class SampleSelectionController
        extends Controller
        implements Observer {

    /**
     * Log4J [[Logger]] used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("GenomeMenuController");
    /**
     * Tag in the header and options used to represent the genomes.
     */
    public static final String GENOME_TAG = "ORI";

    /**
     * The current full graph.
     */
    private GraphHandle graphHandle;

    /**
     * The label that tells the number of genomes that are filtered.
     */
    @FXML
    private Label sampleLabel;

    /**
     * TableColumn of the samples.
     */
    @FXML
    private TableColumn<Sample, String> sampleColumn;

    /**
     * TableColumn of the checkboxes.
     */
    @FXML
    private TableColumn<Sample, Boolean> selectedColumn;

    /**
     * TableColumn of the colors.
     */
    @FXML
    private TableColumn<Sample, Color> colorColumn;

    /**
     * TableView of the different table columns.
     */
    @FXML
    private TableView<Sample> sampleTable;

    /**
     * Textfield that is used as search field.
     * The genomes in the TableView are selected for the
     * genomes that contain the text in the textfield.
     */
    @FXML
    private TextField searchField;

    /**
     * A checkbox for rainbow select.
     */
    @FXML
    private CheckBox rainbowBox;

    /**
     * The names of the genomes as strings.
     */
    private String[] genomes;

    /**
     * The class that is used to paint the nodes in
     * different colors corresponding to the selected
     * genome color.
     */
    private GenomePainter painter;

    /**
     * The class that returns unique colors.
     */
    private ColorPicker colorPicker = new ColorPicker();

    /**
     * A list of all the genomes. Is used to filter the
     * genomes in the tableview using the TextField.
     */
    private FilteredList<Sample> filteredList;

    /**
     * Used to check if colorButton is pressed.
     */
    private boolean colorButtonPressed = false;

    @Override
    public final void initializeFxml() {
        UIHelper.addObserver(this);
        sampleColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        selectedColumn.setCellValueFactory(cell ->
                cell.getValue().selectedProperty());
        selectedColumn.setCellFactory(CheckBoxTableCell
                .forTableColumn(selectedColumn));
        colorColumn.setCellValueFactory(cell
                -> cell.getValue().colorProperty());
        colorColumn.setCellFactory(column ->
                new TableCell<Sample, Color>() {
                    @Override
                    protected void updateItem(final Color item,
                                              final boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null) {
                            setBackground(Background.EMPTY);
                        } else {
                            setBackground(new Background(
                                    new BackgroundFill(item, null, null)));
                        }
                    }
                });
        sampleTable.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);

        sampleTable.setEditable(true);
    }

    @Override
    public final Parent getWindow() {
        return null;
    }


    /**
     * Observer update function is called
     * when the observable updates all the observers.
     *
     * @param o   Observable
     * @param arg argument
     */
    @Override
    public final void update(final Observable o, final Object arg) {
        if (o instanceof UIHelper$ && arg instanceof GraphHandle) {
            GraphHandle newGraphHandle = (GraphHandle) arg;
            if (graphHandle != newGraphHandle) {
                graphHandle = (GraphHandle) arg;
                graphHandle.registerObserver(this);
                LOGGER.debug("Registered {} "
                        + "as observer of {}.", this, graphHandle);
                NodeDrawer nodeDrawer = UIHelper.drawer();
                painter = new GenomePainter(graphHandle, nodeDrawer);
                nodeDrawer.setPainter(painter);
            }
        }
        if (o instanceof GraphHandle && arg instanceof String[]) {
            genomes = (String[]) arg;
            createSampleList(genomes);
            sampleTable.setItems(filteredList);

            setListeners();
        }
    }


    /**
     * Initializes all the listeners for the
     * different items in the Genome selection pane.
     */
    private void setListeners() {
        sampleTable.getItems().forEach(sample ->
                sample.selectedProperty()
                        .addListener((obj, oldChange, newChange) -> {
                            if (newChange) {
                                if (!colorButtonPressed) {
                                    sample.colorProperty()
                                            .set(colorPicker.newColor());
                                    painter.addSelected(sample.getName(),
                                            sample.colorProperty().get());
                                }
                            } else {
                                painter.removeSelected(sample.getName(),
                                        sample.colorProperty().get());
                                colorPicker.removeColor(
                                        sample.colorProperty().get());
                                sample.colorProperty().set(null);
                            }
                        }));

        sampleLabel.setText("Samples found: " + filteredList.size());


        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(p ->
                    p.getName().toLowerCase(Locale.US)
                            .contains(newVal.toLowerCase(Locale.US)));
            sampleLabel.setText("Samples found: " + filteredList.size());
        });

    }

    /**
     * Creates an Filtered list of all the genomes.
     * Is used to filter the TableView genomes by the
     * TextField.
     *
     * @param samples genome names
     */
    private void createSampleList(
            final String[] samples) {
        ObservableList<Sample> sampleList =
                FXCollections.observableArrayList();
        for (String s : samples) {
            sampleList.add(new Sample(s));
        }

        filteredList =
                new FilteredList<>(sampleList,
                        p -> p.getName().contains(searchField.getText()));
    }

    /**
     * Called when clear selection button is clicked.
     * Clears all the checkboxes.
     *
     * @param event event.
     */
    @FXML
    final void clearSelection(final ActionEvent event) {
        LOGGER.info("selected nodes are cleared");
        filteredList.getSource().forEach(column
                -> column.selectedProperty().set(false));
        event.consume();
    }

    /**
     * Called when color selected button is clicked.
     * Colors all the selected genomes in the same color.
     *
     * @param event ActionEvent.
     */
    @FXML
    final void colorSelected(final ActionEvent event) {
        LOGGER.info("Coloring selected genomes");
        final Color color = rainbowBox.isSelected()
                ? null : colorPicker.newColor();
        colorButtonPressed = true;
        sampleTable.getSelectionModel().getSelectedItems().forEach(sample -> {
            if (sample.selectedProperty().get()) {
                sample.selectedProperty().set(false);
            }
            Color newColor = color;
            if (newColor == null) {
                newColor = colorPicker.newColor();
            }
            sample.selectedProperty().set(true);
            sample.colorProperty().set(newColor);
            painter.addSelected(sample.getName(),
                    sample.colorProperty().get());

        });

        colorButtonPressed = false;
        event.consume();
    }

}
