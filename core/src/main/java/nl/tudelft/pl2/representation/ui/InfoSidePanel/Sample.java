package nl.tudelft.pl2.representation.ui.InfoSidePanel;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

/**
 * Created by Just on 6-6-2018.
 */
public class Sample {

    /**
     * Name string.
     */
    private final String name;

    /**
     * Boolean property is set by the tableview checkboxes.
     * If checked, the boolean property is set to true.
     */
    private final SimpleBooleanProperty selected;

    /**
     * Color property.
     */
    private final SimpleObjectProperty<Color> color;


    /**
     * Sample constructor.
     *
     * @param nameInput String
     */
    public Sample(final String nameInput) {
        this.name = nameInput;
        selected = new SimpleBooleanProperty();
        color = new SimpleObjectProperty<>();
    }

    /**
     * Name getter.
     *
     * @return String
     */
    public final String getName() {
        return name;
    }

    /**
     * SelectedProperty getter.
     *
     * @return SimpleBooleanProperty
     */
    public final SimpleBooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * ColorProperty getter.
     *
     * @return SimpleObjectProperty
     */
    public final SimpleObjectProperty<Color> colorProperty() {
        return color;
    }
}
