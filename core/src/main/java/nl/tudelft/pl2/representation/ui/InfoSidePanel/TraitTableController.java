package nl.tudelft.pl2.representation.ui.InfoSidePanel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import nl.tudelft.pl2.data.gff.Landmark;
import nl.tudelft.pl2.data.gff.Trait;
import nl.tudelft.pl2.representation.ui.Controller;
import nl.tudelft.pl2.representation.ui.TraitHelper;
import nl.tudelft.pl2.representation.ui.graph.DrawableTrait;
import scala.collection.JavaConverters;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * The controller used by the Trait table fxml in order to update the
 * information displayed here.
 */
public class TraitTableController extends Controller implements Observer {

    /**
     * Text field for start.
     */
    @FXML
    private Label start;

    /**
     * Text field for end.
     */
    @FXML
    private Label end;

    /**
     * Text field for score.
     */
    @FXML
    private Label score;

    /**
     * Text field for strand.
     */
    @FXML
    private Label strand;

    /**
     * Text field for phase.
     */
    @FXML
    private Label phase;

    /**
     * Text field for type.
     */
    @FXML
    private Label type;

    /**
     * Text field for id info.
     */
    @FXML
    private Label id;

    /**
     * Text field for source info.
     */
    @FXML
    private Label source;

    /**
     * The table with all the options.
     */
    @FXML
    private TableView<TableOption> optionTable;

    /**
     * The table column with the options.
     */
    @FXML
    private TableColumn<TableOption, String> optionValue;

    /**
     * The table column with options.
     */
    @FXML
    private TableColumn<TableOption, String> optionName;

    /**
     * Initializes the private JavaFX context. This
     * is to initialize parameters for JavaFX elements,
     * adding event listeners to the elements, etc.
     */
    @Override
    public final void initializeFxml() {
        TraitHelper.addObserver(this);

        optionValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        optionValue.setCellFactory((TableColumn<TableOption, String> column)
                -> new TableCell<TableOption, String>() {
            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setTooltip(new Tooltip(splitAndNewLineString(item)));
            }
        });
        optionName.setCellValueFactory(new PropertyValueFactory<>("name"));
    }

    @Override
    public final Parent getWindow() {
        return null;
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
        if (arg instanceof DrawableTrait) {
            Trait trait = ((DrawableTrait) arg).getTrait();
            Landmark landmark = ((DrawableTrait) arg).getLandmark();
            start.setText(String.valueOf(trait.start()));
            end.setText(String.valueOf(trait.end()));
            score.setText(String.valueOf(trait.score()));
            strand.setText(String.valueOf(trait.strand()));
            phase.setText(trait.phase());
            type.setText(landmark.ty());
            id.setText(landmark.id());
            source.setText(landmark.source());

            optionTable.setItems(createTableOptionsList(
                    JavaConverters.mapAsJavaMap(trait.attributes())));
        }
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
            return value;
        }
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

}
