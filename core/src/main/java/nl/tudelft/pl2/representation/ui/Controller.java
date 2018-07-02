package nl.tudelft.pl2.representation.ui;

import javafx.fxml.FXML;
import javafx.scene.Parent;

/**
 * Abstract class to govern FXML initialization for
 * JavaFX controller-classes.
 *
 * This class defines methods for initialization of
 * a controller, registering each instance in the
 * {@link ControllerManager} singleton.
 *
 * @author Chris Lemaire
 */
public abstract class Controller {

    /**
     * Initializes the JavaFX context and adds this
     * {@link Controller} instance to the {@link ControllerManager}.
     */
    @FXML
    public final void initialize() {
        ControllerManager.add(this);
        initializeFxml();
    }

    /**
     * Initializes the private JavaFX context. This
     * is to initialize parameters for JavaFX elements,
     * adding event listeners to the elements, etc.
     */
    public abstract void initializeFxml();

    /**
     * Gets the parent of a controller.
     * @return The parent of a controller.
     */
    public abstract Parent getWindow();

}
