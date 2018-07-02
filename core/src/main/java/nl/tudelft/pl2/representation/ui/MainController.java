package nl.tudelft.pl2.representation.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import nl.tudelft.pl2.data.Scheduler;

/**
 * The entry class for the program.
 *
 * The class is responsible for setting up the application
 * on startup and delegating other classes to setup
 * as well.
 *
 * This includes delegating classes responsible for
 * loading previous user settings, previous loaded files,
 * etc.
 */
public final class MainController extends Application {




    /**
     * The current start point for the app.
     *
     * @param args The arguments which can be passed to the
     *             program.
     */
    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        ControllerManager.preLoad("ui/graph/corrupt_file_popup.fxml");

        final BorderPane root = FXMLLoader
                .load(getClass().getResource("/ui/main_view.fxml"));

        primaryStage.setTitle("C-TAG");
        final Scene scene = new Scene(root);

        primaryStage.setScene(scene);

        primaryStage.getIcons().add(new Image("ui/images/logo.png"));


        primaryStage.show();
        UIHelper.registerPrimaryStage(primaryStage);

        String css =  getClass()
                .getResource("/css/material.css").toExternalForm();

        scene.getStylesheets().add(css);
        primaryStage.setOnCloseRequest((e) -> Scheduler.shutdown());

    }
}
