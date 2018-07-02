package nl.tudelft.pl2.representation.graph.loaders;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import nl.tudelft.pl2.data.Scheduler;
import nl.tudelft.pl2.data.caches.CacheChunk;
import nl.tudelft.pl2.data.caches.MasterCache;
import nl.tudelft.pl2.data.loaders.MasterCacheLoader;
import nl.tudelft.pl2.representation.GraphBuilderHelper;
import nl.tudelft.pl2.representation.external.IntegerInterval;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.external.chunking.Chunk;
import nl.tudelft.pl2.representation.external.components.DummyNode;
import nl.tudelft.pl2.representation.graph.GraphHandle;
import nl.tudelft.pl2.representation.graph.GraphLoader;
import nl.tudelft.pl2.representation.graph.LoadingState;
import nl.tudelft.pl2.representation.graph.handles.ChunkedGraph;
import nl.tudelft.pl2.representation.graph.helpers.DummyNodeHelper;
import nl.tudelft.pl2.representation.graph.helpers.VerticalIndexingHelper;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.ThreadCompleter;
import nl.tudelft.pl2.representation.ui.UIHelper;
import nl.tudelft.pl2.representation.ui.graph.NodeDrawer;
import nl.tudelft.pl2.representation.ui.menu.MenuBarController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.collection.Iterator;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class for building a graph that uses a
 * secondary storage cache to load data during running.
 */
@SuppressWarnings("checkstyle:methodcount")
public class ChunkedGraphLoader implements GraphLoader {
    /**
     * Log4J {@link Logger} used to log debug information
     * and other significant events.
     */
    private static final Logger LOGGER =
            LogManager.getLogger("ChunkedGraphLoader");
    /**
     * The multiplier that the ID calculator uses
     * to ensure a unique ID is generated.
     */
    private static final double ID_MULTIPLIER = 0.66;

    /**
     * The number of layers to keep outside of the screen as a buffer.
     */
    private static final int LOAD_BUFFER_AMOUNT = 300;

    /**
     * The cache containing the data about the graph.
     */
    private MasterCache cache;

    /**
     * Map that maps layer to a set of the segment in that layer.
     */
    private Map<Integer, HashSet<Node>> layerMap;
    /**
     * A helper for building the graph.
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
     * Map that maps the id of a segment to its vertical index.
     */
    private Map<Integer, Integer> verticalMap;

    /**
     * The chunkedGraph being loaded.
     */
    private ChunkedGraph chunkedGraph;

    /**
     * The currently generated ID.
     *
     * This ID is generated to be a unique ID for a new node,
     * A dummy node in this case. This means that the dummy node
     * can be saved in locations where a node id is used as a query.
     */
    private int currentId;

    /**
     * The previous min to load.
     */
    private IntegerInterval previousToLoadInterval =
            new IntegerInterval(0, 0);

    /**
     * The loadFutures of the loader.
     */
    private List<Future> loadFutures;

    /**
     * The amount of layers shown on screen.
     */
    private int shownLayers = NodeDrawer.SHOW_DEFAULT;

    /**
     * Map that maps the index of a chunk to the chunk object.
     */
    private Map<Integer, Chunk> chunkMap;

    /**
     * Map that maps the index of a chunk to the IntegerInterval in that chunk.
     */
    private Map<Integer, IntegerInterval> chunkIntervalMap;

    /**
     * Map that maps the index of a chunk to the dummy nodes in that chunk.
     */
    private Map<Integer, HashSet<DummyNode>> dummyMap;

    /**
     * Lock for the layerMap.
     */
    private ReentrantLock layerMapLock;

    /**
     * Lock for the chunkMap.
     */
    private Lock chunkMapLock;

    /**
     * Lock for the chunkIntervalMap.
     */
    private Lock chunkIntervalLock;

    /**
     * Lock for the dummyMap.
     */
    private Lock dummyMapLock;

    /**
     * Loading property for the loadingBar.
     */
    private DoubleProperty loadingProperty =
            new SimpleDoubleProperty(0.0);

    /**
     * The loading state of the loading bar.
     */
    private ObjectProperty<LoadingState> loadingState =
            new SimpleObjectProperty<>();

    /**
     * Constructor of the loader.
     */
    public ChunkedGraphLoader() {
        layerMapLock = new ReentrantLock();
        chunkMapLock = new ReentrantLock();
        chunkIntervalLock = new ReentrantLock();
        dummyMapLock = new ReentrantLock();
    }

