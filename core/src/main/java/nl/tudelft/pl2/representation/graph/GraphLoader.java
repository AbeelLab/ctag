package nl.tudelft.pl2.representation.graph;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import nl.tudelft.pl2.data.Gfa1Parser;
import nl.tudelft.pl2.representation.graph.helpers.DummyNodeHelper;
import nl.tudelft.pl2.representation.graph.helpers.VerticalIndexingHelper;
import nl.tudelft.pl2.representation.ui.ThreadCompleter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.Future;

/**
 * An interface designed to load a Graph into memory
 * and/or disk and hand off further operations to
 * a handle that performs appropriate actions based
 * on the type of filesystem used.
 *
 * @param <H> The type of handle that is loaded when this
 *            {@link GraphLoader}'s {@link #load} method is
 *            called.
 */
public interface GraphLoader<H extends GraphHandle> {

    /**
     * Loads a Population Graph (including pre-processing,
     * storage and handle creation) and returns a handle
     * presenting that graph.
     *
     * @param path    The path to the file from which the
     *                Population Graph must be parsed.
     * @param handler The completion handler called upon
     *                throwing an exception or completing
     *                loading.
     * @return A handle representing the Graph and
     * allowing certain actions and queries to be posed
     * upon it.
     */
    H load(Path path, ThreadCompleter handler);

    /**
     * Create a new unique ID.
     *
     * @return The new ID
     */
    int createNewId();

    /**
     * Creates a {@link Runnable} that closes over the given
     * file parameter. The {@link Runnable} loads and completes
     * the layerMap which is also passed to the {@link GraphHandle}.
     *
     * @param file    File from which to load the graph.
     * @param handler The completion handler called upon
     *                throwing an exception or completing
     *                loading.
     * @return A {@link Runnable} that can be threaded completing
     * the layer map.
     */
    Runnable loadGraphRunnable(File file,
                               ThreadCompleter handler);

    /**
     * Get the loadFutures of the loader.
     *
     * @return The loadFuture
     */
    List<Future> getLoadFutures();

    /**
     * Add a load future to the list of load futures.
     *
     * @param future The future to add
     */
    void addLoadFuture(Future future);

    /**
     * Get the double property of the loader.
     *
     * @return The double property
     */
    DoubleProperty getLoadingProperty();

    /**
     * Get the loading state of the loader.
     *
     * @return The loading state
     */
    ObjectProperty<LoadingState> getLoadingState();

    /**
     * Get the dummyNodeHelper used in the GraphLoader.
     *
     * @return The dummyNodeHelper
     */
    DummyNodeHelper getDummyNodeHelper();

    /**
     * Get the vertical index helper of the loader.
     *
     * @return The verticalIndexHelper
     */
    VerticalIndexingHelper getVerticalIndexingHelper();

    /**
     * Creates the observer needed for the loadingBar.
     *
     * @param doubleProperty Property for the double
     *                       representing the loading percentage
     * @param objectProperty Property for the loadingState
     * @return The observer
     */
    default Observer createLoadingObserver(
            final DoubleProperty doubleProperty,
            final ObjectProperty<LoadingState> objectProperty) {
        final double halfLoaded = 0.5;
        final double indexLoaded = 0.4 / 3.0;
        final double hFileRead = 0.1;
        final double filesPresent = 0.7;
        return (o, m) -> {
            if (!(m instanceof LoadingState)) {
                return;
            }
            objectProperty.set((LoadingState) m);

            switch ((LoadingState) m) {
                case MILESTONE:
                    doubleProperty.set(
                            Math.min(halfLoaded, doubleProperty.get()
                                    + halfLoaded / Gfa1Parser.MILESTONES())
                    );
                    break;
                case FULLY_PARSED:
                    doubleProperty.set(halfLoaded);
                    break;
                case INDEX_WRITTEN:
                    doubleProperty.set(doubleProperty.get() + indexLoaded);
                    break;
                case FILES_PRESENT:
                    doubleProperty.set(filesPresent);
                    break;
                case HEADERS_READ:
                case HEATMAP_READ:
                    doubleProperty.set(doubleProperty.get() + hFileRead);
                    break;
                default:
                    doubleProperty.set(1.0);
                    break;
            }
        };
    }
}
