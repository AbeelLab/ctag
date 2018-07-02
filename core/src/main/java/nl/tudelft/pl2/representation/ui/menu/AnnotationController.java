package nl.tudelft.pl2.representation.ui.menu;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import nl.tudelft.pl2.data.gff.FeaturePair;
import nl.tudelft.pl2.data.gff.GFFNodeFinder;
import nl.tudelft.pl2.data.gff.Trait;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.TraitHelper;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.UIHelper$;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Just on 22-6-2018.
 */
public class AnnotationController extends Controller implements Observer {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("GoToViewController");

    /**
     * Annotation stage.
     */
    private Stage annotationStage;

    /**
     * The Height of the view used by the goto controller.
     */
    private static final int HEIGHT = 620;

    /**
     * The width of the view used by the goto controller.
     */
    private static final int WIDTH = 810;


    /**
     * Search field for the annotation table.
     */
    @FXML
    private TextField searchField;


    /**
     * Table that contains the annotations.
     */
    @FXML
    private TableView<FeaturePair> annotationTable;

    /**
     * Id of the trait in the pair.
     */
    @FXML
    private TableColumn<FeaturePair, String> traitID;

    /**
     * Start position of the feature.
     */
    @FXML
    private TableColumn<FeaturePair, Integer> startPos;

    /**
     * End position of the feature.
     */
    @FXML
    private TableColumn<FeaturePair, Integer> endPos;

    /**
     * Type of the trait.
     */
    @FXML
    private TableColumn<FeaturePair, String> type;

    /**
     * Attributes of the trait.
     */
    @FXML
    private TableColumn<FeaturePair, String> attributes;

    /**
     * TableColumn of the checkboxes.
     */
    @FXML
    private TableColumn<FeaturePair, Boolean> selectedColumn;

    /**
     * The pane containing the annotations.
     */
    @FXML
    private GridPane annotationPane;

    /**
     * Filtered list of the feature pairs.
     */
    private FilteredList<FeaturePair> filteredList;