    @Override
    public final GraphHandle load(final Path path,
                                  final ThreadCompleter handler) {
        this.helper = new GraphBuilderHelper();
        this.verticalMap = Collections.synchronizedMap(new HashMap<>());
        this.currentId = (int) (((double) Integer.MIN_VALUE) * ID_MULTIPLIER);
        this.layerMap = Collections.synchronizedMap(new HashMap<>());
        this.loadFutures = new LinkedList<>();
        this.chunkIntervalMap = Collections.synchronizedMap(new HashMap<>());
        this.chunkMap = Collections.synchronizedMap(new HashMap<>());

        this.dummyMap = Collections.synchronizedMap(new HashMap<>());
        this.dummyNodeHelper =
                new DummyNodeHelper(this, layerMap, helper);

        this.verticalIndexingHelper =
                new VerticalIndexingHelper(layerMap, verticalMap, this);

        this.chunkedGraph =
                new ChunkedGraph(
                        this,
                        verticalMap,
                        layerMap,
                        path
                );

        addLoadFuture(Scheduler.schedule(
                loadGraphRunnable(path.toFile(), handler)));

        return chunkedGraph;
    }

    @Override
    public final int createNewId() {
        currentId -= 1;
        return currentId;
    }

    @Override
    public final Runnable loadGraphRunnable(final File file,
                                            final ThreadCompleter
                                                    handler) {
        return handler.wrap(() -> {
            Observer observer = createLoadingObserver(
                    loadingProperty, loadingState);
            cache = MasterCacheLoader.loadGraph(file.toPath(), observer);

            chunkedGraph.setCache(cache);

            checkForNewChunks();

            chunkedGraph.updateHeaders(cache.retrieveHeaderMap());
            chunkedGraph.signalLoaded(LoadingState.FULLY_LOADED);

            if (ControllerManager.get(MenuBarController.class) != null) {
                ControllerManager.get(MenuBarController.class)
                        .enableMenuItems();
            }
        });

    }

    /**
     * Get the lock used for concurrency related to the layerMap.
     *
     * @return The lock
     */
    public final ReentrantLock getMapLock() {
        return layerMapLock;
    }

    @Override
    public final List<Future> getLoadFutures() {
        return loadFutures;
    }

    @Override
    public final void addLoadFuture(final Future future) {
        loadFutures.add(future);
    }

    @Override
    public final DoubleProperty getLoadingProperty() {
        return loadingProperty;
    }

    @Override
    public final ObjectProperty<LoadingState> getLoadingState() {
        return loadingState;
    }

    @Override
    public final DummyNodeHelper getDummyNodeHelper() {
        return dummyNodeHelper;
    }

    @Override
    public final VerticalIndexingHelper getVerticalIndexingHelper() {
        return verticalIndexingHelper;
    }

    /**
     * Get the layer map in the loader.
     *
     * @return The layermap
     */
    public final Map<Integer, HashSet<Node>> getLayerMap() {
        return layerMap;
    }

    /**
     * Set the number of layers currently shown.
     *
     * @param layers The number of layers shown
     */
    public final void setShownLayers(final int layers) {
        this.shownLayers = layers;
    }

    /**
     * Check if chunks have to be loaded or unloaded.
     */
    public final synchronized void checkForNewChunks() {
        int minToLoad = calculateMinToLoad();
        int maxToLoad = calculateMaxToLoad();

        checkForUnloads(minToLoad, maxToLoad);

        Integer newMin = minToLoad;
        Integer newMax = maxToLoad;

        if (previousToLoadInterval.lowerBound() < minToLoad
                && minToLoad < previousToLoadInterval.upperBound()) {
            newMin = (int) previousToLoadInterval.upperBound();
        }

        if (previousToLoadInterval.lowerBound() < maxToLoad
                && maxToLoad < previousToLoadInterval.upperBound()) {
            newMax = (int) previousToLoadInterval.lowerBound();
        }

        loadBetween(newMin, newMax);

        previousToLoadInterval = new IntegerInterval(minToLoad, maxToLoad);

        calculateMinAndMaxLoaded();
    }

    /**
     * Calculate the lowest layer that should be loaded.
     *
     * @return The lowest layer to load
     */
    private int calculateMinToLoad() {
        return Math.max(
                chunkedGraph.getCentreLayer()
                        - (shownLayers / 2)
                        - LOAD_BUFFER_AMOUNT,
                0);
    }

