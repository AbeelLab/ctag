package nl.tudelft.pl2.representation.ui.menu;

import javafx.scene.Node;
import javafx.stage.Stage;
import nl.tudelft.pl2.representation.ui.MainController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.TimeoutException;

public abstract class BaseTestFx extends ApplicationTest {

    @BeforeAll
    public static void setupClass() throws Exception {
        ApplicationTest.launch(MainController.class);
    }

    @AfterAll
    public static void afterTest() throws TimeoutException {
        FxToolkit.hideStage();
    }

    @Override
    public void start(Stage stage) {
        stage.show();
    }

    public <T extends Node> T find(final String query) {
        return (T) lookup(query).queryAll().iterator().next();
    }

}