    @Override
    public final void initializeFxml() {
        annotationStage = new Stage() {
            {
                this.setTitle("Annotation");
                this.setAlwaysOnTop(true);
                this.setScene(new Scene(annotationPane, WIDTH, HEIGHT));
                LOGGER.info("Created the GoToView ");
            }
        };

        annotationStage.getScene().getStylesheets().add(getClass()
                .getResource("/css/material.css").toExternalForm());

        annotationTable.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);
        annotationTable.setEditable(true);
        setValueFactories();
        addListeners();
        setFactories();
        UIHelper.addObserver(this);


    }

    @Override
    public final Parent getWindow() {
        return annotationPane;
    }

    /**
     * Sets all the listeners.
     */
    private void addListeners() {
        annotationTable.getSelectionModel()
                .selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> {
                    if (newItem != null) {
                        ControllerManager.get(AnnotationDetailController.class)
                                .setContent(newItem.feaureTrait(),
                                        newItem.landmark());
                    }
                });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredList != null) {
                filteredList.setPredicate(p ->
                        p.getString().toLowerCase(Locale.US)
                                .contains(newVal.toLowerCase(Locale.US)));
            }
        });

    }

    /**
     * Sets all the table factories.
     */
    private void setFactories() {
        selectedColumn.setCellFactory(CheckBoxTableCell
                .forTableColumn(selectedColumn));

        annotationTable.setRowFactory(tv -> {
            TableRow<FeaturePair> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    GraphHandle handle = UIHelper.getGraph();
                    FeaturePair pair = row.getItem();
                    Node node = new GFFNodeFinder(
                            pair, handle).findNode();
                    UIHelper.goToLayer(node.layer());
                    LOGGER.debug("TraitHelper has {} traits",
                            TraitHelper.selectTrait(pair.feaureTrait()));
                }
            });
            return row;
        });
    }

    /**
     * Sets all the cell value factories.
     */
    private void setValueFactories() {
        selectedColumn.setCellValueFactory(cell ->
                cell.getValue().selectedProperty());


        traitID.setCellValueFactory(value -> {
            String des = value.getValue().id();
            if (des != null) {
                return new SimpleStringProperty(des);
            }
            return null;
        });

        startPos.setCellValueFactory(value -> {
            Integer startInt = value.getValue().start();
            if (startInt != null) {
                return new SimpleIntegerProperty(startInt).asObject();
            }
            return null;
        });

        endPos.setCellValueFactory(value -> {
            Integer endInt = value.getValue().end();
            if (endInt != null) {
                return new SimpleIntegerProperty(endInt).asObject();
            }
            return null;
        });

        type.setCellValueFactory(value -> {
            String des = value.getValue().ty();
            if (des != null) {
                return new SimpleStringProperty(des);
            }
            return null;
        });

        attributes.setCellValueFactory(value -> {
            String attrString = value.getValue().attrString();
            if (attrString != null) {
                return new SimpleStringProperty(attrString);
            }
            return null;
        });
    }

    /**
     * Shows the stage connected
     * to this controller.
     */
    final void show() {
        this.annotationStage.show();
    }

    /**
     * Gets called when observable's update function is called.
     *
     * @param o   observable
     * @param arg argument
     */
    @Override
    public final void update(final Observable o, final Object arg) {
        if (o instanceof UIHelper$ && arg instanceof GraphHandle) {
            ((GraphHandle) arg).registerObserver(this);
        }
        if (o instanceof GraphHandle && arg instanceof LoadingState) {
            LoadingState state = (LoadingState) arg;
            if (state == LoadingState.FULLY_LOADED_GFF) {
                TraitHelper.setFilteredTraits(new HashSet<>());
                LOGGER.debug("Starting gff item add");
                createSampleList(UIHelper.getGraph()
                        .getTraitMap().retrieveFeatureList());
                LOGGER.debug("GFF list creation done");
                annotationTable.setItems(filteredList);
                LOGGER.debug("Adding gff to menu");
            }

        }
    }

    /**
     * Creates an Filtered list of all the annotations.
     * Is used to filter the TableView annotations by the
     * TextField.
     *
     * @param featurePairList annotations
     */
    private void createSampleList(
            final List<FeaturePair> featurePairList) {
        ObservableList<FeaturePair> sampleList =
                FXCollections.observableArrayList();

        LOGGER.debug("Adding all features");
        sampleList.addAll(featurePairList);
        LOGGER.debug("All features added");


        filteredList =
                new FilteredList<>(sampleList, p ->
                        p.getString().toLowerCase(Locale.US)
                                .contains(searchField.getText()
                                        .toLowerCase(Locale.US)));
    }


    /**
     * Uncheck all checkboxes of the rows that are selected.
     *
     * @param event event.
     */
    @FXML
    private void hideSelected(final ActionEvent event) {
        annotationTable.getSelectionModel()
                .getSelectedItems().forEach(featurePair ->
                featurePair.selectedProperty().set(false));
        event.consume();
    }


    /**
     * Check all checkboxes of the rows that are selected.
     *
     * @param event event.
     */
    @FXML
    private void showSelected(final ActionEvent event) {
        annotationTable.getSelectionModel()
                .getSelectedItems().forEach(featurePair ->
                featurePair.selectedProperty().set(true));
        event.consume();
    }

    /**
     * Sets the selected traits.
     *
     * @param event event
     */
    @FXML
    private void applySelected(final ActionEvent event) {
        HashSet<Trait> traitSet = new HashSet<>();
        annotationTable.getItems().forEach(item -> {
            if (item.selectedProperty().get()) {
                traitSet.add(item.feaureTrait());
            }
        });
        TraitHelper.setFilteredTraits(traitSet);
        event.consume();
        UIHelper.drawer().redrawGraph();
        annotationStage.hide();
    }


}