    /**
     * Calculate the highest layer that should be loaded.
     *
     * @return The highest layer to load
     */
    private int calculateMaxToLoad() {
        return Math.min(
                chunkedGraph.getCentreLayer()
                        + (shownLayers / 2)
                        + LOAD_BUFFER_AMOUNT,
                chunkedGraph.getMaxLayer());
    }

    /**
     * Calculate the highest and lowest layer that have been loaded.
     */
    private void calculateMinAndMaxLoaded() {
        Long min = Long.MAX_VALUE;
        Long max = Long.MIN_VALUE;

        try {
            chunkIntervalLock.lock();
            for (Map.Entry<Integer, IntegerInterval> entry
                    : chunkIntervalMap.entrySet()) {
                IntegerInterval integerInterval = entry.getValue();
                min = Math.min(integerInterval.lowerBound(), min);
                max = Math.max(integerInterval.upperBound(), max);
            }
            if (chunkIntervalMap.keySet().size() == 0) {
                max = (long) chunkedGraph.getCentreLayer();
                min = (long) chunkedGraph.getCentreLayer();
            }
        } finally {
            chunkIntervalLock.unlock();
        }
        assert min <= max;
    }

    /**
     * Check if any chunks have to be unloaded.
     *
     * @param minToLoad The lowest layer to keep loaded
     * @param maxToLoad The highest layer to keep loaded
     */
    private void checkForUnloads(final int minToLoad, final int maxToLoad) {
        IntegerInterval interval = new IntegerInterval(minToLoad, maxToLoad);

        try {
            chunkIntervalLock.lock();
            chunkMapLock.lock();
            for (Integer key : new HashSet<>(chunkIntervalMap.keySet())) {
                IntegerInterval integerInterval = chunkIntervalMap.get(key);
                if (!interval.intersects(integerInterval)) {
                    unloadChunk(chunkMap.get(key));
                }
            }
        } finally {
            chunkIntervalLock.unlock();
            chunkMapLock.unlock();
        }
        calculateMinAndMaxLoaded();
    }

    /**
     * Load chunks between the given layers.
     *
     * @param from The lowest layer to load from
     * @param to   The highest layer to load
     */
    private void loadBetween(final long from, final long to) {
        Set<Integer> newChunks = new HashSet<>();
        try {
            chunkMapLock.lock();
            for (long i = from; i <= to; i++) {
                cache.retrieveChunksByLayer((int) i).foreach(
                        (chunk) -> addChunkToNewChunks(chunk, newChunks));
            }
            chunkIntervalLock.lock();
            newChunks.removeAll(chunkIntervalMap.keySet());
            newChunks.forEach(this::addChunkToMaps);
            newChunks.forEach((id) -> loadChunk(chunkMap.get(id)));
        } finally {
            chunkIntervalLock.unlock();
            chunkMapLock.unlock();
        }
    }

    /**
     * Add the chunk to the new chunk set.
     *
     * @param chunk     The chunk to add
     * @param newChunks The set to add it to
     * @return The id of the chunk
     */
    private int addChunkToNewChunks(final Chunk chunk,
                                    final Set<Integer> newChunks) {
        int id = chunk.cacheChunk().index();
        newChunks.add(id);
        if (!chunkMap.containsKey(id)) {
            chunkMap.put(id, chunk);
        }
        return id;
    }

    /**
     * Add the chunk with the given index to the chunkIntervalMap.
     *
     * @param chunkIndex The index of the chunk
     */
    private void addChunkToMaps(final Integer chunkIndex) {
        Chunk newChunk = chunkMap.get(chunkIndex);
        CacheChunk cacheChunk = newChunk.cacheChunk();
        chunkIntervalMap.put(chunkIndex,
                new IntegerInterval(
                        cacheChunk.minLayer(), cacheChunk.maxLayer()));
    }

    /**
     * Load the given chunk into the handle.
     *
     * @param chunk The chunk to load
     */
    private void loadChunk(final Chunk chunk) {
        LOGGER.debug("Loading chunk : {}", chunk.cacheChunk().index());
        int from = Integer.MAX_VALUE;
        int to = Integer.MIN_VALUE;
        Iterator<Integer> it = chunk.layers().iterator();
        while (it.hasNext()) {
            int next = it.next();
            from = Math.min(next, from);
            to = Math.max(next, to);
        }

        try {
            layerMapLock.lock();
            chunk.graph().segments().foreach(this::addNodeToMaps);
            createAndRunColorRunnable(chunk, true);
        } finally {
            layerMapLock.unlock();
        }
        int dummyFrom = layerMap.containsKey(from - 1) ? from - 1 : from;
        int dummyTo = layerMap.containsKey(to + 1) ? to + 1 : to;

        HashSet<DummyNode> dummyNodes =
                dummyNodeHelper.addDummyNodeToLayers(dummyFrom, dummyTo);
        try {
            dummyMapLock.lock();
            dummyMap.put(chunk.cacheChunk().index(), dummyNodes);
        } finally {
            dummyMapLock.unlock();
        }
        verticalIndexingHelper.addVerticalOrientation(dummyFrom, dummyTo);
        LOGGER.debug("Handle has {} chunks", chunkMap.keySet().size());
    }

