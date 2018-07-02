package nl.tudelft.pl2.representation.graph.loaders;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import nl.tudelft.pl2.data.Scheduler;
import nl.tudelft.pl2.data.caches.MasterCache;
import nl.tudelft.pl2.data.loaders.MasterCacheLoader;
import nl.tudelft.pl2.representation.GraphBuilderHelper;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.GraphLoader;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.graph.handles.FullGraph;
import nl.tudelft.pl2.representation.graph.helpers.DummyNodeHelper;
import nl.tudelft.pl2.representation.graph.helpers.VerticalIndexingHelper;
import nl.tudelft.pl2.representation.ui.ThreadCompleter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.collection.immutable.List;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Class for building a graph.
 */
public class FullGraphLoader implements GraphLoader {

    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER = LogManager
            .getLogger("FullGraphBuilder");

    /**
     * A helper class for using scala functionality
     * while constructing the graph.
     */
    private GraphBuilderHelper helper;

    /**
     * A helper for adding dummy nodes to layers.
     */
    private DummyNodeHelper dummyNodeHelper;

    /**
     * A helper for adding vertical indexes to layers.
     */
    private VerticalIndexingHelper verticalIndexingHelper;

    /**
     * Cache from which the graph is loaded.
     */
    private MasterCache cache;

    /**
     * Map of layers in the graph.
     */
    private HashMap<Integer, HashSet<Node>> layerMap;

    /**
     * Map that maps a segment to the vertical index of the segment.
     */
    private HashMap<Integer, Integer> verticalMap;

    /**
     * The full graph being loaded.
     */
    private FullGraph fullGraph;

    /**
     * The load future of the full graph.
     */
    private Future loadFuture;

    @Override
    public final Runnable loadGraphRunnable(final File file,
                                            final ThreadCompleter
                                                    handler) {
        return handler.wrap(() -> {
            Observer observer = (o, m) -> {

            };
            cache = MasterCacheLoader.loadGraph(
                    Paths.get(file.toURI()), observer);

            fullGraph.setCache(cache);

            List<Node> nodes = cache.createNodeList();

            nodes.foreach((node) -> {
                int id = node.id();
                int layer = node.layer();
                if (!layerMap.containsKey(layer)) {
                    layerMap.put(layer, new HashSet<>());
                }
                layerMap.get(layer).add(node);
                helper.addNodeToMap(node);
                helper.updateMaxId(id);
                helper.updateLayers(layer);
                return node;
            });

            dummyNodeHelper =
                    new DummyNodeHelper(this, layerMap, helper);

            verticalIndexingHelper =
                    new VerticalIndexingHelper(layerMap, verticalMap, this);

            dummyNodeHelper.addDummyNodeToLayers(
                    helper.minLayer(),
                    helper.maxLayer());

            verticalIndexingHelper.addVerticalOrientation(
                    helper.maxLayer(),
                    helper.minLayer());

            fullGraph.updateHeaders(cache.retrieveHeaderMap());

            fullGraph.signalLoaded(LoadingState.FULLY_LOADED);
            MasterCacheLoader.unload(cache);
        });
    }

    @Override
    public final java.util.List<Future> getLoadFutures() {
        java.util.List<Future> futureList = new LinkedList<Future>();
        futureList.add(loadFuture);
        return futureList;
    }

    @Override
    public final void addLoadFuture(final Future future) {
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final DoubleProperty getLoadingProperty() {
        return new SimpleDoubleProperty(1.0);
    }

    @Override
    public final ObjectProperty<LoadingState> getLoadingState() {
        return new SimpleObjectProperty<>(LoadingState.CACHES_LOADED);
    }

    @Override
    public final GraphHandle load(final Path path,
                                  final ThreadCompleter handler) {
        LOGGER.debug("Starting graph load");
        verticalMap = new HashMap<>();
        layerMap = new HashMap<>();
        helper = new GraphBuilderHelper();
        fullGraph = new FullGraph(
                layerMap, verticalMap, helper);

        loadFuture = Scheduler.schedule(
                loadGraphRunnable(path.toFile(), handler));

        return fullGraph;
    }

    @Override
    public final int createNewId() {
        int maxSegmentID = helper.maxId() + 1;
        helper.updateMaxId(maxSegmentID);
        return maxSegmentID;
    }

    @Override
    public final DummyNodeHelper getDummyNodeHelper() {
        return dummyNodeHelper;
    }

    @Override
    public final VerticalIndexingHelper getVerticalIndexingHelper() {
        return verticalIndexingHelper;
    }
}