    /**
     * Unload a chunk from the handle.
     *
     * @param chunk The chunk to unload
     */
    private void unloadChunk(final Chunk chunk) {
        LOGGER.debug("Unloading chunk : {}", chunk.cacheChunk().index());
        int index;
        try {
            layerMapLock.lock();
            chunk.graph().segments().foreach(this::removeNodeFromMaps);
            createAndRunColorRunnable(chunk, false);
            index = chunk.cacheChunk().index();
            dummyMapLock.lock();
            if (dummyMap.containsKey(index)) {
                dummyMap.get(index).forEach(this::removeNodeFromMaps);
                dummyMap.remove(index);
            }
        } finally {
            dummyMapLock.unlock();
            layerMapLock.unlock();
        }
        chunkIntervalMap.remove(index);
        chunkMap.remove(index);
        LOGGER.debug("Handle has {} chunks", chunkMap.keySet().size());
    }

    /**
     * Create and add colors from a chunk.
     *
     * @param chunk The chunk to add from
     * @param load  If the colors should be loaded
     *              If false it will unload
     * @return The future of the runnable
     */
    private Future createAndRunColorRunnable(
            final Chunk chunk,
            final boolean load) {
        return Scheduler.schedule(() -> {
            if (load) {
                chunk.graph().segments().foreach(UIHelper::addColorToNode);
            } else {
                chunk.graph().segments().foreach(UIHelper::removeColorFromNode);
            }

            NodeDrawer nodeDrawer = UIHelper.drawer();
            if (chunkedGraph.isLoaded()) {
                try {
                    Platform.runLater(nodeDrawer::redrawGraph);
                } catch (IllegalStateException e) {
                    nodeDrawer.redrawGraph();
                }
            }
        });
    }

    /**
     * Add the given node to the helper and the layerMap.
     *
     * @param node The node to add
     * @return Whether the node was already present or not
     */
    private boolean addNodeToMaps(final Node node) {
        helper.addNodeToMap(node);
        if (!layerMap.containsKey(node.layer())) {
            layerMap.put(node.layer(), new HashSet<>());
        }
        return layerMap.get(node.layer()).add(node);
    }

    /**
     * Remove a node from the layerMap, helper and the verticalMap.
     *
     * @param node The node to remove
     * @return If the node was successfully removed
     */
    private boolean removeNodeFromMaps(final Node node) {
        int layer = node.layer();
        helper.removeSegmentFromMap(node);
        verticalMap.remove(node.id());
        if (!layerMap.containsKey(layer)) {
            return false;
        }
        layerMap.get(layer).remove(node);
        if (node instanceof DummyNode) {
            restoreEdges((DummyNode) node);
        }
        UIHelper.removeColorFromNode(node);
        if (layerMap.get(layer).isEmpty()) {
            layerMap.remove(layer);
            return true;
        }
        return false;
    }

    /**
     * Restore the original edge to a Dummy Node.
     *
     * @param node The dummy node to restore from
     */
    private void restoreEdges(final DummyNode node) {
//        DummyLink link = (DummyLink) node.incoming().iterator().next();
//        Edge origin = link.origin();
//        Node from = cache.retrieveNodeByID(link.origin().from());
//        Node to = cache.retrieveNodeByID(link.origin().to());
//        helper.addIncoming(to, origin);
//        helper.addOutgoing(from, origin);
    }

    /**
     * Clear all currently loaded chunks.
     */
    public final void clearAllChunks() {
        try {
            chunkMapLock.lock();
            new HashSet<>(chunkMap.values()).forEach(this::unloadChunk);
            cache.clear();
        } finally {
            chunkMapLock.unlock();
        }
    }

    /**
     * Clear the integer interval that stores the previously loaded interval.
     */
    public final void resetPreviousLoaded() {
        this.previousToLoadInterval = new IntegerInterval(0, 0);
    }
}
